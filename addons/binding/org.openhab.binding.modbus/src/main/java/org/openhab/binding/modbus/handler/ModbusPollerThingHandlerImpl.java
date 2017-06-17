/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.StandardToStringStyle;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.binding.modbus.internal.config.ModbusPollerConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusPollerThingHandlerImpl} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusPollerThingHandlerImpl extends AbstractModbusBridgeThing implements ModbusPollerThingHandler {
    private static StandardToStringStyle toStringStyle = new StandardToStringStyle();
    static {
        toStringStyle.setUseShortClassName(true);
    }

    /**
     * {@link ModbusReadCallback} that delegates all tasks forward.
     *
     * All instances of {@linkplain ReadCallbackDelegator} are considered equal, if they are connected to the same
     * bridge. This makes sense, as the callback delegates
     * to all child things of this bridge.
     *
     * @author Sami Salonen
     *
     */
    private class ReadCallbackDelegator implements ModbusReadCallback {

        private void forEachAllChildCallbacks(Consumer<ModbusReadCallback> callback) {
            getThing().getThings().stream()
                    .filter(thing -> thing.getHandler() != null && thing.getHandler() instanceof ModbusReadCallback)
                    .map(thing -> (ModbusReadCallback) thing.getHandler()).forEach(callback);
        }

        @Override
        public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
            forEachAllChildCallbacks(callback -> callback.onRegisters(request, registers));
        }

        @Override
        public void onBits(ModbusReadRequestBlueprint request, BitArray coils) {
            forEachAllChildCallbacks(callback -> callback.onBits(request, coils));
        }

        @Override
        public void onError(ModbusReadRequestBlueprint request, Exception error) {
            forEachAllChildCallbacks(callback -> callback.onError(request, error));
        }

        private ThingUID getThingUID() {
            return getThing().getUID();
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
            ReadCallbackDelegator rhs = (ReadCallbackDelegator) obj;
            return getThingUID().equals(rhs.getThingUID());
        }

        @Override
        public int hashCode() {
            return getThingUID().hashCode();
        }
    }

    /**
     * Immutable {@link ModbusReadRequestBlueprint} to read from endpoint represented by this Poller's bridge
     *
     * Equals and hashCode implemented for PollTask. Two instances of this class are considered the same if they have
     * the equal parameters (same slave id, start, length and function).
     *
     * @author Sami Salonen
     *
     */
    private class ModbusReadRequestBlueprintImpl implements ModbusReadRequestBlueprint {

        private int slaveId;
        private ModbusReadFunctionCode functionCode;
        private int start;
        private int length;

        public ModbusReadRequestBlueprintImpl(ModbusEndpointThingHandler slaveEndpointThingHandler) {
            super();
            this.slaveId = slaveEndpointThingHandler.getSlaveId();
            this.functionCode = ModbusBindingConstants.READ_FUNCTION_CODES.get(config.getType());
            if (this.functionCode == null) {
                logger.error("Illegal function code: {}", config.getType());
                throw new IllegalArgumentException("No function code found for " + config.getType());
            }
            start = config.getStart();
            length = config.getLength();
        }

        @Override
        public int getUnitID() {
            return slaveId;
        }

        @Override
        public int getReference() {
            return start;
        }

        @Override
        public ModbusReadFunctionCode getFunctionCode() {
            return functionCode;
        }

        @Override
        public int getDataLength() {
            return length;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(89, 3).append(slaveId).append(functionCode).append(start).append(length)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, toStringStyle).append("slaveId", slaveId)
                    .append("functionCode", functionCode).append("start", start).append("length", length).toString();
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
            ModbusReadRequestBlueprintImpl rhs = (ModbusReadRequestBlueprintImpl) obj;
            return new EqualsBuilder().append(slaveId, rhs.slaveId).append(functionCode, rhs.functionCode)
                    .append(start, rhs.start).append(length, rhs.length).isEquals();
        }

    }

    /**
     * HashCode and equals should be defined such that two poll tasks considered the same only if their request,
     * endpoint and callback are the same. This allows two differentiate poll tasks with different callbacks.
     *
     * @author Sami Salonen
     *
     */
    private class PollTaskImpl implements PollTask {

        private ModbusReadRequestBlueprintImpl request;
        private ModbusSlaveEndpoint endpoint;

        public PollTaskImpl(ModbusSlaveEndpoint endpoint, ModbusReadRequestBlueprintImpl request) {
            super();
            this.endpoint = endpoint;
            this.request = request;
        }

        @Override
        public ModbusReadRequestBlueprint getRequest() {
            return request;
        }

        @Override
        public ModbusSlaveEndpoint getEndpoint() {
            return endpoint;
        }

        @Override
        public ModbusReadCallback getCallback() {
            return callbackDelegator;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(71, 5).append(request).append(getEndpoint()).append(getCallback()).toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, toStringStyle).append("request", request).append("endpoint", endpoint)
                    .append("callback", getCallback()).toString();
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
            PollTaskImpl rhs = (PollTaskImpl) obj;
            return new EqualsBuilder().append(request, rhs.request).append(endpoint, rhs.endpoint)
                    .append(getCallback(), rhs.getCallback()).isEquals();
        }

    }

    private Logger logger = LoggerFactory.getLogger(ModbusPollerThingHandlerImpl.class);
    private ChannelUID stringChannelUid;

    private volatile Exception lastResponseError;
    private volatile BitArray lastResponseCoils;
    private volatile ModbusRegisterArray lastResponseRegisters;
    private ModbusPollerConfiguration config;
    private volatile PollTask pollTask;
    private ModbusManagerReference managerRef;

    private ModbusReadCallback callbackDelegator = new ReadCallbackDelegator();

    public ModbusPollerThingHandlerImpl(Bridge bridge, ModbusManagerReference managerRef) {
        super(bridge);
        this.managerRef = managerRef;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_STRING)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public ModbusEndpointThingHandler getEndpointThingHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Bridge is null");
            return null;
        }
        ThingHandler handler = bridge.getHandler();
        if (handler == null) {
            logger.debug("Bridge handler is null");
            return null;
        }

        if (handler instanceof ModbusEndpointThingHandler) {
            ModbusEndpointThingHandler slaveEndpoint = (ModbusEndpointThingHandler) handler;
            return slaveEndpoint;
        } else {
            logger.error("Unexpected bridge handler: {}", handler);
            throw new IllegalStateException();
        }
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        config = getConfigAs(ModbusPollerConfiguration.class);
        initPolling();
    }

    @Override
    public void dispose() {
        unregisterPollTask();
    }

    public void initPolling() {
        unregisterPollTask();
        registerPollTask();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        initPolling();
    }

    public synchronized void unregisterPollTask() {
        if (pollTask == null) {
            return;
        }
        managerRef.getManager().unregisterRegularPoll(pollTask);
        pollTask = null;
        updateStatus(ThingStatus.OFFLINE);
    }

    private synchronized void registerPollTask() {
        if (pollTask != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            throw new IllegalStateException("pollTask should be unregistered before registering a new one!");
        }
        if (config.getRefresh() <= 0L) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Not polling");
            return;
        }

        ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        if (slaveEndpointThingHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' is offline", Optional.ofNullable(getBridge()).map(b -> b.getLabel())));
            logger.debug("No bridge handler -- aborting init for {}", this);
            return;
        }

        ModbusReadRequestBlueprintImpl request = new ModbusReadRequestBlueprintImpl(slaveEndpointThingHandler);
        pollTask = new PollTaskImpl(slaveEndpointThingHandler.asSlaveEndpoint(), request);
        managerRef.getManager().registerRegularPoll(pollTask, config.getRefresh(), 0);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public ModbusManagerReference getManagerRef() {
        return managerRef;
    }

    @Override
    public PollTask getPollTask() {
        return pollTask;
    }

}
