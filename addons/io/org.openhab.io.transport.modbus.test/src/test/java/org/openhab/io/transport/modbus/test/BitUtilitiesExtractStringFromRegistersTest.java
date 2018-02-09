package org.openhab.io.transport.modbus.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.smarthome.core.library.types.StringType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusRegisterArrayImpl;
import org.openhab.io.transport.modbus.ModbusRegisterImpl;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class BitUtilitiesExtractStringFromRegistersTest {

    final ModbusRegisterArray registers;
    final int index;
    final int length;
    final Object expectedResult;

    @Rule
    public final ExpectedException shouldThrow = ExpectedException.none();

    public BitUtilitiesExtractStringFromRegistersTest(Object expectedResult, ModbusRegisterArray registers, int index,
            int length) {
        this.registers = registers;
        this.index = index;
        this.length = length;
        this.expectedResult = expectedResult;
    }

    private static ModbusRegisterArray shortArrayToRegisterArray(int... arr) {
        ModbusRegister[] tmp = new ModbusRegister[0];
        return new ModbusRegisterArrayImpl(IntStream.of(arr).mapToObj(val -> {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort((short) val);
            return new ModbusRegisterImpl(buffer.get(0), buffer.get(1));
        }).collect(Collectors.toList()).toArray(tmp));
    }

    @Parameters
    public static Collection<Object[]> data() {
        return ImmutableList.of(new Object[] { new StringType(""), shortArrayToRegisterArray(0), 0, 0 },
                new Object[] { new StringType("hello"), shortArrayToRegisterArray(0x6865, 0x6c6c, 0x6f00), 0, 5 },
                new Object[] { new StringType("hello "), shortArrayToRegisterArray(0, 0, 0x6865, 0x6c6c, 0x6f20, 0, 0),
                        2, 6 },

                // Invalid values
                new Object[] { IllegalArgumentException.class, shortArrayToRegisterArray(0, 0), 2, 4 },
                new Object[] { IllegalArgumentException.class, shortArrayToRegisterArray(0, 0), 0, -1 },
                new Object[] { IllegalArgumentException.class, shortArrayToRegisterArray(0, 0), 0, 5 });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testExtractStringFromRegisters() {
        if (expectedResult instanceof Class && Exception.class.isAssignableFrom((Class) expectedResult)) {
            shouldThrow.expect((Class) expectedResult);
        }

        StringType actualState = ModbusBitUtilities.extractStringFromRegisters(this.registers, this.index, this.length);
        assertThat(String.format("registers=%s, index=%d, length=%d", registers, index, length), actualState,
                is(equalTo(expectedResult)));
    }

}
