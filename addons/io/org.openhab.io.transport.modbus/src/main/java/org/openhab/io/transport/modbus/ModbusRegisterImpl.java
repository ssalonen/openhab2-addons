package org.openhab.io.transport.modbus;

import net.wimpi.modbus.procimg.SimpleInputRegister;

public class ModbusRegisterImpl implements ModbusRegister {

    private SimpleInputRegister wrapped;

    /**
     * Constructs a new <tt>BytesRegister</tt> instance.
     *
     * @param b1 the first (hi) byte of the word.
     * @param b2 the second (low) byte of the word.
     */
    public ModbusRegisterImpl(byte b1, byte b2) {
        wrapped = new SimpleInputRegister(b1, b2);
    }

    @Override
    public byte[] getBytes() {
        return wrapped.toBytes();
    }

    @Override
    public int getValue() {
        return wrapped.getValue();
    }

    @Override
    public int toUnsignedShort() {
        return wrapped.toUnsignedShort();
    }

}
