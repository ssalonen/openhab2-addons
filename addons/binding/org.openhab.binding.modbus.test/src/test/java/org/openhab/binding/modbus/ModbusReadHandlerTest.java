package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.eclipse.smarthome.core.types.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandlerImpl;
import org.openhab.binding.modbus.handler.ModbusReadThingHandler;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ModbusReadHandlerTest {

    private static class Tuple<T1, T2> {
        T1 obj1;
        T2 obj2;

        public Tuple(T1 obj1, T2 obj2) {
            super();
            this.obj1 = obj1;
            this.obj2 = obj2;
        }

    }

    private List<Thing> things = new ArrayList<>();

    @Mock
    private ThingHandlerCallback thingCallback;

    @Mock
    private ThingRegistry thingRegistry;

    private Map<ChannelUID, List<State>> stateUpdates = new HashMap<>();

    private Map<String, String> channelToAcceptedType = ImmutableMap.<String, String> builder()
            .put(ModbusBindingConstants.CHANNEL_SWITCH, "Switch").put(ModbusBindingConstants.CHANNEL_CONTACT, "Contact")
            .put(ModbusBindingConstants.CHANNEL_DATETIME, "DateTime")
            .put(ModbusBindingConstants.CHANNEL_DIMMER, "Dimmer").put(ModbusBindingConstants.CHANNEL_NUMBER, "Number")
            .put(ModbusBindingConstants.CHANNEL_STRING, "String")
            .put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, "Rollershutter").build();

    @SuppressWarnings("restriction")
    private void registerThingToMockRegistry(Thing thing) {
        things.add(thing);
        // update bridge with the new child thing
        if (thing.getBridgeUID() != null) {
            ThingUID bridgeUID = thing.getBridgeUID();
            things.stream().filter(t -> t.getUID().equals(bridgeUID)).findFirst()
                    .ifPresent(t -> ((BridgeImpl) t).addThing(thing));
        }
    }

    private void hookItemRegistry(ThingHandler thingHandler)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field thingRegisteryField = BaseThingHandler.class.getDeclaredField("thingRegistry");
        thingRegisteryField.setAccessible(true);
        thingRegisteryField.set(thingHandler, thingRegistry);
    }

    private void hookStatusUpdates(Thing thing) {
        Mockito.doAnswer(invocation -> {
            thing.setStatusInfo(invocation.getArgumentAt(1, ThingStatusInfo.class));
            return null;
        }).when(thingCallback).statusUpdated(Matchers.same(thing), Matchers.any());
    }

    private void hookStateUpdates(Thing thing) {
        Mockito.doAnswer(invocation -> {
            ChannelUID channelUID = invocation.getArgumentAt(0, ChannelUID.class);
            State state = invocation.getArgumentAt(1, State.class);
            stateUpdates.putIfAbsent(channelUID, new ArrayList<>());
            stateUpdates.get(channelUID).add(state);
            return null;
        }).when(thingCallback).stateUpdated(any(), any());
    }

    private Tuple<Bridge, Bridge> createReadWriteAndPoller(String readwriteId, String pollerId, PollTask task) {

        final Bridge poller;
        {
            ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER, pollerId);
            BridgeBuilder builder = BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER, thingUID)
                    .withLabel("label for " + pollerId);
            for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
                String channelId = entry.getKey();
                String channelAcceptedType = entry.getValue();
                builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
            }
            poller = builder.build();
            poller.setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));
            ModbusPollerThingHandlerImpl handler = Mockito.mock(ModbusPollerThingHandlerImpl.class);
            doReturn(task).when(handler).getPollTask();
            poller.setHandler(handler);
            registerThingToMockRegistry(poller);
        }

        final Bridge readwrite;
        {
            ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, readwriteId);
            BridgeBuilder builder = BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, thingUID)
                    .withLabel("label for " + readwriteId);
            for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
                String channelId = entry.getKey();
                String channelAcceptedType = entry.getValue();
                builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
            }
            builder = builder.withBridge(poller.getUID());
            readwrite = builder.build();
            readwrite.setStatusInfo(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, ""));
            registerThingToMockRegistry(readwrite);
        }

        return new Tuple<Bridge, Bridge>(readwrite, poller);
    }

    private ModbusReadThingHandler createReadHandler(String id, Bridge bridge,
            Function<ThingBuilder, ThingBuilder> builderConfigurator)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, id);
        ThingBuilder builder = ThingBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, thingUID)
                .withLabel("label for " + id);
        for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
        }
        if (builderConfigurator != null) {
            builder = builderConfigurator.apply(builder);
        }

        Thing read = builder.withBridge(bridge.getUID()).build();
        registerThingToMockRegistry(read);
        hookStatusUpdates(read);
        hookStateUpdates(read);

        ModbusReadThingHandler readThingHandler = new ModbusReadThingHandler(read);
        hookItemRegistry(readThingHandler);
        read.setHandler(readThingHandler);
        readThingHandler.setCallback(thingCallback);
        readThingHandler.initialize();
        return readThingHandler;
    }

    @Before
    public void setUp() {
        Mockito.when(thingRegistry.get(Matchers.any())).then(invocation -> {
            ThingUID uid = invocation.getArgumentAt(0, ThingUID.class);
            for (Thing thing : things) {
                if (thing.getUID().equals(uid)) {
                    return thing;
                }
            }
            throw new IllegalArgumentException("UID is unknown: " + uid.getAsString());
        });
    }

    @Test
    public void testOnBits() {

    }

    private void testOutOfBoundsGeneric(int pollLength, int smallestStartThatIsInvalid,
            ModbusReadFunctionCode functionCode, String valueType)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(pollLength).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Tuple<Bridge, Bridge> bridgeThings = createReadWriteAndPoller("readwrite1", "poller1", task);
        Bridge readwrite = bridgeThings.obj1;
        Bridge poller = bridgeThings.obj2;

        Configuration readConfig = new Configuration();
        readConfig.put("start", smallestStartThatIsInvalid);
        readConfig.put("trigger", "*");
        readConfig.put("transform", "default");
        readConfig.put("valueType", valueType);
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig));
        assertThat(readHandler.getThing().getStatus(), is(equalTo(ThingStatus.OFFLINE)));

        // start - 1 should be ok
        Configuration readConfig2 = new Configuration();
        readConfig.put("start", smallestStartThatIsInvalid - 1);
        readConfig.put("trigger", "*");
        readConfig.put("transform", "default");
        readConfig.put("valueType", valueType);
        ModbusReadThingHandler readHandler2 = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig2));
        assertThat(readHandler2.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

    @Test
    public void testCoilsOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // value type plays no role with coils
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_COILS, null);
    }

    @Test
    public void testDiscreteOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // value type plays no role with discrete
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_DISCRETES, null);
    }

    @Test
    public void testHoldingInt8OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 7, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT8);
    }

    @Test
    public void testHoldingBitOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 48, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testHoldingInt16OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT16);
    }

    @Test
    public void testHoldingUInt16OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT16);
    }

    @Test
    public void testHoldingInt32OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32);
    }

    @Test
    public void testHoldingUInt32OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32);
    }

    @Test
    public void testHoldingInt32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP);
    }

    @Test
    public void testHoldingUInt32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP);
    }

    @Test
    public void testHoldingFloat32SOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32);
    }

    @Test
    public void testHoldingFloat32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP);
    }

    @Test
    public void testInputRegisterInt8OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 7, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT8);
    }

    @Test
    public void testInputRegisterBitOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 48, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testInputRegisterInt16OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT16);
    }

    @Test
    public void testInputRegisterUInt16OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_UINT16);
    }

    @Test
    public void testInputRegisterInt32OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT32);
    }

    @Test
    public void testInputRegisterUInt32OutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_UINT32);
    }

    @Test
    public void testInputRegisterInt32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP);
    }

    @Test
    public void testInputRegisterUInt32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP);
    }

    @Test
    public void testInputRegisterFloat32SOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32);
    }

    @Test
    public void testInputRegisterFloat32SwapOutOfIndex()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP);
    }

    @Test
    public void testOnRegistersSpecificTriggerMatchingAndTransformation()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

    }

    @Test
    public void testOnRegistersSpecificTriggerNotMatching() {
    }

    @Test
    public void testOnRegistersSpecificTriggerMatching() {
    }

    @Test
    public void testOnRegistersFloat32() {
    }

    @Test
    public void testOnError() {
    }

}
