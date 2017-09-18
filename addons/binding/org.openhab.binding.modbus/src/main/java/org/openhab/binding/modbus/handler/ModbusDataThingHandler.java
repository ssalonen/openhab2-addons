/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.modbus.ModbusBindingConstants;
import org.openhab.binding.modbus.internal.Transformation;
import org.openhab.binding.modbus.internal.config.ModbusDataConfiguration;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusResponse;
import org.openhab.io.transport.modbus.ModbusWriteCallback;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprintImpl;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprintImpl;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.WriteTaskImpl;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.json.WriteRequestJsonUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusDataThingHandler} is responsible for interperting polled modbus data, as well as handling openhab
 * commands
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusDataThingHandler extends BaseThingHandler implements ModbusReadCallback, ModbusWriteCallback {

    private Logger logger = LoggerFactory.getLogger(ModbusDataThingHandler.class);
    private volatile ModbusDataConfiguration config;

    private static final Map<String, List<Class<? extends State>>> channelIdToAcceptedDataTypes = new HashMap<>();

    static {
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_SWITCH,
                new SwitchItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_CONTACT,
                new ContactItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_DATETIME,
                new DateTimeItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_DIMMER,
                new DimmerItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_NUMBER,
                new NumberItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_STRING,
                new StringItem("").getAcceptedDataTypes());
        channelIdToAcceptedDataTypes.put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER,
                new RollershutterItem("").getAcceptedDataTypes());
    }

    private static Map<ChannelUID, List<Class<? extends State>>> channelUIDToAcceptedDataTypes;
    private volatile ValueType readValueType;
    private volatile ValueType writeValueType;
    private volatile Transformation readTransformation;
    private volatile Transformation writeTransformation;
    private volatile Optional<Integer> readIndex;
    private volatile Optional<Integer> readSubIndex;

    public ModbusDataThingHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Thing {} '{}' received command '{}' to channel '{}'", getThing().getUID(), getThing().getLabel(),
                command, channelUID);

        // Note, poller status does not matter for writes. This is on purpose: read errors should have no implication to
        // write attempts
        // For the same reason we do not check the status of this thing
        if (writeValueType == null) {
            // not initialized yet
            return;
        }

        if (RefreshType.REFRESH.equals(command)) {
            logger.trace(
                    "Thing {} '{}' received REFRESH which not implemented yet. Aborting processing of command '{}' to channel '{}'",
                    getThing().getUID(), getThing().getLabel(), command, channelUID);
            return;
        }
        if (config.getWriteStart() == null) {
            return;
        }

        Bridge pollerBridge = getBridge();
        if (pollerBridge == null) {
            logger.debug(
                    "Thing {} '{}' has no poller bridge. Aborting writing of command '{}' to channel '{}' of thing {}",
                    getThing().getUID(), getThing().getLabel(), command, channelUID, getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No poller bridge");
            return;
        }
        if (pollerBridge.getHandler() == null) {
            logger.warn("Poller {} '{}' has no handler. Aborting writing of command '{}' to channel '{}' of thing {}.",
                    pollerBridge.getUID(), pollerBridge.getLabel(), command, channelUID, getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Poller '%s' configuration incomplete or with errors", pollerBridge.getLabel()));
            return;
        }

        @SuppressWarnings("null")
        @NonNull
        ModbusPollerThingHandler pollerHandler = (@NonNull ModbusPollerThingHandler) pollerBridge.getHandler();

        PollTask pollTask = pollerHandler.getPollTask();
        if (pollTask == null) {
            logger.warn("WriteThing '{}': No poll task available. Not processing command '{}'", getThing().getLabel(),
                    command);
            return;
        }

        int slaveId = pollTask.getRequest().getUnitID();
        ModbusSlaveEndpoint slaveEndpoint = pollTask.getEndpoint();
        ModbusManager manager = pollerHandler.getManagerRef().get();

        String transformOutput;
        Optional<Command> transformedCommand;
        if (writeTransformation == null || writeTransformation.isIdentityTransform()) {
            transformedCommand = Optional.of(command);
        } else {
            transformOutput = writeTransformation.transform(bundleContext, command.toString());
            if (transformOutput.trim().contains("[")) {
                final Collection<ModbusWriteRequestBlueprint> requests;
                try {
                    requests = WriteRequestJsonUtilities.fromJson(slaveId, transformOutput);
                } catch (Throwable e) {
                    logger.warn(
                            "Thing {} '{}' could handle transformation result '{}'. Original command {}. Error details follow",
                            getThing().getUID(), getThing().getLabel(), transformOutput, command, e);
                    return;
                }

                requests.stream().map(request -> new WriteTaskImpl(slaveEndpoint, request, this))
                        .forEach(writeTask -> manager.submitOneTimeWrite(writeTask));
                return;
            } else {
                transformedCommand = Transformation.tryConvertToCommand(transformOutput);
            }
        }

        if (!transformedCommand.isPresent()) {
            // transformation failed, return
            logger.warn("Cannot process command {} with channel {} since transformation was unsuccessful", command,
                    channelUID);
            return;
        }

        ModbusWriteRequestBlueprint request;
        if (config.getWriteType().equals(WRITE_TYPE_COIL)) {
            Optional<Boolean> commandAsBoolean = ModbusBitUtilities.translateCommand2Boolean(transformedCommand.get());
            if (!commandAsBoolean.isPresent()) {
                logger.warn(
                        "Cannot process command {} with channel {} since command is not OnOffType, OpenClosedType or Decimal trying to write to coil. Do not know how to convert to 0/1",
                        command, channelUID);
                return;
            }
            boolean data = commandAsBoolean.get();
            request = new ModbusWriteCoilRequestBlueprintImpl(slaveId, config.getWriteStart(), data, false);
        } else if (config.getWriteType().equals(WRITE_TYPE_HOLDING)) {
            ModbusRegisterArray data = ModbusBitUtilities.commandToRegisters(transformedCommand.get(), writeValueType);
            boolean writeMultiple = config.isWriteMultipleEvenWithSingleRegisterOrCoil() || data.size() > 1;
            request = new ModbusWriteRegisterRequestBlueprintImpl(slaveId, config.getWriteStart(), data, writeMultiple);
        } else {
            // should not happen
            throw new NotImplementedException();
        }

        WriteTaskImpl writeTask = new WriteTaskImpl(slaveEndpoint, request, this);
        manager.submitOneTimeWrite(writeTask);

    }

    @Override
    public synchronized void initialize() {
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        try {
            config = getConfigAs(ModbusDataConfiguration.class);
            if (config.getReadStart().trim().isEmpty()) {
                readIndex = Optional.empty();
                readSubIndex = Optional.empty();
                readValueType = null;
            } else {
                String[] readParts = config.getReadStart().split("\\.", 2);
                readIndex = Optional.of(Integer.parseInt(readParts[0]));
                readSubIndex = Optional.ofNullable(readParts.length == 2 ? Integer.parseInt(readParts[1]) : null);
                readValueType = ValueType.fromConfigValue(config.getReadValueType());
            }
            if (config.getWriteStart() != null) {
                writeValueType = ValueType.fromConfigValue(config.getWriteValueType());
            } else {
                writeValueType = null;
            }
            readTransformation = new Transformation(config.getReadTransform());
            writeTransformation = new Transformation(config.getWriteTransform());
        } catch (IllegalArgumentException e) {
            logger.error("Initialize failed for thing {} '{}'", getThing().getUID(), getThing().getLabel(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.toString());
            return;
        }

        channelUIDToAcceptedDataTypes = channelIdToAcceptedDataTypes.keySet().stream()
                .collect(Collectors.toMap(channelId -> new ChannelUID(getThing().getUID(), channelId),
                        channel -> channelIdToAcceptedDataTypes.get(channel)));

        validateConfiguration();
    }

    public synchronized void validateConfiguration() {

        Bridge poller = getBridge();
        if (poller == null) {
            logger.debug("Thing {} '{}' has no Poller bridge. Aborting config validation", getThing().getUID(),
                    getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format("No poller bridge set"));
            return;
        }
        if (poller.getStatus() != ThingStatus.ONLINE) {
            logger.debug("Poller bridge {} '{}' of thing {} '{}' is offline. Aborting config validation",
                    poller.getUID(), poller.getLabel(), getThing().getUID(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, String.format(
                    "Poller bridge %s '%s' of the read-write bridge is offline", poller.getUID(), poller.getLabel()));
            return;
        }

        if (poller.getHandler() == null) {
            logger.warn("Poller bridge {} '{}' has no handler. Aborting config validation for thing {} '{}'",
                    poller.getUID(), poller.getLabel(), getThing().getUID(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Poller '%s' configuration incomplete or with errors", poller.getLabel()));
            return;
        }

        @SuppressWarnings("null")
        @NonNull
        ModbusPollerThingHandler handler = (@NonNull ModbusPollerThingHandler) poller.getHandler();
        PollTask pollTask = handler.getPollTask();
        if (pollTask == null) {
            logger.warn("Poller {} '{}' has no poll task. Aborting config validation for thing {} '{}'",
                    poller.getUID(), poller.getLabel(), getThing().getUID(), getThing().getLabel());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Poller '%s' configuration incomplete or with errors", poller.getLabel()));
            return;
        }

        if (!validateReadValueType(pollTask)) {
            return;
        }
        if (!validateReadIndex(pollTask)) {
            return;
        }

        updateStatus(ThingStatus.ONLINE);
    }

    private boolean validateReadValueType(PollTask pollTask) {
        ModbusReadFunctionCode functionCode = pollTask.getRequest().getFunctionCode();
        if ((functionCode == ModbusReadFunctionCode.READ_COILS
                || functionCode == ModbusReadFunctionCode.READ_INPUT_DISCRETES)
                && !ModbusConstants.ValueType.BIT.equals(readValueType)) {
            logger.error(
                    "Thing {} invalid readValueType: Only readValueType='{}' supported with coils or discrete inputs. Value type was: {}",
                    getThing().getUID(), ModbusConstants.ValueType.BIT, config.getReadValueType());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Only readValueType='%s' supported with coils or discrete inputs. Value type was: {}",
                            ModbusConstants.ValueType.BIT, config.getReadValueType()));
            return false;
        } else {
            return true;
        }
    }

    private boolean validateReadIndex(PollTask pollTask) {
        if (!readIndex.isPresent()) {
            return true;
        }
        // bits represented by the value type, e.g. int32 -> 32
        int valueTypeBitCount = readValueType.getBits();
        // textual name for the data element, e.g. register
        // (for logging)
        String dataElementName;
        int dataElementBits;
        switch (pollTask.getRequest().getFunctionCode()) {
            case READ_INPUT_REGISTERS:
            case READ_MULTIPLE_REGISTERS:
                dataElementName = "register";
                dataElementBits = 16;
                break;
            case READ_COILS:
                dataElementName = "coil";
                dataElementBits = 1;
                break;
            case READ_INPUT_DISCRETES:
                dataElementName = "discrete input";
                dataElementBits = 1;
                break;
            default:
                throw new IllegalStateException(pollTask.getRequest().getFunctionCode().toString());
        }

        boolean bitQuery = false;
        switch (pollTask.getRequest().getFunctionCode()) {
            case READ_COILS:
            case READ_INPUT_DISCRETES:
                bitQuery = true;
                if (readSubIndex.isPresent()) {
                    String errmsg = String
                            .format("readStart=X.Y is not allowed to be used with coils or discrete inputs!");
                    logger.error("Thing '{}' invalid readStart: {}", getThing().getUID(), errmsg);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
                    return false;
                }
                break;
        }

        if (valueTypeBitCount >= 16 && readSubIndex.isPresent()) {
            String errmsg = String
                    .format("readStart=X.Y is not allowed to be used with value types larger than 16bit!");
            logger.error("Thing '{}' invalid readStart: {}", getThing().getUID(), errmsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
            return false;
        } else if (!bitQuery && valueTypeBitCount < 16 && !readSubIndex.isPresent()) {
            String errmsg = String.format("readStart=X.Y must be used with value types less than 16bit!");
            logger.error("Thing '{}' invalid readStart: {}", getThing().getUID(), errmsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
            return false;
        } else if (readSubIndex.isPresent() && (readSubIndex.get() + 1) * valueTypeBitCount > 16) {
            // the sub index Y (in X.Y) is above the register limits
            String errmsg = String.format("readStart=X.Y, the value Y is too large");
            logger.error("Thing '{}' invalid readStart: {}", getThing().getUID(), errmsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
            return false;
        }

        int pollStartBitIndex = pollTask.getRequest().getReference() * dataElementBits;
        int pollEndBitIndex = pollStartBitIndex + (pollTask.getRequest().getDataLength() - 1) * dataElementBits;

        int readStartBitIndex = readIndex.get() * dataElementBits;
        int readEndBitIndex = readStartBitIndex + valueTypeBitCount - dataElementBits;

        if (readStartBitIndex < pollStartBitIndex || readEndBitIndex > pollEndBitIndex) {
            String errmsg = String.format(
                    "Out-of-bounds: Poller is reading from index %d to %d but configuration tries to parse %s starting from register %d. Exceeds polled data by %d bits",
                    pollStartBitIndex / dataElementBits, pollEndBitIndex / dataElementBits, readValueType,
                    readIndex.get(), readEndBitIndex - pollEndBitIndex);
            logger.error("ReadThing {} '{}' readIndex is out of bounds: {}", getThing().getUID(), getThing().getLabel(),
                    errmsg);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errmsg);
            return false;
        }
        return true;
    }

    @Override
    public synchronized void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        validateConfiguration();
    }

    private boolean containsOnOff(List<Class<? extends State>> channelAcceptedDataTypes) {
        return channelAcceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OnOffType.class);
        });
    }

    private boolean containsOpenClosed(List<Class<? extends State>> acceptedDataTypes) {
        return acceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OpenClosedType.class);
        });
    }

    @Override
    public void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        if (!readIndex.isPresent()) {
            return;
        }
        DecimalType numericState;
        int subIndex = readSubIndex.orElse(0);
        // index of the bit or 8bit integer
        // e.g. with bit, index=4 means 4th bit (from right)
        // e.g. with 8bit integer, index=3 means high byte of second register
        int dataIndex = readIndex.get() * readValueType.getBits() + subIndex;
        numericState = ModbusBitUtilities.extractStateFromRegisters(registers, dataIndex, readValueType);
        boolean boolValue = !numericState.equals(DecimalType.ZERO);
        processUpdatedValue(numericState, boolValue);
    }

    @Override
    public synchronized void onBits(ModbusReadRequestBlueprint request, BitArray bits) {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }
        if (!readIndex.isPresent()) {
            return;
        }
        boolean boolValue = bits.getBit(readIndex.get());
        DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;

        processUpdatedValue(numericState, boolValue);
    }

    @Override
    public synchronized void onError(ModbusReadRequestBlueprint request, Exception error) {
        logger.error("Thing {} '{}' received read error: {} {}. Stack trace follows for unexpected errors.",
                getThing().getUID(), getThing().getLabel(), error.getClass().getName(), error.getMessage(), error);
        Map<ChannelUID, State> states = new HashMap<>();
        states.put(new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_LAST_READ_ERROR),
                new DateTimeType());

        synchronized (this) {
            // Update channels
            states.forEach((uid, state) -> {
                tryUpdateState(uid, state);
            });

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error with read: %s: %s", error.getClass().getName(), error.getMessage()));
        }

    }

    private Map<ChannelUID, State> processUpdatedValue(DecimalType numericState, boolean boolValue) {
        Map<ChannelUID, State> states = new HashMap<>();
        channelUIDToAcceptedDataTypes.entrySet().stream().forEach(entry -> {
            ChannelUID channelUID = entry.getKey();
            List<Class<? extends State>> acceptedDataTypes = entry.getValue();
            if (acceptedDataTypes.isEmpty()) {
                // Channel is not linked -- skip
                return;
            }

            State boolLikeState;
            if (containsOnOff(acceptedDataTypes)) {
                boolLikeState = boolValue ? OnOffType.ON : OnOffType.OFF;
            } else if (containsOpenClosed(acceptedDataTypes)) {
                boolLikeState = boolValue ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            } else {
                boolLikeState = null;
            }

            State transformedState;
            if (readTransformation.isIdentityTransform()) {
                if (boolLikeState != null) {
                    // A bit of smartness for ON/OFF and OPEN/CLOSED with boolean like items
                    transformedState = boolLikeState;
                } else {
                    // Numeric states always go through transformation. This allows value of 17.5 to be converted to
                    // 17.5% with percent types (instead of raising error)
                    transformedState = readTransformation.transformState(bundleContext, acceptedDataTypes,
                            numericState);
                }
            } else {
                transformedState = readTransformation.transformState(bundleContext, acceptedDataTypes, numericState);
            }

            if (transformedState == null) {
                logger.debug("Thing {} '{}', channel {} will not be updated since transformation was unsuccesful",
                        getThing().getUID(), getThing().getLabel(), channelUID);
            } else {
                states.put(channelUID, transformedState);
            }
        });

        states.put(new ChannelUID(getThing().getUID(), ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS),
                new DateTimeType());

        synchronized (this) {
            updateStatus(ThingStatus.ONLINE);
            // Update channels
            states.forEach((uid, state) -> {
                tryUpdateState(uid, state);
            });
        }
        return states;
    }

    @Override
    public void onError(ModbusWriteRequestBlueprint request, Exception error) {
        DateTimeType now = new DateTimeType();
        logger.error("Unsuccessful write: {} {}", error.getClass().getName(), error.getMessage());
        updateState(ModbusBindingConstants.CHANNEL_LAST_WRITE_ERROR, now);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, String.format(
                "Error with writing request %s: %s: %s", request, error.getClass().getName(), error.getMessage()));
    }

    @Override
    public void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response) {
        logger.debug("Successful write, matching request {}", request);
        DateTimeType now = new DateTimeType();
        updateStatus(ThingStatus.ONLINE);
        updateState(ModbusBindingConstants.CHANNEL_LAST_WRITE_SUCCESS, now);
    }

    private void tryUpdateState(ChannelUID uid, State state) {
        try {
            updateState(uid, state);
        } catch (IllegalArgumentException e) {
            logger.warn("Error updating state '{}' (type {}) to channel {}: {} {}", state,
                    Optional.ofNullable(state).map(s -> s.getClass().getName()).orElse("null"), uid,
                    e.getClass().getName(), e.getMessage());
        }
    }

}
