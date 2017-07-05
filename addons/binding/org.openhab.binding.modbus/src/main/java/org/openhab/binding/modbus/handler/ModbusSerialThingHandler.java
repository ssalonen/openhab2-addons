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
import org.openhab.binding.modbus.internal.config.ModbusSerialConfiguration;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusSerialThingHandler extends AbstractModbusBridgeThing
        implements ModbusEndpointThingHandler, ModbusManagerListener {

    private Logger logger = LoggerFactory.getLogger(ModbusSerialThingHandler.class);
    private ModbusSerialConfiguration config;
    private volatile ModbusSlaveEndpoint endpoint;
    private Supplier<ModbusManager> managerRef;
    private volatile EndpointPoolConfiguration configuration;

    public ModbusSerialThingHandler(Bridge bridge, Supplier<ModbusManager> managerRef) {
        super(bridge);
        this.managerRef = managerRef;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        synchronized (this) {
            updateStatus(ThingStatus.UNKNOWN);
            this.configuration = null;
        }

        this.config = getConfigAs(ModbusSerialConfiguration.class);
        // FIXME:
        // new SerialParameters
        // endpoint = new ModbusSerialSlaveEndpoint(config.getHost(), config.getPort());

        EndpointPoolConfiguration configNew = new EndpointPoolConfiguration();
        configNew.setConnectMaxTries(config.getConnectMaxTries());
        configNew.setConnectTimeoutMillis(config.getConnectTimeoutMillis());
        configNew.setPassivateBorrowMinMillis(config.getTimeBetweenTransactionsMillis());

        // Never reconnect serial connections "automatically"
        configNew.setInterConnectDelayMillis(1000);
        configNew.setReconnectAfterMillis(-1);

        synchronized (this) {
            managerRef.get().addListener(this);
            configuration = configNew;
            managerRef.get().setEndpointPoolConfiguration(endpoint, configuration);
            updateStatus(ThingStatus.ONLINE);
        }
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
    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration otherConfig) {
        synchronized (this) {
            if (this.configuration != null && endpoint.equals(this.endpoint)
                    && !this.configuration.equals(configuration)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(
                                "Endpoint '%s' has conflicting parameters: parameters of this thing (%s) {} are different from {}",
                                endpoint, this.thing, this.configuration, otherConfig));
            }
        }
    }
}
