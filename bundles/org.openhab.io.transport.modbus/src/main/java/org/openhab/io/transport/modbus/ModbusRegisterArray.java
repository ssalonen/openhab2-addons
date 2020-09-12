/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.transport.modbus;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Immutable {@link ModbusRegisterArray} implementation
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class ModbusRegisterArray {

    // private final ModbusRegister[] registers;
    private final ByteBuffer buffer;

    /**
     * Construct plain <code>ModbusRegister[]</code> array from register values
     *
     * @param registerValues register values, each <code>int</code> corresponding to one register
     * @return
     */
    public static ModbusRegister[] registersFromValues(int... registerValues) {

        ModbusRegister[] registers = new ModbusRegister[registerValues.length];
        for (int i = 0; i < registerValues.length; i++) {
            registers[i] = new ModbusRegister(registerValues[i]);
        }
        return registers;
    }

    private static int[] fromRegisters(ModbusRegister... registers) {
        int[] values = new int[registers.length];
        for (int i = 0; i < registers.length; i++) {
            values[i] = registers[i].getValue();
        }
        return values;
    }

    /**
     * Construct ModbusRegisterArrayImpl from array of {@link ModbusRegister}
     *
     * @deprecated Use other constructors instead to avoid unnecessary allocations
     *
     * @param registers
     */
    @Deprecated
    public ModbusRegisterArray(ModbusRegister[] registers) {
        this(fromRegisters(registers));
    }

    public ModbusRegisterArray(ByteBuffer buffer) {
        // TODO:copy
        this.buffer = buffer.asReadOnlyBuffer();
    }

    public ModbusRegisterArray(byte... bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        // TODO:Copy?
        buffer = ByteBuffer.wrap(bytes);
    }

    /**
     * Construct plain <code>ModbusRegisterArrayImpl</code> array from register values
     *
     * @param registerValues register values, each <code>int</code> corresponding to one register
     * @return
     */
    public ModbusRegisterArray(int... registerValues) {
        buffer = ByteBuffer.allocate(registerValues.length * 2);
        for (int registerValue : registerValues) {
            buffer.putInt(registerValue);
        }
    }

    /**
     * Return register at the given index
     *
     * Index 0 matches first register (lowest register index).
     * <p>
     *
     * @param index the index of the register to be returned.
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public ModbusRegister getRegister(int index) {
        return registers[index];
    }

    /**
     * Get number of registers stored in this instance
     *
     * @return
     */
    public int size() {
        return registers.length;
    }

    @Override
    public String toString() {
        if (registers.length == 0) {
            return "ModbusRegisterArrayImpl(<empty>)";
        }
        StringBuffer buffer = new StringBuffer(registers.length * 2).append("ModbusRegisterArrayImpl(");
        return appendHexString(buffer).append(')').toString();
    }

    /**
     * Get register data as a hex string
     *
     * For example, 04 45 00 00
     *
     * @return string representing the bytes of the register array
     */
    public String toHexString() {
        if (size() == 0) {
            return "";
        }
        // Initialize capacity to (n*2 + n-1), two chars per byte + spaces in between
        StringBuffer buffer = new StringBuffer(size() * 2 + (size() - 1));
        return appendHexString(buffer).toString();
    }

    /**
     * Appends the register data as hex string to the given StringBuffer
     *
     */
    public StringBuffer appendHexString(StringBuffer buffer) {
        IntStream.range(0, size()).forEachOrdered(index -> {
            getRegister(index).appendHexString(buffer);
            if (index < size() - 1) {
                buffer.append(' ');
            }
        });
        return buffer;
    }
}
