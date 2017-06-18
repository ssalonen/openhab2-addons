/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DateTimeType;
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
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.Transformation;
import org.openhab.binding.modbus.internal.config.ModbusReadConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusReadThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusReadThingHandler extends BaseThingHandler implements ModbusReadCallback {

    private Logger logger = LoggerFactory.getLogger(ModbusReadThingHandler.class);
    private volatile ModbusReadConfiguration config;
    private volatile Object lastStateLock = new Object();
    private volatile Map<ChannelUID, State> lastState;
    private volatile Transformation transformation;
    private volatile String trigger;
    private volatile List<Channel> linkedChannels;

    public ModbusReadThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // All channels are read-only for now
        // TODO: handle REFRESH
    }

    @Override
    public synchronized void initialize() {
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);
        synchronized (lastStateLock) {
            lastState = null;
        }
        config = getConfigAs(ModbusReadConfiguration.class);
        trigger = config.getTrigger();
        transformation = new Transformation(config.getTransform());

        linkedChannels = getThing().getChannels().stream().filter(channel -> isLinked(channel.getUID().getId()))
                .collect(Collectors.toList());

        validateConfiguration();
    }

    public synchronized void validateConfiguration() {
        updateStatus(ThingStatus.INITIALIZING);
        Bridge readwrite = getBridgeOfThing(getThing());
        if (readwrite == null) {
            logger.debug("ReadThing '{}' has no ReadThing bridge. Aborting config validation", getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No read-write bridge");
            return;
        }
        if (readwrite.getStatus() != ThingStatus.ONLINE) {
            logger.debug("ReadWrite bridge '{}' of ReadThing '{}' is offline. Aborting config validation",
                    readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Read-write bridge %s is offline", readwrite.getLabel()));
            return;
        }

        Bridge poller = getBridgeOfThing(readwrite);
        if (poller == null) {
            logger.debug("ReadWrite bridge '{}' of ReadThing '{}' has no Poller bridge. Aborting config validation",
                    readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("No poller bridge set for the read-write bridge %s", readwrite.getLabel()));
            return;
        }
        if (poller.getStatus() != ThingStatus.ONLINE) {
            logger.debug(
                    "Poller bridge '{}' of ReadWriteThing bridge '{}' of ReadThing '{}' is offline. Aborting config validation",
                    poller.getLabel(), readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Poller bridge %s of the read-write bridge is offline", poller.getLabel()));
            return;
        }

        ModbusPollerThingHandler handler = (ModbusPollerThingHandler) poller.getHandler();
        PollTask pollTask = handler.getPollTask();
        if (pollTask == null) {
            logger.debug(
                    "Poller '{}' of ReadWrite bridge '{}' of ReadThing '{}' has no active polling. Aborting config validation",
                    poller.getLabel(), readwrite.getLabel(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    String.format("Poller %s is configured not to poll", poller.getLabel()));
            return;
        }

        if (validateIndex(pollTask)) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    private boolean validateIndex(PollTask pollTask) {
        // bits represented by the value type, e.g. int32 -> 32
        int valueTypeBitCount;
        // bits represented by the function code. For registers this is 16, for coils and discrete inputs it is 1.
        int functionObjectBitSize;
        // textual name for the data element, e.g. register
        // (for logging)
        String dataElement;
        if (pollTask.getRequest().getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_REGISTERS
                || pollTask.getRequest().getFunctionCode() == ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS) {
            valueTypeBitCount = ModbusBitUtilities.getBitCount(config.getValueType());
            functionObjectBitSize = 16;
            dataElement = "register";
        } else {
            valueTypeBitCount = 1;
            functionObjectBitSize = 1;
            if (pollTask.getRequest().getFunctionCode() == ModbusReadFunctionCode.READ_COILS) {
                dataElement = "coil";
            } else {
                dataElement = "discrete input";
            }
        }

        // First index of polled items (e.g. registers or coils) that is needed to read the object. For example, the
        // index of the register that corresponds to the first 16bits of float32 object.
        int firstObjectIndex;
        if (valueTypeBitCount < 16) {
            firstObjectIndex = (config.getStart() * valueTypeBitCount) / functionObjectBitSize;
        } else {
            firstObjectIndex = config.getStart();
        }
        // Convert object size to polled items. E.g. float32 -> 2 (registers)
        int objectSizeInPolledItemCount = Math.max(1, valueTypeBitCount / functionObjectBitSize);
        int lastObjectIndex = firstObjectIndex + objectSizeInPolledItemCount - 1;
        int pollObjectCount = pollTask.getRequest().getDataLength();

        if (firstObjectIndex >= pollObjectCount || lastObjectIndex >= pollObjectCount) {
            String errmsg = String.format(
                    "Out-of-bounds: tried to read %s elements with index %d to %d (zero based index). Poller reads only %d %s elements which means that maximum index (zero-indexed) is %d",
                    dataElement, firstObjectIndex, lastObjectIndex, pollObjectCount, dataElement, pollObjectCount);
            logger.error("ReadThing {} is out of bounds: {}", getThing(), errmsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
            return false;
        }
        return true;
    }

    /**
     * Implementation copied from BaseThingHandler
     *
     * @param thing
     * @return
     */
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

    @Override
    public synchronized void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        validateConfiguration();
    }

    private boolean containsOnOff(List<Class<? extends State>> channelAcceptedDataTypes) {
        return channelAcceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OnOffType.class);
        });
    }

    private boolean containsOpenClosed(List<Class<? extends State>> acceptedDataTypes) {
        return acceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OpenClosedType.class);
        });
    }

    @Override
    public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        boolean boolValue;
        DecimalType numericState;
        Map<ChannelUID, List<Class<? extends State>>> channelAcceptedDataTypes;
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
            numericState = ModbusBitUtilities.extractStateFromRegisters(registers, config.getStart(),
                    config.getValueType());
            boolValue = !numericState.equals(DecimalType.ZERO);
            channelAcceptedDataTypes = getLinkedChannelDataTypesUnsynchronized();
        }
        Map<ChannelUID, State> state = processUpdatedValue(numericState, transformation, linkedChannels, trigger,
                channelAcceptedDataTypes, boolValue);
        synchronized (lastStateLock) {
            lastState = state;
        }

        updateState(ModbusBindingConstants.CHANNEL_LAST_SUCCESS, new DateTimeType());
    }

    @Override
    public synchronized void onBits(ModbusReadRequestBlueprint request, BitArray bits) {
        boolean boolValue;
        DecimalType numericState;
        Map<ChannelUID, List<Class<? extends State>>> channelAcceptedDataTypes;
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
            boolValue = bits.getBit(config.getStart());
            numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;
            channelAcceptedDataTypes = getLinkedChannelDataTypesUnsynchronized();
        }

        Map<ChannelUID, State> state = processUpdatedValue(numericState, transformation, linkedChannels, trigger,
                channelAcceptedDataTypes, boolValue);
        synchronized (lastStateLock) {
            lastState = state;
        }

        updateState(ModbusBindingConstants.CHANNEL_LAST_SUCCESS, new DateTimeType());
    }

    @Override
    public synchronized void onError(ModbusReadRequestBlueprint request, Exception error) {
        logger.error("Thing {} received read error: {} {}", getThing(), error.getClass().getName(), error.getMessage(),
                error);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                String.format("%s: %s", error.getClass().getName(), error.getMessage()));
        updateState(ModbusBindingConstants.CHANNEL_LAST_ERROR, new DateTimeType());
    }

    public Optional<Map<ChannelUID, State>> getLastState() {
        return Optional.ofNullable(lastState);
    }

    private Map<ChannelUID, State> processUpdatedValue(DecimalType numericState, Transformation transformation,
            List<Channel> linkedChannels, String trigger,
            Map<ChannelUID, List<Class<? extends State>>> channelAcceptedDataTypes, boolean boolValue) {
        Map<ChannelUID, State> states = new HashMap<>();
        linkedChannels.forEach(channel -> {
            ChannelUID channelUID = channel.getUID();
            List<Class<? extends State>> acceptedDataTypes = channelAcceptedDataTypes.get(channelUID);
            if (acceptedDataTypes.isEmpty()) {
                // Channel is not linked -- skip
                return;
            }

            State boolLikeState;
            if (containsOnOff(acceptedDataTypes)) {
                boolLikeState = boolValue ? OnOffType.ON : OnOffType.OFF;
            } else if (containsOpenClosed(acceptedDataTypes)) {
                boolLikeState = boolValue ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            } else {
                boolLikeState = null;
            }

            if (trigger.equals("*")) {
                // catch all
            } else if (trigger.equalsIgnoreCase(numericState.toString())) {
                // matches numeric state
            } else if (boolLikeState != null && trigger.equalsIgnoreCase(boolLikeState.toString())) {
                // Channel is bound to OnOff or OpenClosed type of item, and the trigger matches ON/OFF, OPEN/CLOSED
            } else {
                // no match, continue to next channel
                return;
            }

            State transformedState;
            if (transformation == null || transformation.isIdentityTransform()) {
                if (boolLikeState != null) {
                    transformedState = boolLikeState;
                } else {
                    transformedState = numericState;
                }
            } else {
                transformedState = transformation.transformState(bundleContext, acceptedDataTypes, numericState);
            }

            if (transformedState != null) {
                states.put(channel.getUID(), transformedState);
            }
        });

        // Update channels
        states.forEach((uid, state) -> updateState(uid, state));
        return states;
    }

    private Map<ChannelUID, List<Class<? extends State>>> getLinkedChannelDataTypesUnsynchronized() {
        return linkedChannels.stream().collect(Collectors.toMap(channel -> channel.getUID(), channel -> {
            Optional<Item> item = linkRegistry.getLinkedItems(channel.getUID()).stream().findFirst();
            if (!item.isPresent()) {
                return Collections.emptyList();
            }

            List<Class<? extends State>> acceptedDataTypes = item.get().getAcceptedDataTypes();
            return acceptedDataTypes;
        }));
    }

}
