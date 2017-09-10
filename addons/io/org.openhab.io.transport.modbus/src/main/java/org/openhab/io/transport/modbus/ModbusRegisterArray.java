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
 * ModbusRegisterArray interface.
 * </p>
 *
 * @author Sami Salonen
 */
public interface ModbusRegisterArray {
    /**
     * Return register at the given index
     *
     * Index 0 matches first register (lowest register index).
     * <p>
     *
     * @param index the index of the register to be returned.
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    ModbusRegister getRegister(int index);

    /**
     * Get number of registers stored in this instance
     *
     * @return
     */
    int size();
}
