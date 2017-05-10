/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ReadCallback;
import org.openhab.io.transport.modbus.RegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusReadWriteThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusReadWriteThingHandler extends AbstractModbusBridgeThing implements ReadCallback {

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
    public void internalUpdateItem(ModbusReadRequestBlueprint request, RegisterArray registers) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got registers: {}", registers);
        // 1. update readers
        // 2. update channels based on readers
    }

    @Override
    public void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray coils) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got coils: {}", coils);
        // 1. update readers
        // 2. update channels based on readers
    }

    @Override
    public void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error) {
        // TODO Auto-generated method stub
        logger.info("Read write thing handler got error: {} {}", error.getClass().getName(), error.getMessage(), error);
        // 1. update readers
        // 2. update channels based on readers
    }

}
