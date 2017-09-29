package org.openhab.binding.modbus.handler;

import java.util.function.Supplier;

import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusEndpointThingHandler {

    public ModbusSlaveEndpoint asSlaveEndpoint();

    public int getSlaveId();

    public Supplier<ModbusManager> getManagerRef();
}
