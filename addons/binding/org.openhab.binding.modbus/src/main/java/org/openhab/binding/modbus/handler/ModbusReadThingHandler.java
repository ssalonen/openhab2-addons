/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import java.util.stream.Stream;

import org.eclipse.smarthome.core.library.types.DecimalType;
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
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.config.ModbusReadConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ReadCallback;
import org.openhab.io.transport.modbus.RegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusReadThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusReadThingHandler extends BaseThingHandler implements ReadCallback {

    private Logger logger = LoggerFactory.getLogger(ModbusReadThingHandler.class);
    private ChannelUID stringChannelUid;
    private ModbusReadConfiguration config;
    private ModbusSlaveEndpoint endpoint;

    public ModbusReadThingHandler(Thing thing) {
        super(thing);
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
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        config = getConfigAs(ModbusReadConfiguration.class);
        validateConfiguration();
    }

    public synchronized void validateConfiguration() {
        updateStatus(ThingStatus.OFFLINE);
        Bridge readwrite = getBridgeOfThing(getThing());
        if (readwrite == null) {
            logger.debug("ReadThing {} has no ReadWrite bridge. Aborting config validation", getThing());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        if (readwrite.getStatus() != ThingStatus.ONLINE) {
            logger.debug("ReadWrite bridge of ReadThing {} is offline. Aborting config validation", getThing());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        Bridge poller = getBridgeOfThing(readwrite);
        if (poller == null) {
            logger.debug("ReadWrite bridge {} of ReadThing {} has no Poller bridge. Aborting config validation",
                    readwrite, getThing());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        if (poller.getStatus() != ThingStatus.ONLINE) {
            logger.debug("Poller bridge of ReadWrite bridge {} of ReadThing {} is offline. Aborting config validation",
                    getThing());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        ModbusPollerThingHandler handler = (ModbusPollerThingHandler) poller.getHandler();
        PollTask pollTask = handler.getPollTask();
        if (pollTask == null) {
            logger.debug(
                    "Poller of {} ReadWrite bridge {} of ReadThing {} has no active polling. Aborting config validation",
                    handler, readwrite, getThing());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }

        int dataLength = pollTask.getMessage().getDataLength();

        int bitCount;
        String dataElement;
        if (pollTask.getMessage().getFunctionCode() == ModbusReadFunctionCode.READ_INPUT_REGISTERS
                || pollTask.getMessage().getFunctionCode() == ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS) {
            bitCount = ModbusBitUtilities.getBitCount(config.getValueType());
            dataElement = "register";
        } else {
            bitCount = 1;
            if (pollTask.getMessage().getFunctionCode() == ModbusReadFunctionCode.READ_COILS) {
                dataElement = "coil";
            } else {
                dataElement = "discrete input";
            }
        }

        int firstIndex;
        if (bitCount < 16) {
            firstIndex = config.getStart() / bitCount;
        } else {
            firstIndex = config.getStart();
        }
        int registerCount = Math.max(1, bitCount / 16);
        int lastIndex = firstIndex + registerCount - 1;

        if (firstIndex >= dataLength || lastIndex >= dataLength) {
            logger.error(
                    "ReadThing {} with start={} and valueType={} would try to read from {}'th {} to {}'th {} which "
                            + "is out of bounds with poller {} that reads only {} registers",
                    getThing(), config.getStart(), config.getValueType(), firstIndex, dataElement, lastIndex,
                    dataElement, poller, pollTask.getMessage().getDataLength());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            return;
        }

        updateStatus(ThingStatus.ONLINE);
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
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        validateConfiguration();
    }

    @Override
    public void internalUpdateItem(ModbusReadRequestBlueprint request, RegisterArray registers) {
        DecimalType state;
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
            state = ModbusBitUtilities.extractStateFromRegisters(registers, config.getStart(), config.getValueType());
            // config.getTrigger()
            // config.getTransform()

            // CHANNEL_SWITCH
            // CHANNEL_CONTACT
            // CHANNEL_DATETIME
            // CHANNEL_DIMMER
            // CHANNEL_NUMBER
            // CHANNEL_STRING
            // CHANNEL_ROLLERSHUTTER
            //
            Stream<Channel> linkedChannels = getThing().getChannels().stream()
                    .filter(channel -> isLinked(channel.getUID().getId()));
            linkedChannels.forEach(channel -> {

                switch (channel.getAcceptedItemType()) {
                    case ModbusBindingConstants.CHANNEL_SWITCH:
                        break;
                    case ModbusBindingConstants.CHANNEL_CONTACT:
                        break;
                    case ModbusBindingConstants.CHANNEL_DATETIME:
                        // this.updateState(channelId, new DateTimeType(value));
                        break;
                    case ModbusBindingConstants.CHANNEL_DIMMER:
                        // this.updateState(channelId, new DateTimeType(value));
                        break;
                    case ModbusBindingConstants.CHANNEL_NUMBER:
                        // this.updateState(channelId, new DateTimeType(value));
                        break;
                    case ModbusBindingConstants.CHANNEL_STRING:
                        // this.updateState(channelId, new DateTimeType(value));
                        break;
                    case ModbusBindingConstants.CHANNEL_ROLLERSHUTTER:
                        // this.updateState(channelId, new DateTimeType(value));
                        break;
                    default:
                        logger.trace("Type '{}' for channel '{}' not implemented", channel.getAcceptedItemType(),
                                channel.getUID().getId());
                }
            });

            updateState(CHANNEL_STRING, state);
        }

    }

    @Override
    public synchronized void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray coils) {
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
        }
    }

    @Override
    public synchronized void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error) {
        synchronized (this) {
            if (getThing().getStatus() != ThingStatus.ONLINE) {
                return;
            }
        }
    }

}
