package org.openhab.io.transport.modbus;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

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

        private final @NonNull String configValue;
        private final int bits;

        ValueType(@NonNull String configValue, int bits) {
            this.configValue = configValue;
            this.bits = bits;
        }

        public int getBits() {
            return bits;
        }

        public @NonNull String getConfigValue() {
            return configValue;
        }

        @Override
        public String toString() {
            return getConfigValue();
        }

        @SuppressWarnings("null")
        public static @NonNull ValueType fromConfigValue(String configValueType) throws IllegalArgumentException {
            return Stream.of(ValueType.values()).filter(v -> v.getConfigValue().equals(configValueType)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid valueType"));
        }
    }

}
