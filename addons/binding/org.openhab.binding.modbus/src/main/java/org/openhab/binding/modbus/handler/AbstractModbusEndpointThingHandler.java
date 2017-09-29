package org.openhab.binding.modbus.handler;

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManagerListener;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public abstract class AbstractModbusEndpointThingHandler<E extends ModbusSlaveEndpoint, C>
        extends AbstractModbusBridgeThing implements ModbusManagerListener, ModbusEndpointThingHandler {

    @Nullable
    protected volatile C config;
    @Nullable
    protected volatile E endpoint;
    @NonNull
    protected Supplier<ModbusManager> managerRef;
    protected volatile EndpointPoolConfiguration poolConfiguration;

    public AbstractModbusEndpointThingHandler(@NonNull Bridge bridge, @NonNull Supplier<ModbusManager> managerRef) {
        super(bridge);
        this.managerRef = managerRef;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    /**
     * Must be overriden by subclasses to initialize config, endpoint, and poolConfiguration
     */
    protected abstract void configure();

    @Override
    public void initialize() {
        synchronized (this) {
            updateStatus(ThingStatus.UNKNOWN);
            config = null;
            configure();

            managerRef.get().addListener(this);
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
    public abstract int getSlaveId();

    protected abstract String formatConflictingParameterError(EndpointPoolConfiguration otherPoolConfig);

    @Override
    public Supplier<ModbusManager> getManagerRef() {
        return managerRef;
    }

    @Override
    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint otherEndpoint,
            EndpointPoolConfiguration otherPoolConfiguration) {
        synchronized (this) {
            if (endpoint == null) {
                return;
            }
            if (this.poolConfiguration != null && otherEndpoint.equals(this.endpoint)
                    && !this.poolConfiguration.equals(otherPoolConfiguration)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        formatConflictingParameterError(otherPoolConfiguration));
            }
        }
    }

}