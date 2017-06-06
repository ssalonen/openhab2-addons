/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
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
    private ChannelUID stringChannelUid;
    private ModbusSlaveEndpoint endpoint;

    public ModbusReadWriteThingHandler(Bridge bridge) {
        super(bridge);
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
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void internalUpdateItem(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got registers: {}", registers);
        propagateToChildren(reader -> reader.internalUpdateItem(request, registers));
    }

    @Override
    public void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray bits) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got bits: {}", bits);
        propagateToChildren(reader -> reader.internalUpdateItem(request, bits));
    }

    @Override
    public void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got error: {} {}", error.getClass().getName(), error.getMessage(), error);
        propagateToChildren(reader -> reader.internalUpdateReadErrorItem(request, error));
    }

    private void propagateToChildren(Consumer<ModbusReadThingHandler> consumer) {
        List<ModbusReadThingHandler> readers = getReaders();
        // Call each readers callback, and update this bridge's items (matching by channel id)
        readers.stream().forEach(reader -> {
            consumer.accept(reader);
            Optional<Map<ChannelUID, State>> optionalLastState = reader.getLastState();
            optionalLastState.ifPresent(lastState -> lastState.forEach((uid, state) -> {
                updateState(uid.getId(), state);
            }));
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

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        // Re-trigger child thing handlers
        updateStatus(ThingStatus.OFFLINE);
        updateStatus(ThingStatus.ONLINE);
    }

}
