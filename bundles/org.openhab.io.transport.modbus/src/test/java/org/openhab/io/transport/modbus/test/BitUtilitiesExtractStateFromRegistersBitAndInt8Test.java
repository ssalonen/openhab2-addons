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
package org.openhab.io.transport.modbus.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openhab.io.transport.modbus.ModbusBitUtilities;

/**
 * @author Sami Salonen - Initial contribution
 */
public class BitUtilitiesExtractStateFromRegistersBitAndInt8Test {

    @Test
    public void testExtractBitWithRegisterIndexAndBitIndex() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register

        {
            int registerIndex = 0;
            int[] expectedBitsFromLSBtoMSB = new int[] { //
                    1, 0, 1, 0, 0, 1, 0, 0, // lo byte, with increasing significance
                    1, 0, 0, 0, 0, 1, 0, 0 // hi byte, with increasing significance
            };
            for (int bitIndex = 0; bitIndex < expectedBitsFromLSBtoMSB.length; bitIndex++) {
                assertEquals(String.format("bitIndex=%d", bitIndex), expectedBitsFromLSBtoMSB[bitIndex],
                        ModbusBitUtilities.extractBit(bytes, registerIndex, bitIndex));
            }
        }
        {
            int registerIndex = 1;
            int[] expectedBitsFromLSBtoMSB = new int[] { //
                    1, 0, 0, 1, 0, 1, 0, 0, // lo byte, with increasing significance
                    1, 0, 0, 0, 1, 1, 0, 0 // hi byte, with increasing significance
            };
            for (int bitIndex = 0; bitIndex < expectedBitsFromLSBtoMSB.length; bitIndex++) {
                assertEquals(String.format("bitIndex=%d", bitIndex), expectedBitsFromLSBtoMSB[bitIndex],
                        ModbusBitUtilities.extractBit(bytes, registerIndex, bitIndex));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithRegisterIndexAndBitIndexOOB() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, 3, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithRegisterIndexAndBitIndexOOB2() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, 0, 17);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithRegisterIndexAndBitIndexOOB3() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, 0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithRegisterIndexAndBitIndexOOB4() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, -1, 0);
    }

    @Test
    public void testExtractBitWithSingleIndex() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        int[] expectedBits = new int[] { //
                1, 0, 1, 0, 0, 1, 0, 0, // 1st register: lo byte, with increasing significance
                1, 0, 0, 0, 0, 1, 0, 0, // 1st register: hi byte, with increasing significance
                1, 0, 0, 1, 0, 1, 0, 0, // 2nd register: lo byte, with increasing significance
                1, 0, 0, 0, 1, 1, 0, 0 // 2nd register: hi byte, with increasing significance
        };
        for (int bitIndex = 0; bitIndex < expectedBits.length; bitIndex++) {
            assertEquals(String.format("bitIndex=%d", bitIndex), expectedBits[bitIndex],
                    ModbusBitUtilities.extractBit(bytes, bitIndex));
            assertEquals(String.format("bitIndex=%d", bitIndex), expectedBits[bitIndex],
                    ModbusBitUtilities.extractBit(bytes, bitIndex));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithSingleIndexOOB() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, 32);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractBitWithSingleIndexOOB2() {
        byte[] bytes = new byte[] { 0b00100001, // hi byte of 1st register
                0b00100101, // lo byte of 1st register
                0b00110001, // hi byte of 2nd register
                0b00101001 }; // lo byte of 2nd register
        ModbusBitUtilities.extractBit(bytes, -1);
    }

    @Test
    public void extractSInt8WithSingleIndex() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        assertEquals(-1, ModbusBitUtilities.extractSInt8(bytes, 0));
        assertEquals(2, ModbusBitUtilities.extractSInt8(bytes, 1));
        assertEquals(3, ModbusBitUtilities.extractSInt8(bytes, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractSInt8WithSingleIndexOOB() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        ModbusBitUtilities.extractSInt8(bytes, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractSInt8WithSingleIndexOOB2() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        ModbusBitUtilities.extractSInt8(bytes, -1);
    }

    @Test
    public void extractSInt8WithRegisterIndexAndHiByte() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        assertEquals(-1, ModbusBitUtilities.extractSInt8(bytes, 0, true));
        assertEquals(2, ModbusBitUtilities.extractSInt8(bytes, 0, false));
        assertEquals(3, ModbusBitUtilities.extractSInt8(bytes, 1, true));
        assertEquals(4, ModbusBitUtilities.extractSInt8(bytes, 1, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractSInt8WithRegisterIndexAndHiByteOOB() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        ModbusBitUtilities.extractSInt8(bytes, 2, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractSInt8WithRegisterIndexAndHiByteOOB2() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        ModbusBitUtilities.extractSInt8(bytes, -1, true);
    }

    //
    // unsigned int8 follows
    //

    @Test
    public void extractUInt8WithSingleIndex() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        assertEquals(255, ModbusBitUtilities.extractUInt8(bytes, 0));
        assertEquals(2, ModbusBitUtilities.extractUInt8(bytes, 1));
        assertEquals(3, ModbusBitUtilities.extractUInt8(bytes, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractUInt8WithSingleIndexOOB() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        ModbusBitUtilities.extractUInt8(bytes, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractUInt8WithSingleIndexOOB2() {
        byte[] bytes = new byte[] { -1, 2, 3 };
        ModbusBitUtilities.extractUInt8(bytes, -1);
    }

    @Test
    public void extractUInt8WithRegisterIndexAndHiByte() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        assertEquals(255, ModbusBitUtilities.extractUInt8(bytes, 0, true));
        assertEquals(2, ModbusBitUtilities.extractUInt8(bytes, 0, false));
        assertEquals(3, ModbusBitUtilities.extractUInt8(bytes, 1, true));
        assertEquals(4, ModbusBitUtilities.extractUInt8(bytes, 1, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractUInt8WithRegisterIndexAndHiByteOOB() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        ModbusBitUtilities.extractUInt8(bytes, 2, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractUInt8WithRegisterIndexAndHiByteOOB2() {
        byte[] bytes = new byte[] { -1, 2, 3, 4 };
        ModbusBitUtilities.extractUInt8(bytes, 255, true);
    }
}
