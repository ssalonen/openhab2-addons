package org.openhab.io.transport.modbus.test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import static org.junit.matchers.JUnitMatchers.*

import org.eclipse.smarthome.core.library.types.DecimalType
import org.eclipse.smarthome.core.types.Command
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.*
import org.openhab.io.transport.modbus.ModbusBitUtilities
import org.openhab.io.transport.modbus.ModbusRegisterArray


@RunWith(Parameterized.class)
class BitUtilitiesTest {
    //
    //    private static class RegisterWrapper implements ModbusRegisterArray {
    //        private ByteBuffer buf
    //
    //
    //        public RegisterWrapper(int[] data) {
    //            this.data = data;
    //            buf = ByteBuffer.allocate(data.length * 4);
    //            for(int bits32 : data) {
    //                buf.putInt(bits32)
    //            }
    //        }
    //
    //        @Override
    //        public ModbusRegister getRegister(int registerIndex) {
    //            int byteIndex = registerIndex*2;
    //            return new SimpleRegister(buf.get(byteIndex), buf.get(byteIndex+1));
    //        }
    //
    //        @Override
    //        public int size() {
    //            return buf.capacity() / 4;
    //        }
    //    }

    final Command command
    final String type
    final Object expectedResult

    @Rule
    public final ExpectedException shouldThrow = ExpectedException.none();


    public BitUtilitiesTest(Command command, String type, Object expectedResult) {
        this.command = command
        this.type = type
        this.expectedResult = expectedResult  // Exception or array of 16bit integers
    }

    @Parameters
    public static Collection<Object[]> data() {
        [
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_BIT,
                IllegalArgumentException
            ],
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_INT8,
                IllegalArgumentException
            ],
            //
            // INT16
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [1]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [1]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [2]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [-1004],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [64000],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (int) -> 0x1174 (short) = 4468
                ModbusBitUtilities.VALUE_TYPE_INT16,
                [4468],
            ],
            //
            // UINT16 (same as INT16)
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [1]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [1]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [2]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [-1004],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [64000],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                ModbusBitUtilities.VALUE_TYPE_UINT16,
                [0x1174],
            ],
            //
            // INT32
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [0, 1]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [0, 1]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [0, 2]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_INT32,
                // -1004 = 0xFFFFFC14 (32bit) =
                [0xFFFF, 0xFC14],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [0, 64000],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [1, 4468],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                // 5000000000 -> 0x12a05f200 () -> 0x1174 (16bit)
                ModbusBitUtilities.VALUE_TYPE_INT32,
                [0x2a05, 0xf200],
            ],
            //
            // UINT32 (same as INT32)
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [0, 1]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [0, 1]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [0, 2]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                // -1004 = 0xFFFFFC14 (32bit) =
                [0xFFFF, 0xFC14],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [0, 64000],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (32bit) -> 0x1174 (16bit)
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [1, 4468],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                // 5000000000 -> 0x12a05f200 () -> 0x1174 (16bit)
                ModbusBitUtilities.VALUE_TYPE_UINT32,
                [0x2a05, 0xf200],
            ],
            //
            // INT32_SWAP
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [1, 0]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [1, 0]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [2, 0]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                // -1004 = 0xFFFFFC14 (32bit)
                [0xFC14, 0xFFFF],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [64000, 0],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (32bit)
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [4468, 1],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                // 5000000000 -> 0x12a05f200
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP,
                [0xf200, 0x2a05],
            ],
            //
            // UINT32_SWAP (same as INT32_SWAP)
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [1, 0]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [1, 0]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [2, 0]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                // -1004 = 0xFFFFFC14 (32bit)
                [0xFC14, 0xFFFF],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [64000, 0],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                // 70004 -> 0x00011174 (32bit)
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [4468, 1],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                // 5000000000 -> 0x12a05f200
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP,
                [0xf200, 0x2a05],
            ],
            //
            // FLOAT32
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x3F80, 0x0000]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x3FCC, 0xCCCD]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x4026, 0x6666]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0xC47B, 0x199A],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x477A, 0x0000],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x4788, 0xBA33],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                [0x4F95, 0x02F9],
            ],
            //
            // FLOAT32_SWAP
            //
            [
                new DecimalType("1.0"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0x0000, 0x3F80]
            ],
            [
                new DecimalType("1.6"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0xCCCD, 0x3FCC]
            ],
            [
                new DecimalType("2.6"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0x6666, 0x4026]
            ],
            [
                new DecimalType("-1004.4"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0x199A, 0xC47B],
            ],
            [
                new DecimalType("64000"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0x0000, 0x477A],
            ],
            [
                // out of bounds of unsigned 16bit (0 to 65,535)
                new DecimalType("70004.4"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0xBA33, 0x4788],
            ],
            [
                // out of bounds of unsigned 32bit (0 to 4,294,967,295)
                new DecimalType("5000000000"),
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP,
                [0x02F9, 0x4F95],
            ],
        ]*.toArray();
    }


    @Test
    void testCommandToRegisters() {
        if(expectedResult instanceof Class && Exception.isAssignableFrom(expectedResult)){
            shouldThrow.expect(expectedResult);
        }

        ModbusRegisterArray registers = ModbusBitUtilities.commandToRegisters this.command, this.type
        short[] expectedRegisters = (short[]) expectedResult

        assertThat registers.size(), is(equalTo(expectedRegisters.length))
        for (int i = 0; i < expectedRegisters.size(); i++){
            int expectedRegisterDataUnsigned = expectedRegisters[i] & 0xffff
            int actual = registers.getRegister(i).getValue()

            assertThat "i=${i}, command=${this.command}, type=${this.type}", actual, is(equalTo(expectedRegisterDataUnsigned))
        }
    }



}
