/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

/**
 *
 * @author Sami Salonen
 *
 */
public class ModbusWriteCoilRequestBlueprintImpl implements ModbusWriteCoilRequestBlueprint {

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

    public ModbusWriteCoilRequestBlueprintImpl(int slaveId, int reference, boolean data) {
        super();
        this.slaveId = slaveId;
        this.reference = reference;
        this.bits = new SingleBitArray(data);
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
        return ModbusWriteFunctionCode.WRITE_COIL;
    }

    @Override
    public BitArray getCoils() {
        return bits;
    }
}