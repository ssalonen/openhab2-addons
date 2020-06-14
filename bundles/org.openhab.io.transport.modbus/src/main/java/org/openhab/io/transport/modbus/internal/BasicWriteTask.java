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
package org.openhab.io.transport.modbus.internal;

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.WriteTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Simple implementation for Modbus write requests
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class BasicWriteTask implements WriteTask {

    private static final StandardToStringStyle TO_STRING_STYLE = new StandardToStringStyle();
    static {
        TO_STRING_STYLE.setUseShortClassName(true);
    }

    private ModbusSlaveEndpoint endpoint;
    private ModbusWriteRequestBlueprint request;
    private @Nullable ModbusWriteCallback callback;

    public BasicWriteTask(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
            @Nullable ModbusWriteCallback callback) {
        super();
        this.endpoint = endpoint;
        this.request = request;
        this.callback = callback;
    }

    @Override
    public ModbusSlaveEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public ModbusWriteRequestBlueprint getRequest() {
        return request;
    }

    @Override
    public @Nullable ModbusWriteCallback getCallback() {
        return callback;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, TO_STRING_STYLE).append("request", request).append("endpoint", endpoint)
                .append("callback", callback).toString();
    }
}
