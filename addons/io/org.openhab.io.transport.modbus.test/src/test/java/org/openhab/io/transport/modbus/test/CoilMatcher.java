package org.openhab.io.transport.modbus.test;

import java.util.Objects;
import java.util.stream.StreamSupport;

import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;

class CoilMatcher extends AbstractRequestComparer<ModbusWriteCoilRequestBlueprint> {

    private Boolean[] expectedCoils;

    public CoilMatcher(int expectedUnitId, int expectedAddress, int expectedMaxTries,
            ModbusWriteFunctionCode expectedFunctionCode, Boolean... expectedCoils) {
        super(expectedUnitId, expectedAddress, expectedFunctionCode, expectedMaxTries);
        this.expectedCoils = expectedCoils;
    }

    @Override
    protected boolean doMatchData(ModbusWriteCoilRequestBlueprint item) {
        Object[] actual = StreamSupport.stream(item.getCoils().spliterator(), false).toArray();
        return Objects.deepEquals(actual, expectedCoils);
    }
}