package org.openhab.io.transport.modbus.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusRegisterArrayImpl;
import org.openhab.io.transport.modbus.ModbusRegisterImpl;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
class BitUtilitiesExtractStateFromRegistersTest {

    final ModbusRegisterArray registers;
    final ValueType type;
    final int index;
    final Object expectedResult;

    @Rule
    public final ExpectedException shouldThrow = ExpectedException.none();

    public BitUtilitiesExtractStateFromRegistersTest(Object expectedResult, ValueType type,
            ArrayList<Integer> registers, int index) {
        this.registers = shortArrayToRegisterArray(registers.toArray(new Integer[0]));
        this.index = index;
        this.type = type;
        this.expectedResult = expectedResult; // Exception or DecimalType
    }

    private static short[] shorts(int... ints) {
        short[] shorts = new short[ints.length];
        for (int i : ints) {
            short s = (short) i;
            shorts[i] = s;
        }
        return shorts;
    }

    private static ModbusRegisterArray shortArrayToRegisterArray(Integer[] arr) {
        ModbusRegister[] tmp = new ModbusRegister[0];
        return new ModbusRegisterArrayImpl(Stream.of(arr).map(val -> {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort((short) (int) val);
            return new ModbusRegisterImpl(buffer.get(0), buffer.get(1));
        }).collect(Collectors.toList()).toArray(tmp));
    }

    @Parameters
    public static Collection<Object[]> data() {
        return ImmutableList.of(
                //
                // INT16
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT16, shorts(1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT16, shorts(2), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shorts(-1004), 0 },
                new Object[] { new DecimalType("-1536"), ValueType.INT16, shorts(64000), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shorts(4, -1004), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT16, shorts(-1004, 4), 0 },
                //
                // UINT16
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT16, shorts(1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT16, shorts(2), 0 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shorts(-1004), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT16, shorts(64000), 0 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shorts(4, -1004), 1 },
                new Object[] { new DecimalType("64532"), ValueType.UINT16, shorts(-1004, 4), 0 },
                //
                // INT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT32, shorts(0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT32, shorts(0, 2), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT32, shorts(0, 64000), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0x4, 0xFFFF, 0xFC14), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0xFFFF, 0xFC14, 0x4), 0 },
                //
                // UINT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT32, shorts(0, 1), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT32, shorts(0, 2), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0xFFFF, 0xFC14), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT32, shorts(0, 64000), 0 }, new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType("70004"),
                        // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                        ValueType.UINT32, shorts(1, 4468), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0xFFFF, 0xFC14, 0x5), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0x5, 0xFFFF, 0xFC14), 1 },
                //
                // INT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.INT32_SWAP, shorts(1, 0), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.INT32_SWAP, shorts(2, 0), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0xFC14, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.INT32_SWAP, shorts(64000, 0), 0 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0x4, 0xFC14, 0xFFFF), 1 },
                new Object[] { new DecimalType("-1004"), ValueType.INT32_SWAP,
                        // -1004 = 0xFFFFFC14 (32bit) =
                        shorts(0xFC14, 0xFFFF, 0x4), 0 },
                //
                // UINT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.UINT32_SWAP, shorts(1, 0), 0 },
                new Object[] { new DecimalType("2.0"), ValueType.UINT32_SWAP, shorts(2, 0), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0xFC14, 0xFFFF), 0 },
                new Object[] { new DecimalType("64000"), ValueType.UINT32_SWAP, shorts(64000, 0), 0 }, new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType("70004"),
                        // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                        ValueType.UINT32_SWAP, shorts(4468, 1), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0xFC14, 0xFFFF, 0x5), 0 },
                new Object[] { new DecimalType("4294966292"), ValueType.UINT32_SWAP,
                        // 4294966292 = 0xFFFFFC14 (32bit) =
                        shorts(0x5, 0xFC14, 0xFFFF), 1 },
                //
                // FLOAT32
                //
                new Object[] { new DecimalType("1.0"), ValueType.FLOAT32, shorts(0x3F80, 0x0000), 0 },
                new Object[] { new DecimalType(1.6f), ValueType.FLOAT32, shorts(0x3FCC, 0xCCCD), 0 },
                new Object[] { new DecimalType(2.6f), ValueType.FLOAT32, shorts(0x4026, 0x6666), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32, shorts(0xC47B, 0x199A), 0 },
                new Object[] { new DecimalType("64000"), ValueType.FLOAT32, shorts(0x477A, 0x0000), 0 }, new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType(70004.4f), ValueType.FLOAT32, shorts(0x4788, 0xBA33), 0 },
                new Object[] {
                        // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                        new DecimalType("5000000000"), ValueType.FLOAT32, shorts(0x4F95, 0x02F9), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32, shorts(0x4, 0xC47B, 0x199A), 1 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32, shorts(0xC47B, 0x199A, 0x4), 0 },
                //
                // FLOAT32_SWAP
                //
                new Object[] { new DecimalType("1.0"), ValueType.FLOAT32_SWAP, shorts(0x0000, 0x3F80), 0 },
                new Object[] { new DecimalType(1.6f), ValueType.FLOAT32_SWAP, shorts(0xCCCD, 0x3FCC), 0 },
                new Object[] { new DecimalType(2.6f), ValueType.FLOAT32_SWAP, shorts(0x6666, 0x4026), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP, shorts(0x199A, 0xC47B), 0 },
                new Object[] { new DecimalType("64000"), ValueType.FLOAT32_SWAP, shorts(0x0000, 0x477A), 0 },
                new Object[] {
                        // out of bounds of unsigned 16bit (0 to 65,535)
                        new DecimalType(70004.4f), ValueType.FLOAT32_SWAP, shorts(0xBA33, 0x4788), 0 },
                new Object[] {
                        // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                        new DecimalType("5000000000"), ValueType.FLOAT32_SWAP, shorts(0x02F9, 0x4F95), 0 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP, shorts(0x4, 0x199A, 0xC47B), 1 },
                new Object[] { new DecimalType(-1004.4f), ValueType.FLOAT32_SWAP, shorts(0x199A, 0xC47B, 0x4), 0 });

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void testCommandToRegisters() {
        if (expectedResult instanceof Class && Exception.class.isAssignableFrom((Class) expectedResult)) {
            shouldThrow.expect((Class) expectedResult);
        }

        DecimalType actualState = ModbusBitUtilities.extractStateFromRegisters(this.registers, this.index, this.type);
        DecimalType expectedState = (DecimalType) this.expectedResult;
        assertThat("registers=${this.registers}, index=${this.index}, type=${this.type}", actualState,
                is(equalTo(expectedState)));
    }
}
