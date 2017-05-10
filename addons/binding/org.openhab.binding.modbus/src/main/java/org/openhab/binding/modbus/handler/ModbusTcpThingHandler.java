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
import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.binding.modbus.internal.config.ModbusTcpConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusTcpThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusTcpThingHandler extends AbstractModbusBridgeThing implements ModbusEndpointThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusTcpThingHandler.class);
    private ChannelUID stringChannelUid;
    private ModbusTcpConfiguration config;
    private ModbusSlaveEndpoint endpoint;

    public ModbusTcpThingHandler(Bridge bridge, ModbusManagerReference managerRef) {
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

        this.config = getConfigAs(ModbusTcpConfiguration.class);
        endpoint = new ModbusTCPSlaveEndpoint(config.getHost(), config.getPort());
    }

    @Override
    public ModbusSlaveEndpoint asSlaveEndpoint() {
        return endpoint;
    }

    @Override
    public int getSlaveId() {
        return config.getId();
    }
}
