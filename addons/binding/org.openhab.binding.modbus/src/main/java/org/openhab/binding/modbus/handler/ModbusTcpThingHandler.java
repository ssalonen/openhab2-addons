/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import java.util.function.Supplier;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.modbus.internal.config.ModbusTcpConfiguration;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
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
public class ModbusTcpThingHandler extends AbstractModbusBridgeThing
        implements ModbusEndpointThingHandler, ModbusManagerListener {

    private Logger logger = LoggerFactory.getLogger(ModbusTcpThingHandler.class);
    private ModbusTcpConfiguration config;
    private ModbusSlaveEndpoint endpoint;
    private Supplier<ModbusManager> managerRef;
    private volatile EndpointPoolConfiguration configuration;

    public ModbusTcpThingHandler(Bridge bridge, Supplier<ModbusManager> managerRef) {
        super(bridge);
        this.managerRef = managerRef;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        this.config = getConfigAs(ModbusTcpConfiguration.class);
        endpoint = new ModbusTCPSlaveEndpoint(config.getHost(), config.getPort());

        EndpointPoolConfiguration configNew = new EndpointPoolConfiguration();
        configNew.setConnectMaxTries(config.getConnectMaxTries());
        configNew.setConnectTimeoutMillis(config.getConnectTimeoutMillis());
        configNew.setInterConnectDelayMillis(config.getTimeBetweenReconnectMillis());
        configNew.setPassivateBorrowMinMillis(config.getTimeBetweenTransactionsMillis());
        configNew.setReconnectAfterMillis(config.getReconnectAfterMillis());
        managerRef.get().addListener(this);
        synchronized (configuration) {
            configuration = configNew;
            managerRef.get().setEndpointPoolConfiguration(endpoint, configuration);
        }

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        managerRef.get().removeListener(this);
    }

    @Override
    public ModbusSlaveEndpoint asSlaveEndpoint() {
        return endpoint;
    }

    @Override
    public int getSlaveId() {
        return config.getId();
    }

    @Override
    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration) {
        synchronized (configuration) {
            if (endpoint.equals(this.endpoint) && !this.configuration.equals(configuration)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(
                                "Endpoint '%s' has conflicting parameters: parameters of this thing {} are different from {}",
                                endpoint, this.configuration, configuration));
            }
        }
    }
}
