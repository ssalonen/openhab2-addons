package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.internal.items.ItemRegistryImpl;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
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
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkProvider;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
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
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.openhab.io.transport.modbus.endpoint.ModbusTCPSlaveEndpoint;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("restriction")
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

    private class ItemChannelLinkRegistryTestImpl extends ItemChannelLinkRegistry {
        public ItemChannelLinkRegistryTestImpl() {
            super();
            this.setItemRegistry(itemRegistry);
            this.setThingRegistry(thingRegistry);

            addProvider(new ItemChannelLinkProvider() {

                @Override
                public void addProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
                }

                @Override
                public Collection<ItemChannelLink> getAll() {
                    return links;
                }

                @Override
                public void removeProviderChangeListener(ProviderChangeListener<ItemChannelLink> listener) {
                }
            });
        }
    };

    private class ItemRegistryTestImpl extends ItemRegistryImpl {
        public ItemRegistryTestImpl() {
            super();

            addProvider(new ItemProvider() {

                @Override
                public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
                }

                @Override
                public Collection<Item> getAll() {
                    return items.values();
                }

                @Override
                public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
                }
            });
        }
    };

    private List<Thing> things = new ArrayList<>();
    private Map<String, Item> items = new HashMap<>();
    private List<ItemChannelLink> links = new ArrayList<>();

    @Mock
    private ThingHandlerCallback thingCallback;

    @Mock
    private ThingRegistry thingRegistry;

    private ItemRegistry itemRegistry = new ItemRegistryTestImpl();
    private ItemChannelLinkRegistry linkRegistry = new ItemChannelLinkRegistryTestImpl();

    Map<ChannelUID, List<State>> stateUpdates = new HashMap<>();

    private Map<String, String> channelToAcceptedType = ImmutableMap.<String, String> builder()
            .put(ModbusBindingConstants.CHANNEL_SWITCH, "Switch").put(ModbusBindingConstants.CHANNEL_CONTACT, "Contact")
            .put(ModbusBindingConstants.CHANNEL_DATETIME, "DateTime")
            .put(ModbusBindingConstants.CHANNEL_DIMMER, "Dimmer").put(ModbusBindingConstants.CHANNEL_NUMBER, "Number")
            .put(ModbusBindingConstants.CHANNEL_STRING, "String")
            .put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, "Rollershutter").build();

    private void registerThingToMockRegistry(Thing thing) {
        things.add(thing);
        // update bridge with the new child thing
        if (thing.getBridgeUID() != null) {
            ThingUID bridgeUID = thing.getBridgeUID();
            things.stream().filter(t -> t.getUID().equals(bridgeUID)).findFirst()
                    .ifPresent(t -> ((BridgeImpl) t).addThing(thing));
        }
    }

    private void hookItemRegistry(ThingHandler thingHandler) {
        Field thingRegisteryField;
        try {
            thingRegisteryField = BaseThingHandler.class.getDeclaredField("thingRegistry");
            thingRegisteryField.setAccessible(true);
            thingRegisteryField.set(thingHandler, thingRegistry);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void hookLinkRegistry(ThingHandler thingHandler) {
        Field linkRegistryField;
        try {
            linkRegistryField = BaseThingHandler.class.getDeclaredField("linkRegistry");
            linkRegistryField.setAccessible(true);
            linkRegistryField.set(thingHandler, linkRegistry);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
            Function<ThingBuilder, ThingBuilder> builderConfigurator) {
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
        hookLinkRegistry(readThingHandler);
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

    private void testOutOfBoundsGeneric(int pollLength, int smallestStartThatIsInvalid,
            ModbusReadFunctionCode functionCode, String valueType) {
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
        readConfig2.put("start", smallestStartThatIsInvalid - 1);
        readConfig2.put("trigger", "*");
        readConfig2.put("transform", "default");
        readConfig2.put("valueType", valueType);
        ModbusReadThingHandler readHandler2 = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig2));
        assertThat(readHandler2.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

    @Test
    public void testCoilsOutOfIndex() {
        // value type plays no role with coils
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_COILS, ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testDiscreteOutOfIndex() {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_DISCRETES, ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testHoldingInt8OutOfIndex() {
        testOutOfBoundsGeneric(3, 6, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT8);
    }

    @Test
    public void testHoldingBitOutOfIndex() {
        testOutOfBoundsGeneric(3, 48, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testHoldingInt16OutOfIndex() {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT16);
    }

    @Test
    public void testHoldingUInt16OutOfIndex() {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT16);
    }

    @Test
    public void testHoldingInt32OutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32);
    }

    @Test
    public void testHoldingUInt32OutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32);
    }

    @Test
    public void testHoldingInt32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP);
    }

    @Test
    public void testHoldingUInt32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP);
    }

    @Test
    public void testHoldingFloat32SOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32);
    }

    @Test
    public void testHoldingFloat32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP);
    }

    @Test
    public void testInputRegisterInt8OutOfIndex() {
        // 3 registers => room for 6 x 8bit integers. Last valid read index is thus 5, and smallest invalid start index
        // is 6.
        testOutOfBoundsGeneric(3, 6, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT8);
    }

    @Test
    public void testInputRegisterBitOutOfIndex() {
        testOutOfBoundsGeneric(3, 48, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_BIT);
    }

    @Test
    public void testInputRegisterInt16OutOfIndex() {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT16);
    }

    @Test
    public void testInputRegisterUInt16OutOfIndex() {
        testOutOfBoundsGeneric(3, 3, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_UINT16);
    }

    @Test
    public void testInputRegisterInt32OutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_INT32);
    }

    @Test
    public void testInputRegisterUInt32OutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS, ModbusBitUtilities.VALUE_TYPE_UINT32);
    }

    @Test
    public void testInputRegisterInt32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_INT32_SWAP);
    }

    @Test
    public void testInputRegisterUInt32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_UINT32_SWAP);
    }

    @Test
    public void testInputRegisterFloat32SOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32);
    }

    @Test
    public void testInputRegisterFloat32SwapOutOfIndex() {
        testOutOfBoundsGeneric(3, 2, ModbusReadFunctionCode.READ_INPUT_REGISTERS,
                ModbusBitUtilities.VALUE_TYPE_FLOAT32_SWAP);
    }

    @Test
    public void testOnRegistersSpecificTriggerMatchingAndTransformation() {

    }

    @Test
    public void testOnBitsSpecificTriggerNotMatching() {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        int pollLength = 3;
        ModbusReadFunctionCode functionCode = ModbusReadFunctionCode.READ_COILS;

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
        readConfig.put("start", 0);
        readConfig.put("trigger", "1"); // should match only on true value
        readConfig.put("transform", "default");
        readConfig.put("valueType", ModbusBitUtilities.VALUE_TYPE_BIT);
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig));
        // linkRegistery.getLinkedItems (for datatpyes)
        // linkRegistry.getLinks (for identifying linked channels)

        // FIXME: hook itemRegitry to ItemChannelLinkRegistry
        // FIXME: link all channels to dummy items from channelToAcceptedType
        // links.add(new ItemChannelLink("foobar", channelUID))

        assertThat(readHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        BitArray oneFalseBit = new BitArray() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean getBit(int index) {
                return false;
            }
        };

        readHandler.onBits(request, oneFalseBit);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(1)));
        ChannelUID channelUID = state.keySet().stream().findFirst().get();

        assertThat(channelUID.getThingUID(), is(equalTo(readHandler.getThing().getUID())));
        assertThat(channelUID.getId(), is(equalTo(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS)));

    }

    @Test
    public void testOnBitsSpecificTriggerMatching() {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        int pollLength = 3;
        ModbusReadFunctionCode functionCode = ModbusReadFunctionCode.READ_COILS;

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
        readConfig.put("start", 0);
        readConfig.put("trigger", "0"); // should match only on false value
        readConfig.put("transform", "default");
        readConfig.put("valueType", ModbusBitUtilities.VALUE_TYPE_BIT);
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig));
        // linkRegistery.getLinkedItems (for datatpyes)
        // linkRegistry.getLinks (for identifying linked channels)

        // FIXME: hook itemRegitry to ItemChannelLinkRegistry
        // FIXME: link all channels to dummy items from channelToAcceptedType

        // links.add(new ItemChannelLink("foobar", channelUID));

        assertThat(readHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        BitArray oneFalseBit = new BitArray() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean getBit(int index) {
                return false;
            }
        };

        readHandler.onBits(request, oneFalseBit);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(2)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));

    }

    @Test
    public void testOnRegistersSpecificTriggerMatching() {
    }

    @Test
    public void testOnRegistersFloat32() {
    }

    @Test
    public void testOnBits() {

    }

    @Test
    public void testOnError() {
    }

    private void testValueTypeGeneric(ModbusReadFunctionCode functionCode, String valueType,
            ThingStatus expectedStatus) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        // Minimally mocked request
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        doReturn(3).when(request).getDataLength();
        doReturn(functionCode).when(request).getFunctionCode();

        PollTask task = Mockito.mock(PollTask.class);
        doReturn(endpoint).when(task).getEndpoint();
        doReturn(request).when(task).getRequest();

        Tuple<Bridge, Bridge> bridgeThings = createReadWriteAndPoller("readwrite1", "poller1", task);
        Bridge readwrite = bridgeThings.obj1;

        Configuration readConfig = new Configuration();
        readConfig.put("start", 1);
        readConfig.put("trigger", "*");
        readConfig.put("transform", "default");
        readConfig.put("valueType", valueType);
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwrite,
                builder -> builder.withConfiguration(readConfig));
        assertThat(readHandler.getThing().getStatus(), is(equalTo(expectedStatus)));
    }

    @Test
    public void testCoilDoesNotAcceptFloat32ValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_COILS, ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                ThingStatus.OFFLINE);
    }

    @Test
    public void testCoilAcceptsBitValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_COILS, ModbusBitUtilities.VALUE_TYPE_BIT, ThingStatus.ONLINE);
    }

    @Test
    public void testDiscreteInputDoesNotAcceptFloat32ValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_INPUT_DISCRETES, ModbusBitUtilities.VALUE_TYPE_FLOAT32,
                ThingStatus.OFFLINE);
    }

    @Test
    public void testDiscreteInputAcceptsBitValueType() {
        testValueTypeGeneric(ModbusReadFunctionCode.READ_INPUT_DISCRETES, ModbusBitUtilities.VALUE_TYPE_BIT,
                ThingStatus.ONLINE);
    }
}
