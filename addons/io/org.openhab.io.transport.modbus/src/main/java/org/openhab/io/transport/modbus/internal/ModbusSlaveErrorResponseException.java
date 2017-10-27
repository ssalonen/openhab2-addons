package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.ModbusTransportException;

import net.wimpi.modbus.ModbusSlaveException;

public class ModbusSlaveErrorResponseException extends ModbusTransportException {

    private static final long serialVersionUID = 6334580162425192133L;
    private int type;

    public ModbusSlaveErrorResponseException(ModbusSlaveException e) {
        type = e.getType();
    }

    @Override
    public String toString() {
        return String.format("ModbusSlaveErrorResponseException(error=%d)", type);
    }
}
