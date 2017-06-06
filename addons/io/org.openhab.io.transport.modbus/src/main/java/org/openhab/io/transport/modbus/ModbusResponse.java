package org.openhab.io.transport.modbus;

/**
 * Minimal representation of a modbus response.
 *
 * Only function code is exposed, which allows detecting MODBUS exception codes from normal codes.
 *
 * @author Sami Salonen
 *
 */
public interface ModbusResponse {

    public int getFunctionCode();

}
