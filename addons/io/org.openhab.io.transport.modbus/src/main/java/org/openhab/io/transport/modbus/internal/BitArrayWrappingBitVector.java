package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.BitArray;

import net.wimpi.modbus.util.BitVector;

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

}
