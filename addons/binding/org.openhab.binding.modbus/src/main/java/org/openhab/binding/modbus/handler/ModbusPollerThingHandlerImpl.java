/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import java.util.function.Consumer;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.binding.modbus.internal.config.ModbusPollerConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusReadCallback;
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

    private class ReadCallbackDelegator implements ModbusReadCallback {

        private void forEachAllChildCallbacks(Consumer<ModbusReadCallback> callback) {
            getThing().getThings().stream()
                    .filter(thing -> thing.getHandler() != null && thing.getHandler() instanceof ModbusReadCallback)
                    .map(thing -> (ModbusReadCallback) thing.getHandler()).forEach(callback);
        }

        @Override
        public void internalUpdateItem(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
            forEachAllChildCallbacks(callback -> callback.internalUpdateItem(request, registers));
        }

        @Override
        public void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray coils) {
            forEachAllChildCallbacks(callback -> callback.internalUpdateItem(request, coils));
        }

        @Override
        public void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error) {
            forEachAllChildCallbacks(callback -> callback.internalUpdateReadErrorItem(request, error));
        }

    }

    /**
     * Immutable {@link ModbusReadRequestBlueprint} to read from endpoint represented by this Poller's bridge
     *
     * @author Sami Salonen
     *
     */
    private class ModbusReadRequestBlueprintImpl implements ModbusReadRequestBlueprint {

        private int slaveId;
        private ModbusReadFunctionCode functionCode;
        private int start;
        private int length;
        private ModbusEndpointThingHandler endpointThingHandler;

        public ModbusReadRequestBlueprintImpl(ModbusEndpointThingHandler slaveEndpoint) {
            super();
            this.endpointThingHandler = slaveEndpoint;
            this.slaveId = slaveEndpoint.getSlaveId();
            this.functionCode = ModbusBindingConstants.READ_FUNCTION_CODES.get(config.getType());
            if (this.functionCode == null) {
                logger.error("Illegal function code: {}", config.getType());
                throw new IllegalArgumentException();
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

        public ModbusEndpointThingHandler getEndpointThingHandler() {
            return endpointThingHandler;
        }
    }

    private class PollTaskImpl implements PollTask {

        private ModbusReadRequestBlueprintImpl request;

        public PollTaskImpl(ModbusReadRequestBlueprintImpl request) {
            super();
            this.request = request;
        }

        @Override
        public ModbusReadRequestBlueprint getMessage() {
            return request;
        }

        @Override
        public ModbusSlaveEndpoint getEndpoint() {
            return request.getEndpointThingHandler().asSlaveEndpoint();
        }

        @Override
        public ModbusReadCallback getCallback() {
            return callbackDelegator;
        }
    }

    private Logger logger = LoggerFactory.getLogger(ModbusPollerThingHandlerImpl.class);
    private ChannelUID stringChannelUid;

    private volatile Exception lastResponseError;
    private volatile BitArray lastResponseCoils;
    private volatile ModbusRegisterArray lastResponseRegisters;
    private ModbusPollerConfiguration config;
    private PollTask pollTask;
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

    private ModbusEndpointThingHandler getEndpointThingHandler() {
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

    public void initPolling() {
        unregisterPollTask();
        registerPollTask();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        initPolling();
    }

    public void unregisterPollTask() {
        if (pollTask == null) {
            return;
        }
        managerRef.getManager().unregisterRegularPoll(pollTask);
        pollTask = null;
        updateStatus(ThingStatus.OFFLINE);
    }

    private void registerPollTask() {
        if (pollTask != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            throw new IllegalStateException("pollTask should be unregistered before registering a new one!");
        }
        if (config.getRefresh() <= 0L) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Not polling");
            return;
        }

        ModbusEndpointThingHandler slaveEndpoint = getEndpointThingHandler();
        if (slaveEndpoint == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' is offline", getBridge().getLabel()));
            logger.debug("No bridge handler -- aborting init for {}", this);
            return;
        }

        ModbusReadRequestBlueprintImpl request = new ModbusReadRequestBlueprintImpl(slaveEndpoint);
        pollTask = new PollTaskImpl(request);
        managerRef.getManager().registerRegularPoll(pollTask, config.getRefresh(), 0);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public PollTask getPollTask() {
        return pollTask;
    }

}
