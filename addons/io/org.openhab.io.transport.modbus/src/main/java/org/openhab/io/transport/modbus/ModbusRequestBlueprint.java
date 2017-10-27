package org.openhab.io.transport.modbus;

import net.wimpi.modbus.Modbus;

/**
 * Base interface for Modbus requests
 *
 * @author Sami Salonen
 * @see ModbusReadRequestBlueprint
 * @see ModbusWriteRequestBlueprint
 *
 */
public interface ModbusRequestBlueprint {

    /**
     * Returns the protocol identifier of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The identifier is a 2-byte (short) non negative
     * integer value valid in the range of 0-65535.
     * <p>
     *
     * @return the protocol identifier as <tt>int</tt>.
     */
    public default int getProtocolID() {
        return Modbus.DEFAULT_PROTOCOL_ID;
    }

    /**
     * Returns the unit identifier of this
     * <tt>ModbusMessage</tt> as <tt>int</tt>.<br>
     * The identifier is a 1-byte non negative
     * integer value valid in the range of 0-255.
     * <p>
     *
     * @return the unit identifier as <tt>int</tt>.
     */
    public int getUnitID();

    /**
     * Maximum number of tries to execute the request, when request fails
     *
     * For example, number 1 means on try only with no re-tries.
     *
     * @return number of maximum tries
     */
    public int getMaxTries();

}
