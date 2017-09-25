package org.openhab.io.transport.modbus.test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.json.WriteRequestJsonUtilities;

public class WriteRequestJsonUtilitiesTest {

    @Test
    public void testEmptyArray() {
        assertThat(WriteRequestJsonUtilities.fromJson(3, "[]").size(), is(equalTo(0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFC6NoRegister() {
        WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": []"//
                + "}]");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC6SingleRegister() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining(
                        (Matcher) new RegisterMatcher(55, 5412, ModbusWriteFunctionCode.WRITE_SINGLE_REGISTER, 3)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFC6MultipleRegisters() {
        WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 6,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4]"//
                + "}]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFC16NoRegister() {
        WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": []"//
                + "}]");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC16SingleRegister() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining(
                        (Matcher) new RegisterMatcher(55, 5412, ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS, 3)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC16MultipleRegisters() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 16,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4, 2]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new RegisterMatcher(55, 5412,
                        ModbusWriteFunctionCode.WRITE_MULTIPLE_REGISTERS, 3, 4, 2)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC5SingeCoil() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 5,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]" // value 3 (!= 0) is converted to boolean true
                + "}]").toArray(),
                arrayContaining((Matcher) new CoilMatcher(55, 5412, ModbusWriteFunctionCode.WRITE_COIL, true)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFC5MultipleCoils() {
        WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 5,"//
                + "\"address\": 5412,"//
                + "\"value\": [3, 4]"//
                + "}]");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC15SingleCoil() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [3]"//
                + "}]").toArray(),
                arrayContaining(
                        (Matcher) new CoilMatcher(55, 5412, ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS, true)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFC15MultipleCoils() {
        assertThat(WriteRequestJsonUtilities.fromJson(55, "[{"//
                + "\"functionCode\": 15,"//
                + "\"address\": 5412,"//
                + "\"value\": [1, 0, 5]"//
                + "}]").toArray(),
                arrayContaining((Matcher) new CoilMatcher(55, 5412, ModbusWriteFunctionCode.WRITE_MULTIPLE_COILS, true,
                        false, true)));
    }

    @Test(expected = IllegalStateException.class)
    public void testEmptyObject() {
        // we are expecting list, not object -> error
        WriteRequestJsonUtilities.fromJson(3, "{}");
    }

    @Test(expected = IllegalStateException.class)
    public void testNumber() {
        // we are expecting list, not primitive (number) -> error
        WriteRequestJsonUtilities.fromJson(3, "3");
    }

    @Test
    public void testEmptyList() {
        assertThat(WriteRequestJsonUtilities.fromJson(3, "[]").size(), is(equalTo(0)));
    }

}
