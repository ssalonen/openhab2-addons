package org.openhab.binding.modbus.handler;

import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusEndpointThingHandler {

    public ModbusSlaveEndpoint asSlaveEndpoint();

    public int getSlaveId();
}
