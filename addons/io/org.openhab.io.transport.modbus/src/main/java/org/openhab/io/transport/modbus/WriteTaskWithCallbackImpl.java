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
import org.openhab.io.transport.modbus.ModbusManager.WriteTaskWithCallback;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Simple implementation for Modbus write requests
 *
 * @author Sami Salonen
 *
 */
public class WriteTaskWithCallbackImpl extends WriteTaskImpl implements WriteTaskWithCallback {

    private static final StandardToStringStyle toStringStyle = new StandardToStringStyle();
    static {
        toStringStyle.setUseShortClassName(true);
    }

    private WeakReference<ModbusWriteCallback> callback;

    public WriteTaskWithCallbackImpl(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
            ModbusWriteCallback callback) {
        super(endpoint, request);
        this.callback = new WeakReference<>(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WeakReference<ModbusWriteCallback> getCallback() {
        return callback;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("request", getRequest())
                .append("endpoint", getEndpoint()).append("callback", getCallback()).toString();
    }
}