/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.modbus.internal.config.ModbusWriteConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusManager.WriteTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
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

    private static class SingleBitArray implements BitArray {

        private boolean bit;

        public SingleBitArray(boolean bit) {
            this.bit = bit;
        }

        @Override
        public boolean getBit(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return bit;
        }

        @Override
        public int size() {
            return 1;
        }

    }

    private static class ModbusWriteCoilRequestBlueprintImpl implements ModbusWriteCoilRequestBlueprint {
        private int slaveId;
        private int reference;
        private BitArray bits;

        public ModbusWriteCoilRequestBlueprintImpl(int slaveId, int reference, boolean data) {
            super();
            this.slaveId = slaveId;
            this.reference = reference;
            this.bits = new SingleBitArray(data);
            ;
        }

        @Override
        public int getUnitID() {
            return slaveId;
        }

        @Override
        public int getReference() {
            return reference;
        }

        @Override
        public ModbusWriteFunctionCode getFunctionCode() {
            return ModbusWriteFunctionCode.WRITE_COIL;
        }

        @Override
        public BitArray getCoils() {
            return bits;
        }
    }

    private static class WriteTaskImpl implements WriteTask {

        private ModbusSlaveEndpoint endpoint;
        private ModbusWriteRequestBlueprint request;
        private ModbusWriteCallback callback;

        public WriteTaskImpl(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
                ModbusWriteCallback callback) {
            super();
            this.endpoint = endpoint;
            this.request = request;
            this.callback = callback;
        }

        @Override
        public ModbusSlaveEndpoint getEndpoint() {
            return endpoint;
        }

        @Override
        public ModbusWriteRequestBlueprint getRequest() {
            return request;
        }

        @Override
        public ModbusWriteCallback getCallback() {
            return callback;
        }

    }

    private Logger logger = LoggerFactory.getLogger(ModbusReadWriteThingHandler.class);
    private ModbusSlaveEndpoint endpoint;
    private volatile ModbusWriteConfiguration config;

    public ModbusReadWriteThingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        //
        // public static final String CHANNEL_SWITCH = "switch";
        // public static final String CHANNEL_CONTACT = "contact";
        // public static final String CHANNEL_DATETIME = "datetime";
        // public static final String CHANNEL_DIMMER = "dimmer";
        // public static final String CHANNEL_NUMBER = "number";
        // public static final String CHANNEL_STRING = "string";
        // public static final String CHANNEL_ROLLERSHUTTER = "rollershutter";
        // public static final String CHANNEL_LAST_SUCCESS = "lastSuccess";
        // public static final String CHANNEL_LAST_ERROR = "lastError";
        //
        forEachChildWriter(handler -> {
            Channel childChannel = handler.getThing().getChannel(channelUID.getId());
            if (childChannel == null) {
                logger.warn("no channel {}", channelUID);
            } else {
                handler.handleCommand(childChannel.getUID(), command);
            }
        });

        // FIXME: commands should be forwarded to writers, and not handled in readerwrite thing
        // move the below code to writers

        Bridge pollerBridge = getBridge();
        if (pollerBridge == null) {
            logger.debug("ReadWriteThing '{}' has no poller bridge. Aborting writing of command '{}' to channel '{}'",
                    getThing().getLabel(), command, channelUID);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No poller bridge");
            return;
        }
        ModbusPollerThingHandler pollerHandler = (ModbusPollerThingHandler) pollerBridge.getHandler();
        ModbusEndpointThingHandler modbusEndpointThingHandler = pollerHandler.getEndpointThingHandler();
        if (modbusEndpointThingHandler == null) {
            logger.debug(
                    "Poller ('{}') (of ReadWriteThing '{}') has no EndpointBridge. Aborting writing of command '{}' to channel '{}'",
                    pollerBridge.getLabel(), getThing().getLabel(), command, channelUID);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Poller has no endpoint bridge");
            return;
        }
        PollTask pollTask = pollerHandler.getPollTask();
        ModbusSlaveEndpoint slaveEndpoint = modbusEndpointThingHandler.asSlaveEndpoint();

        int slaveId = modbusEndpointThingHandler.getSlaveId();
        ModbusManager manager = pollerHandler.getManagerRef().getManager();

        if (config.getType().equals(WRITE_TYPE_COIL)) {
            int reference = pollTask.getRequest().getReference() + config.getStart();
            boolean data = true;
            ModbusWriteCoilRequestBlueprintImpl request = new ModbusWriteCoilRequestBlueprintImpl(slaveId, reference,
                    data);
            // manager.submitOneTimeWrite(task)
        } else if (config.getType().equals(WRITE_TYPE_HOLDING)) {

        } else {
            // should not happen
            throw new NotImplementedException();
        }

        // new ModbusW
        if (channelUID.getId().equals(CHANNEL_SWITCH)) {
            // manager.submitOneTimeWrite(task)
        }
    }

    @Override
    public synchronized void initialize() {
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.INITIALIZING);
        config = getConfigAs(ModbusWriteConfiguration.class);
        updateStatus(ThingStatus.ONLINE);
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
        String msg = String.format("Read write thing handler got read error: {} {}", error.getClass().getName(),
                error.getMessage());
        logger.debug(msg, error);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, msg);
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
        optionalLastState.ifPresent(lastState -> lastState.forEach((uid, state) -> {
            if (getThing().getChannel(uid.getId()) != null) {
                updateState(uid.getId(), state);
            }
        }));
    }

    private void forEachChildWriter(Consumer<ModbusWriteThingHandler> consumer) {
        List<ModbusWriteThingHandler> readers = getWriters();
        // Call each readers callback, and update this bridge's items (matching by channel id)
        readers.stream().forEach(reader -> {
            consumer.accept(reader);
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
