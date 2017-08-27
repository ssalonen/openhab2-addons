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
import org.openhab.io.transport.modbus.endpoint.ModbusSerialSlaveEndpoint;
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
    private volatile ModbusSerialSlaveEndpoint endpoint;
    private Supplier<ModbusManager> managerRef;
    private volatile EndpointPoolConfiguration poolConfiguration;

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
            config = null;
            endpoint = null;
        }

        ModbusSerialConfiguration configNew = getConfigAs(ModbusSerialConfiguration.class);

        EndpointPoolConfiguration poolConfigurationNew = new EndpointPoolConfiguration();
        poolConfigurationNew.setConnectMaxTries(config.getConnectMaxTries());
        poolConfigurationNew.setConnectTimeoutMillis(config.getConnectTimeoutMillis());
        poolConfigurationNew.setPassivateBorrowMinMillis(config.getTimeBetweenTransactionsMillis());

        // Never reconnect serial connections "automatically"
        poolConfigurationNew.setInterConnectDelayMillis(1000);
        poolConfigurationNew.setReconnectAfterMillis(-1);

        ModbusSerialSlaveEndpoint endpointNew = new ModbusSerialSlaveEndpoint(config.getPort(), config.getBaud(),
                config.getFlowControlIn(), config.getFlowControlOut(), config.getDataBits(), config.getStopBits(),
                config.getParity(), config.getEncoding(), config.isEcho(), config.getReceiveTimeoutMillis());

        synchronized (this) {
            managerRef.get().addListener(this);
            poolConfiguration = poolConfigurationNew;
            endpoint = endpointNew;
            managerRef.get().setEndpointPoolConfiguration(endpoint, poolConfiguration);
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        if (managerRef != null) {
            managerRef.get().removeListener(this);
        }
    }

    @Override
    public ModbusSlaveEndpoint asSlaveEndpoint() {
        return endpoint;
    }

    @Override
    public int getSlaveId() {
        if (config == null) {
            throw new IllegalStateException("Poller not configured, but slave id is queried!");
        }
        return config.getId();
    }

    @Override
    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint otherEndpoint,
            EndpointPoolConfiguration otherConfig) {
        synchronized (this) {
            if (this.poolConfiguration != null && otherEndpoint.equals(this.endpoint)
                    && !this.poolConfiguration.equals(poolConfiguration)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(
                                "Endpoint '%s' has conflicting parameters: parameters of this thing (%s: %s) {} are different from some other things parameter: {}. Ensure that all endpoints pointing to serial port '%s' have same parameters.",
                                endpoint, thing.getUID(), this.thing.getLabel(), this.poolConfiguration, otherConfig,
                                this.endpoint.getPortName()));
            }
        }
    }
}
