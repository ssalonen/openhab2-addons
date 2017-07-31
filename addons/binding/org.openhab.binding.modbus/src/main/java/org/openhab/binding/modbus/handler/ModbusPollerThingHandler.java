package org.openhab.binding.modbus.handler;

import java.util.function.Supplier;

import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusPollerThingHandler {

    public PollTask getPollTask();

    public Supplier<ModbusManager> getManagerRef();

    public ModbusEndpointThingHandler getEndpointThingHandler();
}
