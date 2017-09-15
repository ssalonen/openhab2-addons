package org.openhab.io.transport.modbus.internal;

import java.util.Arrays;

import org.openhab.io.transport.modbus.BitArray;

import net.wimpi.modbus.util.BitVector;

/**
 * <p>
 * BitArrayWrappingBitVector class.
 * </p>
 *
 * @author Sami Salonen
 */
public class BitArrayWrappingBitVector implements BitArray {

    private BitVector wrapped;

    public BitArrayWrappingBitVector(BitVector wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean getBit(int index) {
        if (index > this.wrapped.size()) {
            throw new IndexOutOfBoundsException();
        }
        return this.wrapped.getBit(index);
    }

    @Override
    public int size() {
        return this.wrapped.size();
    }

    @Override
    public String toString() {
        return new StringBuilder("BitArrayWrappingBitVector(bytes=").append(Arrays.toString(this.wrapped.getBytes()))
                .append(",bitsSize=").append(this.wrapped.size()).append(")").toString();
    }

}
