package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.internal.items.ItemRegistryImpl;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
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
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusRegisterArrayImpl;
import org.openhab.io.transport.modbus.ModbusRegisterImpl;
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
        }

        public void update() {
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
        }

        public void update() {
            addProvider(new ItemProvider() {

                @Override
                public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
                }

                @Override
                public Collection<Item> getAll() {
                    return items;
                }

                @Override
                public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
                }
            });
        }
    };

    private static final Map<String, Class<? extends Item>> channelToItemClass = new HashMap<>();
    static {
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_SWITCH, SwitchItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_CONTACT, ContactItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_DATETIME, DateTimeItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_DIMMER, DimmerItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_NUMBER, NumberItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_STRING, StringItem.class);
        channelToItemClass.put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, RollershutterItem.class);
    }

    private List<Thing> things = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private List<ItemChannelLink> links = new ArrayList<>();

    @Mock
    private ThingHandlerCallback thingCallback;

    @Mock
    private ThingRegistry thingRegistry;

    private ItemRegistryTestImpl itemRegistry = new ItemRegistryTestImpl();
    private ItemChannelLinkRegistryTestImpl linkRegistry = new ItemChannelLinkRegistryTestImpl();

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

    /**
     * Updates item and link registries such that added items and links are reflected in handlers
     */
    private void updateItemsAndLinks() {
        itemRegistry.update();
        linkRegistry.update();
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

    private ModbusReadThingHandler testReadHandlingGeneric(ModbusReadFunctionCode functionCode, int start,
            String trigger, String transform, String valueType, BitArray bits, ModbusRegisterArray registers,
            Exception error) {
        ModbusSlaveEndpoint endpoint = new ModbusTCPSlaveEndpoint("thisishost", 502);

        int pollLength = 3;

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
        readConfig.put("start", start);
        readConfig.put("trigger", trigger); // should match only on false value
        readConfig.put("transform", transform);
        readConfig.put("valueType", valueType);

        String thingId = "read1";
        //
        // Bind all channels to corresponding items
        //
        for (String channel : channelToItemClass.keySet()) {
            String itemName = channel + "item";
            links.add(new ItemChannelLink(itemName,
                    new ChannelUID(new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, thingId), channel)));
            Class<?> clz = channelToItemClass.get(channel);
            Item item;
            try {
                item = (Item) clz.getConstructor(String.class).newInstance(itemName);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
            items.add(item);
        }

        updateItemsAndLinks();

        ModbusReadThingHandler readHandler = createReadHandler(thingId, readwrite,
                builder -> builder.withConfiguration(readConfig));

        assertThat(readHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));

        if (bits != null) {
            assert registers == null;
            assert error == null;
            readHandler.onBits(request, bits);
        } else if (registers != null) {
            assert bits == null;
            assert error == null;
            readHandler.onRegisters(request, registers);
        } else {
            assert bits == null;
            assert registers == null;
            assert error != null;
            readHandler.onError(request, error);
        }
        return readHandler;
    }

    @Test
    public void testOnBitsSpecificTriggerNotMatching() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_COILS, 0, "0",
                "default", ModbusBitUtilities.VALUE_TYPE_BIT, new BitArray() {

                    @Override
                    public int size() {
                        return 1;
                    }

                    @Override
                    public boolean getBit(int index) {
                        return true; // true does not match trigger=0
                    }
                }, null, null);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(1)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));
    }

    private static void assertThatStateContains(Map<ChannelUID, State> state, String channelId, Object value) {
        Optional<ChannelUID> uid = state.keySet().stream().filter(channelUID -> channelUID.getId().equals(channelId))
                .findFirst();
        assertThat(uid.isPresent(), is(equalTo(true)));
        assertThat(state.get(uid.get()), is(equalTo(value)));
    }

    @Test
    public void testOnBitsSpecificTriggerMatchingZero() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_COILS, 0, "0",
                "default", ModbusBitUtilities.VALUE_TYPE_BIT, new BitArray() {

                    @Override
                    public int size() {
                        return 1;
                    }

                    @Override
                    public boolean getBit(int index) {
                        return false;
                    }
                }, null, null);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(7)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));

        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_CONTACT, OpenClosedType.CLOSED);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_SWITCH, OnOffType.OFF);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_DIMMER, OnOffType.OFF);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_NUMBER, new DecimalType(0));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, new PercentType(0));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_STRING, new StringType("0"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnBitsSpecificTriggerMatchingOne() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_COILS, 0, "1",
                "default", ModbusBitUtilities.VALUE_TYPE_BIT, new BitArray() {

                    @Override
                    public int size() {
                        return 1;
                    }

                    @Override
                    public boolean getBit(int index) {
                        return true;
                    }
                }, null, null);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(7)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));

        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_CONTACT, OpenClosedType.OPEN);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_SWITCH, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_DIMMER, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_NUMBER, new DecimalType(1));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, new PercentType(1));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_STRING, new StringType("1"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnRegistersSpecificTriggerMatchingInt16MinusThree() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, 0,
                "-3", "default", ModbusBitUtilities.VALUE_TYPE_INT16, null,
                new ModbusRegisterArrayImpl(new ModbusRegister[] { new ModbusRegisterImpl((byte) 0xff, (byte) 0xfd) }),
                null);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(6)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));

        // -3 converts to "true"
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_CONTACT, OpenClosedType.OPEN);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_SWITCH, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_DIMMER, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_NUMBER, new DecimalType(-3));
        // roller shutter fails since -3 is invalid value (not between 0...100)
        // assertThatStateContains(state, ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, new PercentType(1));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_STRING, new StringType("-3"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnRegistersWildcardTriggerFloat32TwoPointThree() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, 0,
                "*", "default", ModbusBitUtilities.VALUE_TYPE_FLOAT32, null,
                new ModbusRegisterArrayImpl(new ModbusRegister[] { new ModbusRegisterImpl((byte) 0x40, (byte) 0x13),
                        new ModbusRegisterImpl((byte) 0x33, (byte) 0x33) }),
                null);

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(7)));

        assertThat(
                state.keySet().stream()
                        .filter(channelUID -> channelUID.getId()
                                .equals(ModbusBindingConstants.CHANNEL_LAST_READ_SUCCESS))
                        .findFirst().isPresent(),
                is(equalTo(true)));

        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_CONTACT, OpenClosedType.OPEN);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_SWITCH, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_DIMMER, OnOffType.ON);
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_NUMBER, new DecimalType(2.299999952316284));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_ROLLERSHUTTER,
                new PercentType("2.299999952316284"));
        assertThatStateContains(state, ModbusBindingConstants.CHANNEL_STRING, new StringType("2.299999952316284"));
        // no datetime, conversion not possible without transformation
    }

    @Test
    public void testOnError() {
        ModbusReadThingHandler readHandler = testReadHandlingGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS, 0,
                "*", "default", ModbusBitUtilities.VALUE_TYPE_FLOAT32, null, null, new Exception("fooerror"));

        assertThat(readHandler.getLastState().isPresent(), is(equalTo(true)));
        Map<ChannelUID, State> state = readHandler.getLastState().get();
        assertThat(state.size(), is(equalTo(1)));
        assertThat(state.keySet().stream()
                .filter(channelUID -> channelUID.getId().equals(ModbusBindingConstants.CHANNEL_LAST_READ_ERROR))
                .findFirst().isPresent(), is(equalTo(true)));
    }

    @Test
    public void testTransformationTODO() {

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
