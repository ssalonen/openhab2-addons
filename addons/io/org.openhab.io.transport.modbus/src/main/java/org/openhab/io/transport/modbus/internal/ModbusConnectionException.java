
/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * @author Sami Salonen
 */
package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

@SuppressWarnings("serial")
public class ModbusConnectionException extends Exception {

    private ModbusSlaveEndpoint endpoint;

    public ModbusConnectionException(ModbusSlaveEndpoint endpoint) {
        this.endpoint = endpoint;

    }

    public ModbusSlaveEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String getMessage() {
        return String.format("Error connecting to endpoint %s", endpoint);
    }

}
