/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.transport.modbus;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.Command;

/**
 * Utilities for working with binary data.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class ModbusBitUtilities {

    private static final BigInteger INT64_MAX = BigInteger.valueOf(0xffff_ffff_ffff_ffffL);

    /**
     * Read data from registers and convert the result to DecimalType
     * Interpretation of <tt>index</tt> goes as follows depending on type
     *
     * BIT:
     * - a single bit is read from the registers
     * - indices between 0...15 (inclusive) represent bits of the first register
     * - indices between 16...31 (inclusive) represent bits of the second register, etc.
     * - index 0 refers to the least significant bit of the first register
     * - index 1 refers to the second least significant bit of the first register, etc.
     * INT8:
     * - a byte (8 bits) from the registers is interpreted as signed integer
     * - index 0 refers to low byte of the first register, 1 high byte of first register
     * - index 2 refers to low byte of the second register, 3 high byte of second register, etc.
     * - it is assumed that each high and low byte is encoded in most significant bit first order
     * UINT8:
     * - same as INT8 except value is interpreted as unsigned integer
     * INT16:
     * - register with index (counting from zero) is interpreted as 16 bit signed integer.
     * - it is assumed that each register is encoded in most significant bit first order
     * UINT16:
     * - same as INT16 except value is interpreted as unsigned integer
     * INT32:
     * - registers (index) and (index + 1) are interpreted as signed 32bit integer.
     * - it assumed that the first register contains the most significant 16 bits
     * - it is assumed that each register is encoded in most significant bit first order
     * INT32_SWAP:
     * - Same as INT32 but registers swapped
     * UINT32:
     * - same as INT32 except value is interpreted as unsigned integer
     * UINT32_SWAP:
     * - same as INT32_SWAP except value is interpreted as unsigned integer
     * FLOAT32:
     * - registers (index) and (index + 1) are interpreted as signed 32bit floating point number.
     * - it assumed that the first register contains the most significant 16 bits
     * - it is assumed that each register is encoded in most significant bit first order
     * - floating point NaN and infinity will return as empty optional
     * FLOAT32_SWAP:
     * - Same as FLOAT32 but registers swapped
     * INT64:
     * - registers (index), (index + 1), (index + 2), (index + 3) are interpreted as signed 64bit integer.
     * - it assumed that the first register contains the most significant 16 bits
     * - it is assumed that each register is encoded in most significant bit first order
     * INT64_SWAP:
     * - same as INT64 but registers swapped, that is, registers (index + 3), (index + 2), (index + 1), (index + 1) are
     * interpreted as signed 64bit integer
     * UINT64:
     * - same as INT64 except value is interpreted as unsigned integer
     * UINT64_SWAP:
     * - same as INT64_SWAP except value is interpreted as unsigned integer
     *
     * @param registers list of registers, each register represent 16bit of data
     * @param index zero based item index. Interpretation of this depends on type, see examples above.
     *            With type larger or equal to 16 bits, the index tells the register index to start reading
     *            from.
     *            With type less than 16 bits, the index tells the N'th item to read from the registers.
     * @param type item type, e.g. unsigned 16bit integer (<tt>ModbusBindingProvider.ValueType.UINT16</tt>)
     * @return number representation queried value, <tt>DecimalType</tt>. Empty optional is returned
     *         with NaN and infinity floating point values
     * @throws NotImplementedException in cases where implementation is lacking for the type. This can be considered a
     *             bug
     * @throws IllegalArgumentException when <tt>index</tt> is out of bounds of registers
     *
     */
    public static Optional<DecimalType> extractStateFromRegisters(ModbusRegisterArray registers, int index,
            ModbusConstants.ValueType type) {
        int endBitIndex = (type.getBits() >= 16 ? 16 * index : type.getBits() * index) + type.getBits() - 1;
        // each register has 16 bits
        int lastValidIndex = registers.size() * 16 - 1;
        if (endBitIndex > lastValidIndex || index < 0) {
            throw new IllegalArgumentException(
                    String.format("Index=%d with type=%s is out-of-bounds given registers of size %d", index, type,
                            registers.size()));
        }
        byte[] bytes = registers.getBytes();
        switch (type) {
            case BIT: {
                int registerIndex = index / 16;
                boolean hiByte = index % 16 >= 8;
                int indexWithinByte = index % 8;
                int byteIndex = 2 * registerIndex + (hiByte ? 0 : 1);
                return Optional.of(new DecimalType((bytes[byteIndex] >>> indexWithinByte) & 1));
            }
            case INT8: {
                int registerIndex = index / 2;
                boolean hiByte = index % 2 == 1;
                int byteIndex = 2 * registerIndex + (hiByte ? 0 : 1);
                int signed = bytes[byteIndex];
                return Optional.of(new DecimalType(signed));
            }
            case UINT8: {
                int registerIndex = index / 2;
                boolean hiByte = index % 2 == 1;
                int byteIndex = 2 * registerIndex + (hiByte ? 0 : 1);
                int signed = bytes[byteIndex];
                int unsigned = signed & 0xff;
                assert unsigned >= 0;
                return Optional.of(new DecimalType(unsigned));
            }
            case INT16: {
                int hi = (bytes[index * 2] & 0xff);
                int lo = (bytes[index * 2 + 1] & 0xff);
                short signed = (short) ((hi << 8) | lo);
                return Optional.of(new DecimalType(signed));
            }
            case UINT16: {
                int hi = bytes[index * 2] & 0xff;
                int lo = bytes[index * 2 + 1] & 0xff;
                short signed = (short) ((hi << 8) | lo);
                int unsigned = signed & 0xffff;
                assert unsigned >= 0;
                return Optional.of(new DecimalType(unsigned));
            }
            case INT32: {
                int hi1 = bytes[(index + 0) * 2] & 0xff;
                int lo1 = bytes[(index + 0) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 1) * 2] & 0xff;
                int lo2 = bytes[(index + 1) * 2 + 1] & 0xff;
                int signed = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                return Optional.of(new DecimalType(signed));
            }
            case UINT32: {
                int hi1 = bytes[(index + 0) * 2] & 0xff;
                int lo1 = bytes[(index + 0) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 1) * 2] & 0xff;
                int lo2 = bytes[(index + 1) * 2 + 1] & 0xff;
                int signed = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                long unsigned = signed & 0xffff_ffffL;
                assert unsigned >= 0;
                return Optional.of(new DecimalType(unsigned));
            }
            case FLOAT32: {
                int hi1 = bytes[(index + 0) * 2] & 0xff;
                int lo1 = bytes[(index + 0) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 1) * 2] & 0xff;
                int lo2 = bytes[(index + 1) * 2 + 1] & 0xff;
                int bits32 = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                try {
                    return Optional.of(new DecimalType(Float.intBitsToFloat(bits32)));
                } catch (NumberFormatException e) {
                    // floating point NaN or infinity encountered
                    return Optional.empty();
                }
            }
            case INT64: {
                byte hi1 = (byte) (bytes[(index + 0) * 2] & 0xff);
                byte lo1 = (byte) (bytes[(index + 0) * 2 + 1] & 0xff);
                byte hi2 = (byte) (bytes[(index + 1) * 2] & 0xff);
                byte lo2 = (byte) (bytes[(index + 1) * 2 + 1] & 0xff);
                byte hi3 = (byte) (bytes[(index + 2) * 2] & 0xff);
                byte lo3 = (byte) (bytes[(index + 2) * 2 + 1] & 0xff);
                byte hi4 = (byte) (bytes[(index + 3) * 2] & 0xff);
                byte lo4 = (byte) (bytes[(index + 3) * 2 + 1] & 0xff);
                return Optional.of(new DecimalType(
                        new BigDecimal(new BigInteger(new byte[] { hi1, lo1, hi2, lo2, hi3, lo3, hi4, lo4 }))));
            }
            case UINT64: {
                byte hi1 = (byte) (bytes[(index + 0) * 2] & 0xff);
                byte lo1 = (byte) (bytes[(index + 0) * 2 + 1] & 0xff);
                byte hi2 = (byte) (bytes[(index + 1) * 2] & 0xff);
                byte lo2 = (byte) (bytes[(index + 1) * 2 + 1] & 0xff);
                byte hi3 = (byte) (bytes[(index + 2) * 2] & 0xff);
                byte lo3 = (byte) (bytes[(index + 2) * 2 + 1] & 0xff);
                byte hi4 = (byte) (bytes[(index + 3) * 2] & 0xff);
                byte lo4 = (byte) (bytes[(index + 3) * 2 + 1] & 0xff);
                return Optional.of(new DecimalType(
                        new BigDecimal(new BigInteger(1, new byte[] { hi1, lo1, hi2, lo2, hi3, lo3, hi4, lo4 }))));
            }
            case INT32_SWAP: {
                // swapped order of registers, high 16 bits *follow* low 16 bits
                int hi1 = bytes[(index + 1) * 2] & 0xff;
                int lo1 = bytes[(index + 1) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 0) * 2] & 0xff;
                int lo2 = bytes[(index + 0) * 2 + 1] & 0xff;
                int signed = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                return Optional.of(new DecimalType(signed));
            }
            case UINT32_SWAP: {
                // swapped order of registers, high 16 bits *follow* low 16 bits
                int hi1 = bytes[(index + 1) * 2] & 0xff;
                int lo1 = bytes[(index + 1) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 0) * 2] & 0xff;
                int lo2 = bytes[(index + 0) * 2 + 1] & 0xff;
                int signed = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                long unsigned = signed & 0xffff_ffffL;
                assert unsigned >= 0;
                return Optional.of(new DecimalType(unsigned));
            }
            case FLOAT32_SWAP: {
                // swapped order of registers, high 16 bits *follow* low 16 bits
                int hi1 = bytes[(index + 1) * 2] & 0xff;
                int lo1 = bytes[(index + 1) * 2 + 1] & 0xff;
                int hi2 = bytes[(index + 0) * 2] & 0xff;
                int lo2 = bytes[(index + 0) * 2 + 1] & 0xff;
                int bits32 = (hi1 << 24) | (lo1 << 16) | (hi2 << 8) | lo2;
                try {
                    return Optional.of(new DecimalType(Float.intBitsToFloat(bits32)));
                } catch (NumberFormatException e) {
                    // floating point NaN or infinity encountered
                    return Optional.empty();
                }
            }
            case INT64_SWAP: {
                // Swapped order of registers
                byte hi1 = (byte) (bytes[(index + 3) * 2] & 0xff);
                byte lo1 = (byte) (bytes[(index + 3) * 2 + 1] & 0xff);
                byte hi2 = (byte) (bytes[(index + 2) * 2] & 0xff);
                byte lo2 = (byte) (bytes[(index + 2) * 2 + 1] & 0xff);
                byte hi3 = (byte) (bytes[(index + 1) * 2] & 0xff);
                byte lo3 = (byte) (bytes[(index + 1) * 2 + 1] & 0xff);
                byte hi4 = (byte) (bytes[(index + 0) * 2] & 0xff);
                byte lo4 = (byte) (bytes[(index + 0) * 2 + 1] & 0xff);
                return Optional.of(new DecimalType(
                        new BigDecimal(new BigInteger(new byte[] { hi1, lo1, hi2, lo2, hi3, lo3, hi4, lo4 }))));
            }
            case UINT64_SWAP: {
                // Swapped order of registers
                byte hi1 = (byte) (bytes[(index + 3) * 2] & 0xff);
                byte lo1 = (byte) (bytes[(index + 3) * 2 + 1] & 0xff);
                byte hi2 = (byte) (bytes[(index + 2) * 2] & 0xff);
                byte lo2 = (byte) (bytes[(index + 2) * 2 + 1] & 0xff);
                byte hi3 = (byte) (bytes[(index + 1) * 2] & 0xff);
                byte lo3 = (byte) (bytes[(index + 1) * 2 + 1] & 0xff);
                byte hi4 = (byte) (bytes[(index + 0) * 2] & 0xff);
                byte lo4 = (byte) (bytes[(index + 0) * 2 + 1] & 0xff);
                return Optional.of(new DecimalType(
                        new BigDecimal(new BigInteger(1, new byte[] { hi1, lo1, hi2, lo2, hi3, lo3, hi4, lo4 }))));
            }
            default:
                throw new IllegalArgumentException(type.getConfigValue());
        }
    }

    /**
     * Read data from registers and convert the result to StringType
     * Strings should start the the first byte of a register, but could
     * have an odd number of characters.
     * Raw byte array values are converted using the charset parameter
     * and a maximum of length bytes are read. However reading stops at the first
     * NUL byte encountered.
     *
     * @param registers list of registers, each register represent 16bit of data
     * @param index zero based register index. Registers are handled as 16bit registers,
     *            this parameter defines the starting register.
     * @param length maximum length of string in 8bit characters.
     * @param charset the character set used to construct the string.
     * @return string representation queried value
     * @throws IllegalArgumentException when <tt>index</tt> is out of bounds of registers
     */
    public static StringType extractStringFromRegisters(ModbusRegisterArray registers, int index, int length,
            Charset charset) {
        if (index * 2 + length > registers.size() * 2) {
            throw new IllegalArgumentException(
                    String.format("Index=%d with length=%d is out-of-bounds given registers of size %d", index, length,
                            registers.size()));
        }
        if (index < 0) {
            throw new IllegalArgumentException("Negative index values are not supported");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Negative string length is not supported");
        }
        byte[] buff = new byte[length];

        int src = index;
        int dest;
        for (dest = 0; dest < length; dest++) {

            byte chr;
            if (dest % 2 == 0) {
                chr = (byte) ((registers.getRegister(src).getValue() >> 8));
            } else {
                chr = (byte) (registers.getRegister(src).getValue() & 0xff);
                src++;
            }
            if (chr == 0) {
                break;
            }
            buff[dest] = chr;
        }
        return new StringType(new String(buff, 0, dest, charset));
    }

    /**
     * Convert command to array of registers using a specific value type
     *
     * @param command command to be converted
     * @param type value type to use in conversion
     * @return array of registers
     * @throws NotImplementedException in cases where implementation is lacking for the type. This is thrown with 1-bit
     *             and 8-bit value types
     */
    public static ModbusRegisterArray commandToRegisters(Command command, ModbusConstants.ValueType type) {
        DecimalType numericCommand;
        if (command instanceof OnOffType || command instanceof OpenClosedType) {
            numericCommand = translateCommand2Boolean(command).get() ? new DecimalType(BigDecimal.ONE)
                    : DecimalType.ZERO;
        } else if (command instanceof DecimalType) {
            numericCommand = (DecimalType) command;
        } else {
            throw new NotImplementedException(String.format(
                    "Command '%s' of class '%s' cannot be converted to registers. Please use OnOffType, OpenClosedType, or DecimalType commands.",
                    command, command.getClass().getName()));
        }
        if (type.getBits() != 16 && type.getBits() != 32 && type.getBits() != 64) {
            throw new IllegalArgumentException(String.format(
                    "Illegal type=%s (bits=%d). Only 16bit and 32bit types are supported", type, type.getBits()));
        }
        switch (type) {
            case INT16:
            case UINT16: {
                short shortValue = numericCommand.shortValue();
                // big endian byte ordering
                byte b1 = (byte) (shortValue >> 8);
                byte b2 = (byte) shortValue;

                ModbusRegister register = new ModbusRegister(b1, b2);
                return new ModbusRegisterArray(new ModbusRegister[] { register });
            }
            case INT32:
            case UINT32: {
                int intValue = numericCommand.intValue();
                // big endian byte ordering
                byte b1 = (byte) (intValue >> 24);
                byte b2 = (byte) (intValue >> 16);
                byte b3 = (byte) (intValue >> 8);
                byte b4 = (byte) intValue;
                ModbusRegister register = new ModbusRegister(b1, b2);
                ModbusRegister register2 = new ModbusRegister(b3, b4);
                return new ModbusRegisterArray(new ModbusRegister[] { register, register2 });
            }
            case INT32_SWAP:
            case UINT32_SWAP: {
                int intValue = numericCommand.intValue();
                // big endian byte ordering
                byte b1 = (byte) (intValue >> 24);
                byte b2 = (byte) (intValue >> 16);
                byte b3 = (byte) (intValue >> 8);
                byte b4 = (byte) intValue;
                ModbusRegister register = new ModbusRegister(b3, b4);
                ModbusRegister register2 = new ModbusRegister(b1, b2);
                return new ModbusRegisterArray(new ModbusRegister[] { register, register2 });
            }
            case FLOAT32: {
                float floatValue = numericCommand.floatValue();
                int intBits = Float.floatToIntBits(floatValue);
                // big endian byte ordering
                byte b1 = (byte) (intBits >> 24);
                byte b2 = (byte) (intBits >> 16);
                byte b3 = (byte) (intBits >> 8);
                byte b4 = (byte) intBits;
                ModbusRegister register = new ModbusRegister(b1, b2);
                ModbusRegister register2 = new ModbusRegister(b3, b4);
                return new ModbusRegisterArray(new ModbusRegister[] { register, register2 });
            }
            case FLOAT32_SWAP: {
                float floatValue = numericCommand.floatValue();
                int intBits = Float.floatToIntBits(floatValue);
                // big endian byte ordering
                byte b1 = (byte) (intBits >> 24);
                byte b2 = (byte) (intBits >> 16);
                byte b3 = (byte) (intBits >> 8);
                byte b4 = (byte) intBits;
                ModbusRegister register = new ModbusRegister(b3, b4);
                ModbusRegister register2 = new ModbusRegister(b1, b2);
                return new ModbusRegisterArray(new ModbusRegister[] { register, register2 });
            }
            case INT64:
            case UINT64: {
                long longValue = numericCommand.longValue();
                // big endian byte ordering
                byte b1 = (byte) (longValue >> 56);
                byte b2 = (byte) (longValue >> 48);
                byte b3 = (byte) (longValue >> 40);
                byte b4 = (byte) (longValue >> 32);
                byte b5 = (byte) (longValue >> 24);
                byte b6 = (byte) (longValue >> 16);
                byte b7 = (byte) (longValue >> 8);
                byte b8 = (byte) longValue;
                return new ModbusRegisterArray(new ModbusRegister[] { new ModbusRegister(b1, b2),
                        new ModbusRegister(b3, b4), new ModbusRegister(b5, b6), new ModbusRegister(b7, b8) });
            }
            case INT64_SWAP:
            case UINT64_SWAP: {
                long longValue = numericCommand.longValue();
                // big endian byte ordering
                byte b1 = (byte) (longValue >> 56);
                byte b2 = (byte) (longValue >> 48);
                byte b3 = (byte) (longValue >> 40);
                byte b4 = (byte) (longValue >> 32);
                byte b5 = (byte) (longValue >> 24);
                byte b6 = (byte) (longValue >> 16);
                byte b7 = (byte) (longValue >> 8);
                byte b8 = (byte) longValue;
                return new ModbusRegisterArray(new ModbusRegister[] { new ModbusRegister(b7, b8),
                        new ModbusRegister(b5, b6), new ModbusRegister(b3, b4), new ModbusRegister(b1, b2) });
            }
            default:
                throw new NotImplementedException(
                        String.format("Illegal type=%s. Missing implementation for this type", type));
        }
    }

    /**
     * Converts command to a boolean
     *
     * true value is represented by {@link OnOffType.ON}, {@link OpenClosedType.OPEN}.
     * false value is represented by {@link OnOffType.OFF}, {@link OpenClosedType.CLOSED}.
     * Furthermore, {@link DecimalType} are converted to boolean true if they are unequal to zero.
     *
     * @param command to convert to boolean
     * @return Boolean value matching the command. Empty if command cannot be converted
     */
    public static Optional<Boolean> translateCommand2Boolean(Command command) {
        if (command.equals(OnOffType.ON)) {
            return Optional.of(Boolean.TRUE);
        }
        if (command.equals(OnOffType.OFF)) {
            return Optional.of(Boolean.FALSE);
        }
        if (command.equals(OpenClosedType.OPEN)) {
            return Optional.of(Boolean.TRUE);
        }
        if (command.equals(OpenClosedType.CLOSED)) {
            return Optional.of(Boolean.FALSE);
        }
        if (command instanceof DecimalType) {
            return Optional.of(!command.equals(DecimalType.ZERO));
        }
        return Optional.empty();
    }
}
