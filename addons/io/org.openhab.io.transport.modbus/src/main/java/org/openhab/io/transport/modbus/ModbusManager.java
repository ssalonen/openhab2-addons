package org.openhab.io.transport.modbus;

import java.util.concurrent.ScheduledFuture;

import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * <p>ModbusManager interface.</p>
 *
 * @author Sami Salonen
 */
public interface ModbusManager {

    /**
     * Poll task represents modbus read request
     *
     * Must be hashable. HashCode and equals should be defined such that no two poll tasks are registered that are
     * equal.
     *
     * @author Sami Salonen
     *
     * @see ModbusManager.registerRegularPoll
     */
    public interface PollTask {
        /**
         * Gets endpoint associated with this poll task
         *
         * @return
         */
        ModbusSlaveEndpoint getEndpoint();

        /**
         * Gets request associated with this poll task
         *
         * @return
         */
        ModbusReadRequestBlueprint getRequest();

        /**
         * Gets callback that will be called with the response
         *
         * @return
         */
        ModbusReadCallback getCallback();
    }

    /**
     * Poll task represents modbus write request
     *
     * Unlike {@link PollTask}, this does not have to be hashable.
     *
     * @author Sami Salonen
     *
     */
    public interface WriteTask {
        /**
         * Gets endpoint associated with this write task
         *
         * @return
         */
        ModbusSlaveEndpoint getEndpoint();

        /**
         * Gets request associated with this write task
         *
         * @return
         */
        ModbusWriteRequestBlueprint getRequest();

        /**
         * Gets callback that will be called with the response
         *
         * @return
         */
        ModbusWriteCallback getCallback();
    }

    /**
     * Submit one-time poll task. The method returns immediately
     *
     * @param task
     * @return
     */
    public ScheduledFuture<?> submitOneTimePoll(PollTask task);

    /**
     * Register regularly polled task. The method returns immediately
     *
     * @param task
     * @return
     */
    public void registerRegularPoll(PollTask task, long pollPeriodMillis, long initialDelayMillis);

    /**
     * Unregister regularly polled task
     *
     * @param task
     * @return whether poll task was unregistered. Poll task is not unregistered in case of unexpected errors or
     *         non-existing poll task
     */
    public boolean unregisterRegularPoll(PollTask task);

    public ScheduledFuture<?> submitOneTimeWrite(WriteTask task);

    /**
     * Configure general connection settings with a given endpoint
     *
     * @param endpoint
     * @param configuration
     */
    public void setEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration);

    public EndpointPoolConfiguration getEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint);

    public void addListener(ModbusManagerListener listener);

    public void removeListener(ModbusManagerListener listener);

}
