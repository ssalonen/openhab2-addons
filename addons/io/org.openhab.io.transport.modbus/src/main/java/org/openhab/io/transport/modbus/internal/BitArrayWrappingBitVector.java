/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
    private int safeSize;

    public BitArrayWrappingBitVector(BitVector wrapped, int safeSize) {
        this.wrapped = wrapped;
        this.safeSize = safeSize;
    }

    @Override
    public boolean getBit(int index) {
        if (index >= size()) {
            throw new IndexOutOfBoundsException();
        }
        return this.wrapped.getBit(index);
    }

    @Override
    public int size() {
        return safeSize;
    }

    @Override
    public String toString() {
        return new StringBuilder("BitArrayWrappingBitVector(bytes=").append(Arrays.toString(this.wrapped.getBytes()))
                .append(",bitsSize=").append(safeSize).append(")").toString();
    }

}
