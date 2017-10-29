/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import java.util.function.Supplier;

import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusEndpointThingHandler {

    public ModbusSlaveEndpoint asSlaveEndpoint();

    public int getSlaveId();

    public Supplier<ModbusManager> getManagerRef();
}
