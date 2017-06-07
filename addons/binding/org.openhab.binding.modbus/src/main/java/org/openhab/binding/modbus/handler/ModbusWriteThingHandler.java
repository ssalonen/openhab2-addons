/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.internal.Transformation;
import org.openhab.binding.modbus.internal.config.ModbusWriteConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusManager.WriteTask;
import org.openhab.io.transport.modbus.ModbusResponse;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusWriteThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusWriteThingHandler extends BaseThingHandler implements ModbusWriteCallback {

    private static class SingleBitArray implements BitArray {

        private boolean bit;

        public SingleBitArray(boolean bit) {
            this.bit = bit;
        }

        @Override
        public boolean getBit(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return bit;
        }

        @Override
        public int size() {
            return 1;
        }

    }

    private static class ModbusWriteCoilRequestBlueprintImpl implements ModbusWriteCoilRequestBlueprint {
        private int slaveId;
        private int reference;
        private BitArray bits;

        public ModbusWriteCoilRequestBlueprintImpl(int slaveId, int reference, boolean data) {
            super();
            this.slaveId = slaveId;
            this.reference = reference;
            this.bits = new SingleBitArray(data);
            ;
        }

        @Override
        public int getUnitID() {
            return slaveId;
        }

        @Override
        public int getReference() {
            return reference;
        }

        @Override
        public ModbusWriteFunctionCode getFunctionCode() {
            return ModbusWriteFunctionCode.WRITE_COIL;
        }

        @Override
        public BitArray getCoils() {
            return bits;
        }
    }

    private static class WriteTaskImpl implements WriteTask {

        private ModbusSlaveEndpoint endpoint;
        private ModbusWriteRequestBlueprint request;
        private ModbusWriteCallback callback;

        public WriteTaskImpl(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
                ModbusWriteCallback callback) {
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
        public ModbusWriteCallback getCallback() {
            return callback;
        }

    }

    private Logger logger = LoggerFactory.getLogger(ModbusWriteThingHandler.class);
    private volatile ModbusWriteConfiguration config;
    private volatile Transformation transformation;
    private List<Channel> linkedChannels;

    public ModbusWriteThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        boolean boolValue;
        DecimalType numericCommand;
        List<Class<? extends Command>> channelAcceptedCommandTypes;
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
            // numericState = ModbusBitUtilities.extractStateFromRegisters(registers, config.getStart(),
            // config.getValueType());
            // boolValue = !numericState.equals(DecimalType.ZERO);
            channelAcceptedCommandTypes = getCommandTypesUnsynchronized(channelUID);
        }

        Bridge readwriteBridge = getBridge();
        if (readwriteBridge == null) {
            logger.debug("WriteThing '{}' has no readwrite bridge. Aborting writing of command '{}' to channel '{}'",
                    getThing().getLabel(), command, channelUID);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No readwrite bridge");
            return;
        }

        Bridge pollerBridge = getBridgeOfThing(readwriteBridge);
        if (pollerBridge == null) {
            logger.debug(
                    "ReadWriteThing '{}' corresponding to WriteThing '{}' has no poller bridge. Aborting writing of command '{}' to channel '{}'",
                    readwriteBridge.getLabel(), getThing().getLabel(), command, channelUID);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No poller bridge");
            return;
        }
        ModbusPollerThingHandler pollerHandler = (ModbusPollerThingHandler) pollerBridge.getHandler();
        PollTask pollTask = pollerHandler.getPollTask();

        int slaveId = pollTask.getRequest().getUnitID();
        ModbusSlaveEndpoint slaveEndpoint = pollTask.getEndpoint();
        ModbusManager manager = pollerHandler.getManagerRef().getManager();

        String trigger = config.getTrigger();
        if (trigger.equals("*")) {
            // catch all, continue
        } else if (!trigger.equalsIgnoreCase(command.toString())) {
            // no match
            return;
        }

        // Command boolLikeCommand;
        // if (containsOnOff(channelAcceptedCommandTypes)) {
        // boolLikeCommand = boolValue ? OnOffType.ON : OnOffType.OFF;
        // } else if (containsOpenClosed(channelAcceptedCommandTypes)) {
        // boolLikeCommand = boolValue ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
        // } else {
        // boolLikeCommand = null;
        // }

        // Command transformedCommand;
        // if (transformation == null || transformation.isIdentityTransform()) {
        // if (boolLikeCommand != null) {
        // transformedCommand = boolLikeCommand;
        // } else {
        // transformedCommand = numericCommand;
        // }
        // } else {
        // transformedCommand = transformation.transformCommand(bundleContext, channelAcceptedCommandTypes,
        // numericCommand);
        // }

        //
        //
        // Think this through
        //
        //

        if (transformedCommand == null) {
            // transformation failed, return
            return;
        }

        if (config.getType().equals(WRITE_TYPE_COIL)) {
            int reference = pollTask.getRequest().getReference() + config.getStart();
            boolean data = true;
            ModbusWriteCoilRequestBlueprintImpl request = new ModbusWriteCoilRequestBlueprintImpl(slaveId, reference,
                    data);
            WriteTaskImpl writeTask = new WriteTaskImpl(slaveEndpoint, request, this);
            manager.submitOneTimeWrite(writeTask);
        } else if (config.getType().equals(WRITE_TYPE_HOLDING)) {

        } else {
            // should not happen
            throw new NotImplementedException();
        }

        // new ModbusW
        if (channelUID.getId().equals(CHANNEL_SWITCH)) {
            // manager.submitOneTimeWrite(task)
        }
    }

    @Override
    public synchronized void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        linkedChannels = getThing().getChannels().stream().filter(channel -> isLinked(channel.getUID().getId()))
                .collect(Collectors.toList());
        config = getConfigAs(ModbusWriteConfiguration.class);
        transformation = new Transformation(config.getTransform());
        validateConfiguration();
    }

    public void validateConfiguration() {
        updateStatus(ThingStatus.INITIALIZING);
        Bridge readwrite = getBridgeOfThing(getThing());
        if (readwrite == null) {
            logger.debug("WriteThing '{}' has no ReadThing bridge. Aborting config validation", getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No read-write bridge");
            return;
        }
        if (readwrite.getStatus() != ThingStatus.ONLINE) {
            logger.debug("ReadWrite bridge '{}' of WriteThing '{}' is offline. Aborting config validation",
                    readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Read-write bridge %s is offline", readwrite.getLabel()));
            return;
        }

        Bridge poller = getBridgeOfThing(readwrite);
        if (poller == null) {
            logger.debug("ReadWrite bridge '{}' of WriteThing '{}' has no Poller bridge. Aborting config validation",
                    readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("No poller bridge set for the read-write bridge %s", readwrite.getLabel()));
            return;
        }
        if (poller.getStatus() != ThingStatus.ONLINE) {
            logger.debug(
                    "Poller bridge '{}' of ReadWriteThing bridge '{}' of WriteThing '{}' is offline. Aborting config validation",
                    poller.getLabel(), readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Poller bridge %s of the read-write bridge is offline", poller.getLabel()));
            return;
        }

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        validateConfiguration();
    }

    @Override
    public void onError(ModbusWriteRequestBlueprint request, Exception error) {
        logger.error("Unsuccessful write: {} {}", error.getClass().getName(), error.getMessage());
    }

    @Override
    public void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response) {
        // The binding does not respond in any way to sucessful writes except logging
        logger.debug("Successful write, matching request {}", request);
    }

    private boolean containsOnOff(List<Class<? extends Command>> channelAcceptedDataTypes) {
        return channelAcceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OnOffType.class);
        });
    }

    private boolean containsOpenClosed(List<Class<? extends Command>> acceptedDataTypes) {
        return acceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OpenClosedType.class);
        });
    }

    private Bridge getBridgeOfThing(Thing thing) {
        ThingUID bridgeUID = thing.getBridgeUID();
        synchronized (this) {
            if (bridgeUID != null && thingRegistry != null) {
                return (Bridge) thingRegistry.get(bridgeUID);
            } else {
                return null;
            }
        }
    }

    private List<Class<? extends Command>> getCommandTypesUnsynchronized(ChannelUID channelUID) {
        if (!linkedChannels.contains(channelUID)) {
            return Collections.emptyList();
        }

        Optional<Item> item = linkRegistry.getLinkedItems(channelUID).stream().findFirst();
        if (!item.isPresent()) {
        }

        List<Class<? extends Command>> acceptedCommandTypes = item.get().getAcceptedCommandTypes();
        return acceptedCommandTypes;
    }
}
