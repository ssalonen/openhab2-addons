package org.openhab.binding.modbus.handler;

import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public interface ModbusPollerThingHandler {

    public PollTask getPollTask();
}
