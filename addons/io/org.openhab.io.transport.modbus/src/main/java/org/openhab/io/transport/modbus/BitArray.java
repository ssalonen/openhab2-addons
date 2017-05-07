package org.openhab.io.transport.modbus;

/**
 * Class that implements a collection for
 * bits
 *
 * @author Sami Salonen
 */
public interface BitArray {
    /**
     * Returns the state of the bit at the given index
     *
     * Index 0 matches LSB (rightmost) bit
     * <p>
     *
     * @param index the index of the bit to be returned.
     *
     * @return true if the bit at the specified index is set,
     *         false otherwise.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    boolean getBit(int index);

    /**
     * Get number of bits stored in this instance
     *
     * @return
     */
    int size();
}
