package org.openhab.io.transport.modbus;

import java.util.concurrent.ScheduledFuture;

import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public interface ModbusManager {

    /**
     * Poll task represents modbus read request
     *
     * Must be hashable. HashCode and equals should be defined such that no two poll tasks are registered that are
     * equal.
     *
     * @author Sami Salonen
     *
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

    public ScheduledFuture<?> submitOneTimePoll(PollTask task);

    public void registerRegularPoll(PollTask task, long pollPeriodMillis, long initialDelayMillis);

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

}
