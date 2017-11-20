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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.openhab.io.transport.modbus.ModbusManager.PollTaskWithCallback;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * Implementation of {@link PollTaskWithCallback} that differentiates tasks using endpoint, request and callbacks.
 *
 * Note: Two differentiate poll tasks are considered unequal if their callbacks are unequal.
 *
 * HashCode and equals should be defined such that two poll tasks considered the same only if their request,
 * maxTries, endpoint and callback are the same.
 *
 * @author Sami Salonen
 *
 */
public class PollTaskWithCallbackImpl extends PollTaskImpl implements PollTaskWithCallback {

    static StandardToStringStyle toStringStyle = new StandardToStringStyle();
    static {
        toStringStyle.setUseShortClassName(true);
    }

    private WeakReference<ModbusReadCallback> callback;

    public PollTaskWithCallbackImpl(ModbusSlaveEndpoint endpoint, ModbusReadRequestBlueprintImpl request,
            ModbusReadCallback callback) {
        super(endpoint, request);
        this.callback = new WeakReference<>(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WeakReference<ModbusReadCallback> getCallback() {
        return callback;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(9, 3).append(getRequest()).append(getEndpoint()).append(getCallback()).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, toStringStyle).append("request", getRequest())
                .append("endpoint", getEndpoint()).append("callback", getCallback()).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PollTaskWithCallbackImpl rhs = (PollTaskWithCallbackImpl) obj;
        return new EqualsBuilder().append(getRequest(), rhs.getRequest()).append(getEndpoint(), rhs.getEndpoint())
                .append(getCallback(), rhs.getCallback()).isEquals();
    }

}