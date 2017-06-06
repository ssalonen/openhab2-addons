package org.openhab.binding.modbus.handler;

import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;

public interface ModbusPollerThingHandler {

    public PollTask getPollTask();

    public ModbusManagerReference getManagerRef();

    public ModbusEndpointThingHandler getEndpointThingHandler();
}
