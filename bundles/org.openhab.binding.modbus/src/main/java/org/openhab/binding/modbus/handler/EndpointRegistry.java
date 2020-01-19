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
package org.openhab.binding.modbus.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Registry to track endpoints that are in use.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public interface EndpointRegistry {

    /**
     * Register endpoint as being in use
     *
     * @param endpoint
     */
    public void register(ModbusSlaveEndpoint endpoint);

    /**
     * Unregister endpoint being in use
     *
     * @param endpoint
     */
    public void unregister(ModbusSlaveEndpoint endpoint);

}
