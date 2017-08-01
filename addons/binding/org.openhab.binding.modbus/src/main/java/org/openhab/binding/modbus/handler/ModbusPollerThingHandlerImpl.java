/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.config.ModbusPollerConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprintImpl;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTaskImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusPollerThingHandlerImpl} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusPollerThingHandlerImpl extends AbstractModbusBridgeThing implements ModbusPollerThingHandler {

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
     * @author Sami Salonen
     *
     */
    private static class ModbusPollerReadRequest extends ModbusReadRequestBlueprintImpl {

        private static ModbusReadFunctionCode getFunctionCode(String type) {
            ModbusReadFunctionCode functionCode = ModbusBindingConstants.READ_FUNCTION_CODES.get(type);
            if (functionCode == null) {
                throw new IllegalArgumentException("No function code found for " + type);
            }
            return functionCode;
        }

        public ModbusPollerReadRequest(ModbusPollerConfiguration config,
                ModbusEndpointThingHandler slaveEndpointThingHandler) {
            super(slaveEndpointThingHandler.getSlaveId(), getFunctionCode(config.getType()), config.getStart(),
                    config.getLength());
        }
    }

    private Logger logger = LoggerFactory.getLogger(ModbusPollerThingHandlerImpl.class);

    private ModbusPollerConfiguration config;
    private volatile PollTask pollTask;
    private Supplier<ModbusManager> managerRef;

    private ModbusReadCallback callbackDelegator = new ReadCallbackDelegator();

    public ModbusPollerThingHandlerImpl(Bridge bridge, Supplier<ModbusManager> managerRef) {
        super(bridge);
        this.managerRef = managerRef;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public ModbusEndpointThingHandler getEndpointThingHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Bridge is null");
            return null;
        }
        if (bridge.getStatus() == ThingStatus.OFFLINE) {
            logger.debug("Bridge is offline");
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
        if (config.getRefresh() > 0L) {
            managerRef.get().unregisterRegularPoll(pollTask);
        }
        pollTask = null;
        updateStatus(ThingStatus.OFFLINE);
    }

    private synchronized void registerPollTask() {
        if (pollTask != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            throw new IllegalStateException("pollTask should be unregistered before registering a new one!");
        }

        ModbusEndpointThingHandler slaveEndpointThingHandler = getEndpointThingHandler();
        if (slaveEndpointThingHandler == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Bridge '%s' is offline", Optional.ofNullable(getBridge()).map(b -> b.getLabel())));
            logger.debug("No bridge handler -- aborting init for {}", this);
            return;
        }

        ModbusReadRequestBlueprintImpl request = new ModbusPollerReadRequest(config, slaveEndpointThingHandler);
        pollTask = new PollTaskImpl(slaveEndpointThingHandler.asSlaveEndpoint(), request, callbackDelegator);

        if (config.getRefresh() <= 0L) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Not polling");
        } else {
            managerRef.get().registerRegularPoll(pollTask, config.getRefresh(), 0);
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public Supplier<ModbusManager> getManagerRef() {
        return managerRef;
    }

    @Override
    public PollTask getPollTask() {
        return pollTask;
    }

}
