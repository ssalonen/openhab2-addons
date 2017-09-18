/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

import java.lang.ref.WeakReference;

import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.openhab.io.transport.modbus.ModbusManager.WriteTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 *
 * @author Sami Salonen
 *
 */
public class WriteTaskImpl implements WriteTask {

    private static final StandardToStringStyle toStringStyle = new StandardToStringStyle();
    static {
        toStringStyle.setUseShortClassName(true);
    }

    private ModbusSlaveEndpoint endpoint;
    private ModbusWriteRequestBlueprint request;
    private WeakReference<ModbusWriteCallback> callback;

    public WriteTaskImpl(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
            ModbusWriteCallback callback) {
        super();
        this.endpoint = endpoint;
        this.request = request;
        this.callback = new WeakReference<>(callback);
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
    public WeakReference<ModbusWriteCallback> getCallback() {
        return callback;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("request", request).append("endpoint", endpoint)
                .append("callback", getCallback()).toString();
    }
}