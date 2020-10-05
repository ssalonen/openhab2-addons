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
package org.openhab.binding.modbus.e3dc.internal.dto;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.transport.modbus.ModbusBitUtilities;

/**
 * The {@link DataConverter} Helper class to convert bytes from modbus into desired data format
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class DataConverter {
    private static final long MAX_INT32 = (long) Math.pow(2, Integer.SIZE);

    /**
     * Get double value from 2 bytes with correction factor
     *
     * @param wrap
     * @return double
     */
    public static double getUDoubleValue(ModbusBitUtilities.ValueReader wrap, double factor) {
        return round(wrap.getUInt16() * factor, 2);
    }

    /**
     * Conversion done according to E3DC Modbus Specification V1.7
     *
     * @param wrap
     * @return decoded long value, Long.MIN_VALUE otherwise
     */
    public static long getE3DCInt32Swap(ModbusBitUtilities.ValueReader wrap) {
        long a = wrap.getUInt16();
        long b = wrap.getUInt16();
        if (b < 32768) {
            return b * 65536 + a;
        } else {
            return (MAX_INT32 - b * 65536 - a) * -1;
        }
    }

    public static String getString(byte[] bArray) {
        return ModbusBitUtilities.extractStringFromBytes(bArray, 0, bArray.length, StandardCharsets.US_ASCII).trim();
    }

    public static int toInt(BitSet bitSet) {
        int intValue = 0;
        for (int bit = 0; bit < bitSet.length(); bit++) {
            if (bitSet.get(bit)) {
                intValue |= (1 << bit);
            }
        }
        return intValue;
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
