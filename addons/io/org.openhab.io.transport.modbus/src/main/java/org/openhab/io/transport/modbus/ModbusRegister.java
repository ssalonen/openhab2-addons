package org.openhab.io.transport.modbus;

public interface ModbusRegister {
    public byte[] getBytes();

    /**
     * Returns the value of this <tt>InputRegister</tt>.
     * The value is stored as <tt>int</tt> but should be
     * treated like a 16-bit word.
     *
     * @return the value as <tt>int</tt>.
     */
    public int getValue();

    /**
     * Returns the content of this <tt>Register</tt> as
     * unsigned 16-bit value (unsigned short).
     *
     * @return the content as unsigned short (<tt>int</tt>).
     */
    public int toUnsignedShort();
}
