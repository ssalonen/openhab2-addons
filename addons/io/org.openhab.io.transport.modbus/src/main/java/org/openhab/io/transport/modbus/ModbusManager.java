package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public interface ModbusManager {

    public interface PollTask {
        // metadata, e.g. last polled?
        ModbusSlaveEndpoint getEndpoint();

        ModbusReadRequestBlueprint getMessage();

        ModbusReadCallback getCallback();
    }

    public void executeOneTimePoll(PollTask task);

    /**
     *
     * @param endpoint
     * @param message
     * @param config
     * @param pollPeriodMillis
     * @param initialDelayMillis
     * @return string identifier for the poll
     */
    void registerRegularPoll(PollTask task, long pollPeriodMillis, long initialDelayMillis);

    public boolean unregisterRegularPoll(PollTask task);

    public void writeCommand(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint message, ModbusWriteCallback callback);

    /**
     * Configure general connection settings with a given endpoint
     *
     * @param endpoint
     * @param configuration
     */
    public void setEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration);

    public EndpointPoolConfiguration getEndpointPoolConfiguration(ModbusSlaveEndpoint endpoint);

}
