/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.config.ModbusWriteConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusReadWriteThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusReadWriteThingHandler extends AbstractModbusBridgeThing implements ModbusReadCallback {

    private Logger logger = LoggerFactory.getLogger(ModbusReadWriteThingHandler.class);
    private volatile ModbusWriteConfiguration config;
    private volatile Set<ChannelUID> channelsToCopyFromRead;
    private volatile Set<ChannelUID> channelsToDelegateWriteCommands;

    public ModbusReadWriteThingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!channelsToDelegateWriteCommands.contains(channelUID)) {
            return;
        }

        forEachChildWriter(handler -> {
            Channel childChannel = handler.getThing().getChannel(channelUID.getId());
            if (childChannel == null) {
                logger.warn("no channel {}", channelUID);
            } else {
                handler.handleCommand(childChannel.getUID(), command);
            }
        });

    }

    @Override
    public synchronized void initialize() {
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);
        config = getConfigAs(ModbusWriteConfiguration.class);
        channelsToCopyFromRead = Stream.of(ModbusBindingConstants.DATA_CHANNELS_TO_COPY_FROM_READ_TO_READWRITE)
                .map(channel -> new ChannelUID(getThing().getUID(), channel)).collect(Collectors.toSet());
        channelsToDelegateWriteCommands = Stream
                .of(ModbusBindingConstants.DATA_CHANNELS_TO_DELEGATE_COMMAND_FROM_READWRITE_TO_WRITE)
                .map(channel -> new ChannelUID(getThing().getUID(), channel)).collect(Collectors.toSet());
        if (getBridge() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge is not set");
        } else if (getBridge().getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge is offline");
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        logger.debug("Read write thing handler got registers: {}", registers);
        updateStatus(ThingStatus.ONLINE);
        forEachChildReader(reader -> {
            reader.onRegisters(request, registers);
            maybeUpdateStateFromReadHandler(reader);
        });
    }

    @Override
    public void onBits(ModbusReadRequestBlueprint request, BitArray bits) {
        logger.debug("Read write thing handler got bits: {}", bits);
        updateStatus(ThingStatus.ONLINE);
        forEachChildReader(reader -> {
            reader.onBits(request, bits);
            maybeUpdateStateFromReadHandler(reader);
        });
    }

    @Override
    public void onError(ModbusReadRequestBlueprint request, Exception error) {
        logger.warn("Read write thing handler got read error: {} {}", error.getClass().getName(), error.getMessage());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, String.format(
                "Read write thing handler got read error: {} {}", error.getClass().getName(), error.getMessage()));
        forEachChildReader(reader -> reader.onError(request, error));
    }

    private void forEachChildReader(Consumer<ModbusReadThingHandler> consumer) {
        List<ModbusReadThingHandler> readers = getReaders();
        // Call each readers callback, and update this bridge's items (matching by channel id)
        readers.stream().forEach(reader -> {
            consumer.accept(reader);
        });
    }

    private void maybeUpdateStateFromReadHandler(ModbusReadThingHandler readHandler) {
        Optional<Map<ChannelUID, State>> optionalLastState = readHandler.getLastState();
        optionalLastState.ifPresent(lastState -> lastState.forEach((childChannelUid, state) -> {
            ChannelUID channelUid = new ChannelUID(getThing().getUID(), childChannelUid.getId());
            if (!channelsToCopyFromRead.contains(channelUid)) {
                return;
            }

            updateState(channelUid, state);
        }));
    }

    private void forEachChildWriter(Consumer<ModbusWriteThingHandler> consumer) {
        List<ModbusWriteThingHandler> writers = getWriters();
        writers.stream().forEach(writer -> {
            consumer.accept(writer);
        });
    }

    /**
     * Get readers by inspecting this bridge's children things
     *
     * @return list of {@link ModbusReadThingHandler}
     */
    private synchronized List<ModbusReadThingHandler> getReaders() {
        return getThing().getThings().stream().map(thing -> thing.getHandler())
                .filter(handler -> handler != null && handler instanceof ModbusReadThingHandler)
                .map(handler -> (ModbusReadThingHandler) handler).collect(Collectors.toList());
    }

    /**
     * Get writers by inspecting this bridge's children things
     *
     * @return list of {@link ModbusWriteThingHandler}
     */
    private synchronized List<ModbusWriteThingHandler> getWriters() {
        return getThing().getThings().stream().map(thing -> thing.getHandler())
                .filter(handler -> handler != null && handler instanceof ModbusWriteThingHandler)
                .map(handler -> (ModbusWriteThingHandler) handler).collect(Collectors.toList());
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        // Re-trigger child thing handlers
        updateStatus(ThingStatus.OFFLINE);
        updateStatus(ThingStatus.ONLINE);
    }

}
