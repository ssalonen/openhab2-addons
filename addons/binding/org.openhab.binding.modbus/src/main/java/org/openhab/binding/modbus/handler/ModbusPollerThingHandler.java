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
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.config.ModbusPollerConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ReadCallback;
import org.openhab.io.transport.modbus.RegisterArray;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusPollerThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusPollerThingHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusPollerThingHandler.class);
    private ChannelUID stringChannelUid;
    private ModbusManager manager;

    private volatile Exception lastResponseError;
    private volatile BitArray lastResponseCoils;
    private volatile RegisterArray lastResponseRegisters;
    private ModbusPollerConfiguration config;
    private PollTask pollTask;

    public ModbusPollerThingHandler(Thing thing, ModbusManager manager) {
        super(thing);
        this.manager = manager;
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

    private ModbusSlaveEndpoint getSlaveEndpoint() {
        ModbusEndpointThingHandler slaveEndpoint = getEndpointThingHandler();
        if (slaveEndpoint == null) {
            return null;
        }
        return slaveEndpoint.asSlaveEndpoint();
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        config = getConfigAs(ModbusPollerConfiguration.class);
        initPolling();
    }

    public void initPolling() {
        unregisterPollTask();
        preparePollTask();
        if (pollTask == null) {
            return;
        }
        manager.registerRegularPoll(pollTask, config.getRefresh(), 0);
    }

    public void unregisterPollTask() {
        if (pollTask == null) {
            return;
        }
        manager.unregisterRegularPoll(pollTask);
    }

    private void preparePollTask() {
        ModbusEndpointThingHandler slaveEndpoint = getEndpointThingHandler();
        if (slaveEndpoint == null) {
            logger.debug("No bridge handler -- aborting init for {}", this);
            return;
        }

        int slaveId = slaveEndpoint.getSlaveId();
        ModbusReadFunctionCode functionCode = ModbusBindingConstants.READ_FUNCTION_CODES.get(config.getType());
        if (functionCode == null) {
            logger.error("Illegal function code: {}", config.getType());
            throw new IllegalArgumentException();
        }
        if (config.getRefresh() <= 0L) {
            return;
        }
        ModbusReadRequestBlueprint request = new ModbusReadRequestBlueprint() {

            @Override
            public int getUnitID() {
                return slaveId;
            }

            @Override
            public int getReference() {
                return config.getStart();
            }

            @Override
            public ModbusReadFunctionCode getFunctionCode() {
                return functionCode;
            }

            @Override
            public int getDataLength() {
                return config.getLength();
            }
        };

        pollTask = new PollTask() {

            @Override
            public ModbusReadRequestBlueprint getMessage() {
                return request;
            }

            @Override
            public ModbusSlaveEndpoint getEndpoint() {
                return getSlaveEndpoint();
            }

            @Override
            public ReadCallback getCallback() {
                return new ReadCallback() {

                    @Override
                    public void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error) {
                        lastResponseError = error;
                        lastResponseCoils = null;
                        lastResponseRegisters = null;
                    }

                    @Override
                    public void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray coils) {
                        lastResponseError = null;
                        lastResponseCoils = coils;
                        lastResponseRegisters = null;
                    }

                    @Override
                    public void internalUpdateItem(ModbusReadRequestBlueprint request, RegisterArray registers) {
                        lastResponseError = null;
                        lastResponseCoils = null;
                        lastResponseRegisters = registers;
                    }
                };
            }
        };

    }
}
