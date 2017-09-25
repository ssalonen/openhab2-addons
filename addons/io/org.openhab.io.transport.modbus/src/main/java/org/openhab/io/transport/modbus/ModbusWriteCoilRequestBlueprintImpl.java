/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author Sami Salonen
 *
 */
public class ModbusWriteCoilRequestBlueprintImpl implements ModbusWriteCoilRequestBlueprint {

    private static StandardToStringStyle toStringStyle = new StandardToStringStyle();

    static {
        toStringStyle.setUseShortClassName(true);
    }

    private static class SingleBitArray implements BitArray {

        private boolean bit;

        public SingleBitArray(boolean bit) {
            this.bit = bit;
        }

        @Override
        public boolean getBit(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return bit;
        }

        @Override
        public int size() {
            return 1;
        }

    }

    private int slaveId;
    private int reference;
    private BitArray bits;
    private boolean writeMultiple;

    public ModbusWriteCoilRequestBlueprintImpl(int slaveId, int reference, boolean data, boolean writeMultiple) {
        this(slaveId, reference, new SingleBitArray(data), writeMultiple);
    }

    public ModbusWriteCoilRequestBlueprintImpl(int slaveId, int reference, BitArray data, boolean writeMultiple) {
        super();
        this.slaveId = slaveId;
        this.reference = reference;
        this.bits = data;
        this.writeMultiple = writeMultiple;

        if (!writeMultiple && bits.size() > 1) {
            throw new IllegalArgumentException("With multiple coils, writeMultiple must be true");
        }
        if (bits.size() == 0) {
            throw new IllegalArgumentException("Must have at least one bit");
        }
    }

    @Override
    public int getUnitID() {
        return slaveId;
    }

    @Override
    public int getReference() {
        return reference;
    }

    @Override
    public ModbusWriteFunctionCode getFunctionCode() {
        return writeMultiple ? ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS : ModbusWriteFunctionCode.WRITE_COIL;
    }

    @Override
    public BitArray getCoils() {
        return bits;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("slaveId", slaveId).append("reference", reference)
                .append("functionCode", getFunctionCode()).append("bits", bits).toString();
    }
}