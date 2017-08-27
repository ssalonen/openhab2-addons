package org.openhab.io.transport.modbus;

/**
 *
 * @author Sami Salonen
 *
 */
public class ModbusConstants {

    public static enum ValueType {
        BIT("bit", 1),
        INT8("int8", 8),
        UINT8("uint8", 8),
        INT16("int16", 16),
        UINT16("uint16", 16),
        INT32("int32", 32),
        UINT32("uint32", 32),
        FLOAT32("float32", 32),
        INT32_SWAP("int32_swap", 32),
        UINT32_SWAP("uint32_swap", 32),
        FLOAT32_SWAP("float32_swap", 32);

        private final String valueType;
        private final int bits;

        ValueType(String valueType, int bits) {
            this.valueType = valueType;
            this.bits = bits;
        }

        public int getBits() {
            return bits;
        }

        public String getValueType() {
            return valueType;
        }

    }

}
