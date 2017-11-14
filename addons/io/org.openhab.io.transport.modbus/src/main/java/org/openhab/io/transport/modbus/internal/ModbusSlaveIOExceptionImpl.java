/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.ModbusSlaveIOException;

import net.wimpi.modbus.ModbusIOException;

/**
 * Exception for all IO errors
 *
 * @author Sami Salonen
 *
 */
public class ModbusSlaveIOExceptionImpl extends ModbusSlaveIOException {

    private static final long serialVersionUID = -8910463902857643468L;
    private ModbusIOException error;

    public ModbusSlaveIOExceptionImpl(ModbusIOException e) {
        this.error = e;
    }

    @Override
    public String toString() {
        return String.format("ModbusSlaveIOException(EOF=%s, message='%s')", error.isEOF(), error.getMessage());
    }
}
