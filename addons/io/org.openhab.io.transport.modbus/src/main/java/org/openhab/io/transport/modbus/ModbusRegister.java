/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

/**
 * <p>
 * ModbusRegister interface.
 * </p>
 *
 * @author Sami Salonen
 */
public interface ModbusRegister {
    public byte[] getBytes();

    /**
     * Returns the value of this <tt>InputRegister</tt>.
     * The value is stored as <tt>int</tt> but should be
     * treated like a 16-bit word.
     *
     * @return the value as <tt>int</tt>.
     */
    public int getValue();

    /**
     * Returns the content of this <tt>Register</tt> as
     * unsigned 16-bit value (unsigned short).
     *
     * @return the content as unsigned short (<tt>int</tt>).
     */
    public int toUnsignedShort();
}
