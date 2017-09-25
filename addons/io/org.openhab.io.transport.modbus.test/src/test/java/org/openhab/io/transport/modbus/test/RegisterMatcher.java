package org.openhab.io.transport.modbus.test;

import java.util.Objects;
import java.util.stream.StreamSupport;

import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;

class RegisterMatcher extends AbstractRequestComparer<ModbusWriteRegisterRequestBlueprint> {

    private Integer[] expectedRegisterValues;

    public RegisterMatcher(int expectedUnitId, int expectedAddress, ModbusWriteFunctionCode expectedFunctionCode,
            Integer... expectedRegisterValues) {
        super(expectedUnitId, expectedAddress, expectedFunctionCode);
        this.expectedRegisterValues = expectedRegisterValues;
    }

    @Override
    protected boolean doMatchData(ModbusWriteRegisterRequestBlueprint item) {
        Object[] actual = StreamSupport.stream(item.getRegisters().spliterator(), false).map(r -> r.getValue())
                .toArray();
        return Objects.deepEquals(actual, expectedRegisterValues);
    }
}