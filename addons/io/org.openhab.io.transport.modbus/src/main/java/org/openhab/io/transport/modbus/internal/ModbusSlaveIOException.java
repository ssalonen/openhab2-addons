package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.ModbusTransportException;

import net.wimpi.modbus.ModbusIOException;

public class ModbusSlaveIOException extends ModbusTransportException {

    private static final long serialVersionUID = -8910463902857643468L;
    private ModbusIOException error;

    public ModbusSlaveIOException(ModbusIOException e) {
        this.error = e;
    }

    @Override
    public String toString() {
        return String.format("ModbusSlaveIOException(EOF=%s, message='%s')", error.isEOF(), error.getMessage());
    }
}
