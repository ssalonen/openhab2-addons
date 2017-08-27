package org.openhab.io.transport.modbus.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.eclipse.smarthome.core.scheduler.ExpressionThreadPoolManager;
import org.eclipse.smarthome.core.scheduler.ExpressionThreadPoolManager.ExpressionThreadPoolExecutor;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprintVisitor;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSerialSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpointVisitor;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusUDPSlaveEndpoint;
import org.openhab.io.transport.modbus.internal.pooling.ModbusSlaveConnectionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.io.ModbusUDPTransaction;
import net.wimpi.modbus.msg.ModbusRequest;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.net.ModbusSlaveConnection;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.net.UDPMasterConnection;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleInputRegister;

/**
 * <p>
 * ModbusManagerImpl class.
 * </p>
 *
 * @author Sami Salonen
 */
public class ModbusManagerImpl implements ModbusManager {

    private static class PollTaskUnregistered extends Exception {
        public PollTaskUnregistered(String msg) {
            super(msg);
        }

        private static final long serialVersionUID = 6939730579178506885L;

    }

    private static final Logger logger = LoggerFactory.getLogger(ModbusManagerImpl.class);

    /**
     * Time to wait between connection passive+borrow, i.e. time to wait between
     * transactions
     * Default 60ms for TCP slaves, Siemens S7 1212 PLC couldn't handle faster
     * requests with default settings.
     */
    public static final long DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS = 60;

    /**
     * Time to wait between connection passive+borrow, i.e. time to wait between
     * transactions
     * Default 35ms for Serial slaves, motivation discussed
     * here https://community.openhab.org/t/connection-pooling-in-modbus-binding/5246/111?u=ssalonen
     */
    public static final long DEFAULT_SERIAL_INTER_TRANSACTION_DELAY_MILLIS = 35;

    public static final long MODBUS_POLLER_THREADS = 10;
    public static final long MODBUS_POLLER_CALLBACK_THREADS = 5;

    private static final String MODBUS_POLLER_THREAD_POOL_NAME = "MODBUS_POLLER_THREAD_POOL";
    private static final String MODBUS_POLLER_CALLBACK_THREAD_POOL_NAME = "MODBUS_POLLER_CALLBACK_THREAD_POOL";

    private static GenericKeyedObjectPoolConfig generalPoolConfig = new GenericKeyedObjectPoolConfig();

    static {
        // When the pool is exhausted, multiple calling threads may be simultaneously blocked waiting for instances to
        // become available. As of pool 1.5, a "fairness" algorithm has been implemented to ensure that threads receive
        // available instances in request arrival order.
        generalPoolConfig.setFairness(true);
        // Limit one connection per endpoint (i.e. same ip:port pair or same serial device).
        // If there are multiple read/write requests to process at the same time, block until previous one finishes
        generalPoolConfig.setBlockWhenExhausted(true);
        generalPoolConfig.setMaxTotalPerKey(1);

        // block infinitely when exhausted
        generalPoolConfig.setMaxWaitMillis(-1);

        // make sure we return connected connections from/to connection pool
        generalPoolConfig.setTestOnBorrow(true);
        generalPoolConfig.setTestOnReturn(true);

        // disable JMX
        generalPoolConfig.setJmxEnabled(false);
    }

    /**
     * We use connection pool to ensure that only single transaction is ongoing per each endpoint. This is especially
     * important with serial slaves but practice has shown that even many tcp slaves have limited
     * capability to handle many connections at the same time
     *
     * Relevant discussion at the time of implementation:
     * - https://community.openhab.org/t/modbus-connection-problem/6108/
     * - https://community.openhab.org/t/connection-pooling-in-modbus-binding/5246/
     */
    private static KeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> connectionPool;
    private static ModbusSlaveConnectionFactoryImpl connectionFactory;

    private volatile Map<PollTask, ScheduledFuture<?>> scheduledPollTasks = new ConcurrentHashMap<>();

    private static ExpressionThreadPoolExecutor scheduledThreadPoolExecutor = ExpressionThreadPoolManager
            .getExpressionScheduledPool(MODBUS_POLLER_THREAD_POOL_NAME);
    private static ExecutorService callbackThreadPool = ExpressionThreadPoolManager
            .getPool(MODBUS_POLLER_CALLBACK_THREAD_POOL_NAME);

    private Collection<ModbusManagerListener> listeners = new CopyOnWriteArraySet<ModbusManagerListener>();

    static {
        connectionFactory = new ModbusSlaveConnectionFactoryImpl();
        connectionFactory.setDefaultPoolConfigurationFactory(endpoint -> {
            return endpoint.accept(new ModbusSlaveEndpointVisitor<EndpointPoolConfiguration>() {

                @Override
                public EndpointPoolConfiguration visit(ModbusTCPSlaveEndpoint modbusIPSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    endpointPoolConfig.setPassivateBorrowMinMillis(DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }

                @Override
                public EndpointPoolConfiguration visit(ModbusSerialSlaveEndpoint modbusSerialSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    // never "disconnect" (close/open serial port) serial connection between borrows
                    endpointPoolConfig.setReconnectAfterMillis(-1);
                    endpointPoolConfig.setPassivateBorrowMinMillis(DEFAULT_SERIAL_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }

                @Override
                public EndpointPoolConfiguration visit(ModbusUDPSlaveEndpoint modbusUDPSlavePoolingKey) {
                    EndpointPoolConfiguration endpointPoolConfig = new EndpointPoolConfiguration();
                    endpointPoolConfig.setPassivateBorrowMinMillis(DEFAULT_TCP_INTER_TRANSACTION_DELAY_MILLIS);
                    endpointPoolConfig.setConnectMaxTries(Modbus.DEFAULT_RETRIES);
                    return endpointPoolConfig;
                }
            });
        });

        GenericKeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection> genericKeyedObjectPool = new GenericKeyedObjectPool<ModbusSlaveEndpoint, ModbusSlaveConnection>(
                connectionFactory, generalPoolConfig);
        genericKeyedObjectPool.setSwallowedExceptionListener(new SwallowedExceptionListener() {

            @Override
            public void onSwallowException(Exception e) {
                logger.error("Connection pool swallowed unexpected exception: {}", e.getMessage());

            }
        });
        connectionPool = genericKeyedObjectPool;
    }

    public ModbusManagerImpl() {
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
    }

    private static void invokeCallbackWithResponse(ModbusReadRequestBlueprint message, ModbusReadCallback callback,
            ModbusResponse response) {
        try {
            if (message.getFunctionCode() == ModbusReadFunctionCode.READ_COILS) {
                callback.onBits(message, new BitArrayWrappingBitVector(((ReadCoilsResponse) response).getCoils()));
            } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_DISCRETES) {
                callback.onBits(message,
                        new BitArrayWrappingBitVector(((ReadInputDiscretesResponse) response).getDiscretes()));
            } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS) {
                callback.onRegisters(message, new RegisterArrayWrappingInputRegister(
                        ((ReadMultipleRegistersResponse) response).getRegisters()));
            } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_REGISTERS) {
                callback.onRegisters(message,
                        new RegisterArrayWrappingInputRegister(((ReadInputRegistersResponse) response).getRegisters()));
            } else {
                throw new IllegalArgumentException(
                        String.format("Unexpected function code %s", message.getFunctionCode()));
            }

        } catch (Exception e) {
            logger.error("Unhandled exception {} {}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    private ModbusRequest createRequest(ModbusReadRequestBlueprint message) {
        ModbusRequest request;
        if (message.getFunctionCode() == ModbusReadFunctionCode.READ_COILS) {
            request = new ReadCoilsRequest(message.getReference(), message.getDataLength());
        } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_DISCRETES) {
            request = new ReadInputDiscretesRequest(message.getReference(), message.getDataLength());
        } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS) {
            request = new ReadMultipleRegistersRequest(message.getReference(), message.getDataLength());
        } else if (message.getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_REGISTERS) {
            request = new ReadInputRegistersRequest(message.getReference(), message.getDataLength());
        } else {
            throw new IllegalArgumentException(String.format("Unexpected function code %s", message.getFunctionCode()));
        }
        request.setUnitID(message.getUnitID());
        request.setProtocolID(message.getProtocolID());

        return request;
    }

    private static Register[] convertRegisters(ModbusRegisterArray arr) {
        return IntStream.range(0, arr.size()).mapToObj(i -> new SimpleInputRegister(arr.getRegister(i).getValue()))
                .collect(Collectors.toList()).toArray(new Register[0]);
    }

    private ModbusRequest createRequest(ModbusWriteRequestBlueprint message) {
        ModbusRequest[] request = new ModbusRequest[1];
        if (message.getFunctionCode() == ModbusWriteFunctionCode.WRITE_COIL) {
            message.accept(new ModbusWriteRequestBlueprintVisitor() {

                @Override
                public void visit(ModbusWriteRegisterRequestBlueprint blueprint) {
                    throw new IllegalStateException();
                }

                @Override
                public void visit(ModbusWriteCoilRequestBlueprint blueprint) {
                    BitArray coils = blueprint.getCoils();
                    if (coils.size() != 1) {
                        throw new IllegalArgumentException("Must provide single coil with WRITE_COIL");
                    }
                    request[0] = new WriteCoilRequest(message.getReference(), coils.getBit(0));
                }
            });

        } else if (message.getFunctionCode() == ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS) {
            message.accept(new ModbusWriteRequestBlueprintVisitor() {

                @Override
                public void visit(ModbusWriteRegisterRequestBlueprint blueprint) {
                    request[0] = new WriteMultipleRegistersRequest(message.getReference(),
                            convertRegisters(blueprint.getRegisters()));
                }

                @Override
                public void visit(ModbusWriteCoilRequestBlueprint blueprint) {
                    throw new IllegalStateException();
                }
            });

        } else if (message.getFunctionCode() == ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER) {
            message.accept(new ModbusWriteRequestBlueprintVisitor() {

                @Override
                public void visit(ModbusWriteRegisterRequestBlueprint blueprint) {
                    if (blueprint.getRegisters().size() != 1) {
                        throw new IllegalArgumentException("Must provide single register with WRITE_SINGLE_REGISTER");
                    }
                    Register[] registers = convertRegisters(blueprint.getRegisters());
                    request[0] = new WriteSingleRegisterRequest(message.getReference(), registers[0]);
                }

                @Override
                public void visit(ModbusWriteCoilRequestBlueprint blueprint) {
                    throw new IllegalStateException();
                }
            });
        } else

        {
            throw new IllegalArgumentException(String.format("Unexpected function code %s", message.getFunctionCode()));
        }
        request[0].setUnitID(message.getUnitID());
        request[0].setProtocolID(message.getProtocolID());

        return request[0];
    }

    private ModbusTransaction createTransactionForEndpoint(ModbusSlaveEndpoint endpoint,
            Optional<ModbusSlaveConnection> connection) {
        ModbusTransaction transaction = endpoint.accept(new ModbusSlaveEndpointVisitor<ModbusTransaction>() {

            @Override
            public ModbusTransaction visit(ModbusTCPSlaveEndpoint modbusIPSlavePoolingKey) {
                ModbusTCPTransaction transaction = new ModbusTCPTransaction();
                transaction.setReconnecting(false);
                return transaction;
            }

            @Override
            public ModbusTransaction visit(ModbusSerialSlaveEndpoint modbusSerialSlavePoolingKey) {
                return new ModbusSerialTransaction();
            }

            @Override
            public ModbusTransaction visit(ModbusUDPSlaveEndpoint modbusUDPSlavePoolingKey) {
                return new ModbusUDPTransaction();
            }
        });
        transaction.setRetryDelayMillis(
                connectionFactory.getEndpointPoolConfiguration(endpoint).getPassivateBorrowMinMillis());
        if (transaction instanceof ModbusSerialTransaction) {
            ((ModbusSerialTransaction) transaction).setSerialConnection((SerialConnection) connection.get());
        } else if (transaction instanceof ModbusUDPTransaction) {
            ((ModbusUDPTransaction) transaction).setTerminal(((UDPMasterConnection) connection.get()).getTerminal());
        } else if (transaction instanceof ModbusTCPTransaction) {
            ((ModbusTCPTransaction) transaction).setConnection((TCPMasterConnection) connection.get());
        } else {
            throw new IllegalStateException();
        }
        return transaction;
    }

    private Optional<ModbusSlaveConnection> borrowConnection(ModbusSlaveEndpoint endpoint) {
        Optional<ModbusSlaveConnection> connection = Optional.empty();
        long start = System.currentTimeMillis();
        try {
            connection = Optional.ofNullable(connectionPool.borrowObject(endpoint));
        } catch (Exception e) {
            logger.warn("Error getting a new connection for endpoint {}. Error was: {} {}", endpoint,
                    e.getClass().getName(), e.getMessage());
        }
        logger.trace("borrowing connection (got {}) for endpoint {} took {} ms", connection, endpoint,
                System.currentTimeMillis() - start);
        return connection;
    }

    private void invalidate(ModbusSlaveEndpoint endpoint, Optional<ModbusSlaveConnection> connection) {
        if (!connection.isPresent()) {
            return;
        }
        connection.ifPresent(con -> {
            try {
                connectionPool.invalidateObject(endpoint, con);
            } catch (Exception e) {
                logger.warn("Error invalidating connection in pool for endpoint {}. Error was: {} {}", endpoint,
                        e.getClass().getName(), e.getMessage(), e);
            }
        });
    }

    private void returnConnection(ModbusSlaveEndpoint endpoint, Optional<ModbusSlaveConnection> connection) {
        connection.ifPresent(con -> {
            try {
                connectionPool.returnObject(endpoint, con);
            } catch (Exception e) {
                logger.warn("Error returning connection to pool for endpoint {}. Error was: {} {}", endpoint,
                        e.getClass().getName(), e.getMessage(), e);
            }
        });
        logger.trace("returned connection for endpoint {}", endpoint);
    }

    private void verifyTaskIsRegistered(PollTask task) throws PollTaskUnregistered {
        if (!this.scheduledPollTasks.containsKey(task)) {
            String msg = String.format("Poll task %s is unregistered", task);
            logger.debug(msg);
            throw new PollTaskUnregistered(msg);
        }
    }

    @Override
    public ScheduledFuture<?> submitOneTimePoll(PollTask task) {
        long scheduleTime = System.currentTimeMillis();
        logger.debug("Scheduling one-off poll task {}", task);
        ScheduledFuture<?> future = scheduledThreadPoolExecutor.schedule(() -> {
            long millisInThreadPoolWaiting = System.currentTimeMillis() - scheduleTime;
            logger.debug("Will now execute one-off poll task {}, waited in thread pool for {}", task,
                    millisInThreadPoolWaiting);
            executeOneTimePoll(task, true);
        }, 0L, TimeUnit.MILLISECONDS);
        return future;
    }

    private void executeOneTimePoll(PollTask task, boolean oneOffTask) {
        ModbusSlaveEndpoint endpoint = task.getEndpoint();
        ModbusReadRequestBlueprint request = task.getRequest();
        ModbusReadCallback callback = task.getCallback();

        logger.trace("Executing task {} (oneOff={})! Waiting for connection", task, oneOffTask);
        long connectionBorrowStart = System.currentTimeMillis();
        Optional<ModbusSlaveConnection> connection = borrowConnection(endpoint); // might take a while
        logger.trace("Executing task {} (oneOff={})! Connection received in {} ms", task, oneOffTask,
                System.currentTimeMillis() - connectionBorrowStart);

        try {
            if (!connection.isPresent()) {
                logger.warn("Not connected to endpoint {} -- aborting request {}", endpoint, request);
                if (!oneOffTask) {
                    // Check poll task is still registered (this is all asynchronous)
                    verifyTaskIsRegistered(task);
                }
                callbackThreadPool.execute(() -> {
                    callback.onError(request, new ModbusConnectionException(endpoint));
                });
                return;
            }

            ModbusTransaction transaction = createTransactionForEndpoint(endpoint, connection);
            ModbusRequest libRequest = createRequest(request);
            transaction.setRequest(libRequest);
            if (!oneOffTask) {
                // Check poll task is still registered (this is all asynchronous)
                verifyTaskIsRegistered(task);
            }

            try {
                transaction.execute();
            } catch (ModbusException e) {
                // Note, one could catch ModbusIOException and ModbusSlaveException if more detailed
                // exception handling is required. For now, all exceptions are handled the same way with writes.
                logger.error("Error when executing read request ({}): {} {}", request, e.getClass().getName(),
                        e.getMessage());
                invalidate(endpoint, connection);
                // set connection to null such that it is not returned to pool
                connection = Optional.empty();
                if (!oneOffTask) {
                    // Check poll task is still registered (this is all asynchronous)
                    verifyTaskIsRegistered(task);
                }
                callbackThreadPool.execute(() -> {
                    callback.onError(request, e);
                });
            }
            ModbusResponse response = transaction.getResponse();
            logger.trace("Response for read (FC={}, transaction ID={}) {}", response.getFunctionCode(),
                    response.getTransactionID(), response.getHexMessage());

            if (!oneOffTask) {
                // Check poll task is still registered (this is all asynchronous)
                verifyTaskIsRegistered(task);
            }

            if ((response.getTransactionID() != transaction.getTransactionID()) && !response.isHeadless()) {
                logger.warn(
                        "Transaction id of the response does not match request {}.  Endpoint {}. Connection: {}. Ignoring response.",
                        request, endpoint, connection);
                callbackThreadPool.execute(() -> {
                    callback.onError(request, new ModbusUnexpectedTransactionIdException());
                });
            } else {
                callbackThreadPool.execute(() -> {
                    invokeCallbackWithResponse(request, callback, response);
                });
            }
        } catch (PollTaskUnregistered e) {
            logger.warn("Poll task was unregistered -- not executing/proceeding with the poll", e);
        } finally {
            returnConnection(endpoint, connection);
        }
    }

    @Override
    public void registerRegularPoll(PollTask task, long pollPeriodMillis, long initialDelayMillis) {
        scheduledPollTasks.compute(task, (prevTask, prevFuture) -> {
            ScheduledFuture<?> future = scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
                logger.debug("Executing scheduled ({}ms) poll task {}", pollPeriodMillis, task);
                this.executeOneTimePoll(task, false);
            }, initialDelayMillis, pollPeriodMillis, TimeUnit.MILLISECONDS);

            // Unregister previous regular poll
            if (prevFuture != null) {
                unregisterRegularPoll(prevTask);
            }
            return future;
        });

    }

    @Override
    public boolean unregisterRegularPoll(PollTask task) {
        // cancel poller
        ScheduledFuture<?> future = scheduledPollTasks.remove(task);
        if (future == null) {
            // No such poll task
            logger.warn("Caller tried to unregister nonexisting poll task {}", task);
            return false;
        }
        logger.info("Unregistering regular poll task {} (interrupting if necessary)", task);

        // Make sure connections to this endpoint are closed when they are returned to pool (which
        // is usually pretty soon as transactions should be relatively short-lived)
        ModbusManagerImpl.connectionFactory.disconnectOnReturn(task.getEndpoint(), System.currentTimeMillis());

        future.cancel(true);

        logger.info("Poll task {} canceled", task);

        try {
            // Close all idle connections as well (they will be reconnected if necessary on borrow)
            connectionPool.clear(task.getEndpoint());
        } catch (Exception e) {
            logger.error("Could not clear poll task {} endpoint {}. Stack trace follows", task, task.getEndpoint(), e);
            return false;
        }

        return true;
    }

    @Override
    public ScheduledFuture<?> submitOneTimeWrite(WriteTask task) {
        long scheduleTime = System.currentTimeMillis();
        logger.debug("Scheduling one-off write task {}", task);
        ScheduledFuture<?> future = scheduledThreadPoolExecutor.schedule(() -> {
            long millisInThreadPoolWaiting = System.currentTimeMillis() - scheduleTime;
            logger.debug("Will now execute one-off write task {}, waited in thread pool for {}", task,
                    millisInThreadPoolWaiting);
            executeOneTimeWrite(task);
        }, 0L, TimeUnit.MILLISECONDS);
        return future;
    }

    private void executeOneTimeWrite(WriteTask task) {
        ModbusSlaveEndpoint endpoint = task.getEndpoint();
        ModbusWriteRequestBlueprint request = task.getRequest();
        ModbusWriteCallback callback = task.getCallback();
        Optional<ModbusSlaveConnection> connection = borrowConnection(endpoint);

        try {
            ModbusTransaction transaction = createTransactionForEndpoint(endpoint, connection);
            ModbusRequest libRequest = createRequest(request);
            transaction.setRequest(libRequest);
            try {
                transaction.execute();
            } catch (ModbusException e) {
                // Note, one could catch ModbusIOException and ModbusSlaveException if more detailed
                // exception handling is required. For now, all exceptions are handled the same way with writes.
                logger.error("Error when executing write request ({}): {} {}", request, e.getClass().getName(),
                        e.getMessage());
                invalidate(endpoint, connection);
                // set connection to null such that it is not returned to pool
                connection = Optional.empty();
                callback.onError(request, e);
            }
            ModbusResponse response = transaction.getResponse();
            logger.trace("Response for write (FC={}) {}", response.getFunctionCode(), response.getHexMessage());
            if ((response.getTransactionID() != transaction.getTransactionID()) && !response.isHeadless()) {
                logger.warn(
                        "Transaction id of the response does not match request {}.  Endpoint {}. Connection: {}. Ignoring response.",
                        request, endpoint, connection);
                callback.onError(request, new ModbusUnexpectedTransactionIdException());
            } else {
                callback.onWriteResponse(request, new ModbusResponseImpl(response));
            }
        } finally {
            returnConnection(endpoint, connection);
        }
    }

    public void setDefaultPoolConfigurationFactory(
            Function<ModbusSlaveEndpoint, EndpointPoolConfiguration> defaultPoolConfigurationFactory) {
        connectionFactory.setDefaultPoolConfigurationFactory(defaultPoolConfigurationFactory);
    }

    @Override
    public void setEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration) {
        connectionFactory.setEndpointPoolConfiguration(endpoint, configuration);
        for (ModbusManagerListener listener : listeners) {
            listener.onEndpointPoolConfigurationSet(endpoint, configuration);
        }
    }

    @Override
    public EndpointPoolConfiguration getEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint) {
        return connectionFactory.getEndpointPoolConfiguration(endpoint);
    }

    protected void activate(Map<String, Object> configProperties) {
        logger.info("Modbus manager activated");
    }

    protected void deactivate() {
        logger.info("Modbus manager deactivated");
    }

    @Override
    public void addListener(ModbusManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ModbusManagerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Set<PollTask> getRegisteredRegularPolls() {
        return this.scheduledPollTasks.keySet();
    }

}
