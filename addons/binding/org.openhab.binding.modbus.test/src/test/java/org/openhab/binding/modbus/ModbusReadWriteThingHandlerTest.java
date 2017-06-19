package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
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
import org.eclipse.smarthome.core.types.Command;
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
import org.openhab.binding.modbus.handler.ModbusReadWriteThingHandler;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.binding.modbus.handler.ModbusWriteThingHandler;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ModbusReadWriteThingHandlerTest {

    @Mock
    private ModbusManager modbusManager;

    @Mock
    private ThingRegistry thingRegistry;

    private ModbusTcpThingHandler tcpThingHandler;
    private Bridge endpoint;
    private Bridge poller;
    private List<Thing> things = new ArrayList<>();

    @Mock
    private ThingHandlerCallback thingCallback;

    private Map<ChannelUID, List<State>> stateUpdates = new HashMap<>();

    private Map<String, String> channelToAcceptedType = ImmutableMap.<String, String> builder()
            .put(ModbusBindingConstants.CHANNEL_SWITCH, "Switch").put(ModbusBindingConstants.CHANNEL_CONTACT, "Contact")
            .put(ModbusBindingConstants.CHANNEL_DATETIME, "DateTime")
            .put(ModbusBindingConstants.CHANNEL_DIMMER, "Dimmer").put(ModbusBindingConstants.CHANNEL_NUMBER, "Number")
            .put(ModbusBindingConstants.CHANNEL_STRING, "String")
            .put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, "Rollershutter").build();

    private static BridgeBuilder createTcpThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_TCP,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_TCP, id)).withLabel("label for " + id);
    }

    private static BridgeBuilder createPollerThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER, id)).withLabel("label for " + id);
    }

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

    private void hookItemRegistry(ThingHandler thingHandler)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field thingRegisteryField = BaseThingHandler.class.getDeclaredField("thingRegistry");
        thingRegisteryField.setAccessible(true);
        thingRegisteryField.set(thingHandler, thingRegistry);
    }

    private ModbusReadWriteThingHandler createInitializedReadWriteWithHandler(String id)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteHandler = createInitializedReadWriteWithHandler(id, poller.getUID(), null);
        assertThat(readwriteHandler.getThing().getStatus(), is(equalTo(ThingStatus.ONLINE)));
        return readwriteHandler;

    }

    private ModbusReadWriteThingHandler createInitializedReadWriteWithHandler(String id, ThingUID bridge,
            Consumer<ModbusReadWriteThingHandler> beforeInitHook)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, id);
        BridgeBuilder builder = BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, thingUID)
                .withLabel("label for " + id);
        for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
        }
        if (bridge != null) {
            builder = builder.withBridge(poller.getUID());
        }
        Bridge readwrite = builder.build();
        registerThingToMockRegistry(readwrite);
        hookStatusUpdates(readwrite);
        hookStateUpdates(readwrite);

        ModbusReadWriteThingHandler readwriteThingHandler = new ModbusReadWriteThingHandler(readwrite);
        hookItemRegistry(readwriteThingHandler);
        readwriteThingHandler.setCallback(thingCallback);
        readwrite.setHandler(readwriteThingHandler);
        if (beforeInitHook != null) {
            beforeInitHook.accept(readwriteThingHandler);
        }
        readwriteThingHandler.initialize();
        return readwriteThingHandler;
    }

    private ModbusWriteThingHandler createWriteHandler(String id, Bridge bridge)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, id);
        ThingBuilder builder = ThingBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, thingUID)
                .withLabel("label for " + id);
        for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
        }

        Thing write = builder.withBridge(bridge.getUID()).build();
        registerThingToMockRegistry(write);
        hookStatusUpdates(write);
        hookStateUpdates(write);

        ModbusWriteThingHandler writeThingHandler = Mockito.mock(ModbusWriteThingHandler.class);
        doReturn(write).when(writeThingHandler).getThing();
        write.setHandler(writeThingHandler);
        return writeThingHandler;
    }

    private ModbusReadThingHandler createReadHandler(String id, Bridge bridge)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, id);
        ThingBuilder builder = ThingBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_WRITE, thingUID)
                .withLabel("label for " + id);
        for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
        }

        Thing read = builder.withBridge(bridge.getUID()).build();
        registerThingToMockRegistry(read);
        hookStatusUpdates(read);
        hookStateUpdates(read);

        ModbusReadThingHandler readThingHandler = Mockito.mock(ModbusReadThingHandler.class);
        doReturn(read).when(readThingHandler).getThing();
        read.setHandler(readThingHandler);
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

        Configuration tcpConfig = new Configuration();
        tcpConfig.put("host", "thisishost");
        tcpConfig.put("port", 44);
        tcpConfig.put("id", 9);
        endpoint = createTcpThingBuilder("tcpendpoint").withConfiguration(tcpConfig).build();
        tcpThingHandler = new ModbusTcpThingHandler(endpoint, () -> modbusManager);
        tcpThingHandler.setCallback(thingCallback);
        endpoint.setHandler(tcpThingHandler);
        registerThingToMockRegistry(endpoint);
        tcpThingHandler.initialize();

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L); // 0 -> non polling
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 9);
        pollerConfig.put("type", ModbusBitUtilities.VALUE_TYPE_INT16);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        registerThingToMockRegistry(poller);
        hookStatusUpdates(poller);
        hookStateUpdates(poller);

        ModbusPollerThingHandlerImpl pollerThingHandler = new ModbusPollerThingHandlerImpl(poller, () -> modbusManager);
        pollerThingHandler.setCallback(thingCallback);
        poller.setHandler(pollerThingHandler);
        pollerThingHandler.initialize();
        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

    @Test
    public void testCommandToNumberChannel()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Command command = DecimalType.ZERO;
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());

        readwriteThingHandler.handleCommand(
                readwriteThingHandler.getThing().getChannel(ModbusBindingConstants.CHANNEL_NUMBER).getUID(), command);

        Thing writeThing = writeHandler.getThing();
        verify(writeHandler).handleCommand(new ChannelUID(writeThing.getUID(), ModbusBindingConstants.CHANNEL_NUMBER),
                command);

        Thing writeThing2 = write2Handler.getThing();
        verify(write2Handler).handleCommand(new ChannelUID(writeThing2.getUID(), ModbusBindingConstants.CHANNEL_NUMBER),
                command);

        verify(write3Handler, never()).handleCommand(any(), any());
    }

    @Test
    public void testOnBitsEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        BitArray bits = Mockito.mock(BitArray.class);
        doReturn(Optional.empty()).when(readHandler).getLastState();

        readwriteThingHandler.onBits(request, bits);

        verify(readHandler).onBits(request, bits);

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testOnBitsNonEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        BitArray bits = Mockito.mock(BitArray.class);
        doReturn(
                Optional.of(ImmutableMap.<ChannelUID, State> builder()
                        .put(new ChannelUID(readHandler.getThing().getUID(),
                                ModbusBindingConstants.CHANNEL_ROLLERSHUTTER), UpDownType.DOWN)
                        .put(new ChannelUID(readHandler.getThing().getUID(), ModbusBindingConstants.CHANNEL_STRING),
                                new StringType("foobar"))
                        .build())).when(readHandler).getLastState();

        readwriteThingHandler.onBits(request, bits);

        verify(readHandler).onBits(request, bits);

        // In this state updates happened only in ReadWrite due to the fact that read handler is mocked and does not
        // update its channels
        assertThat(stateUpdates.size(), is(equalTo(2)));
        assertThat(stateUpdates
                .get(new ChannelUID(readwriteThingHandler.getThing().getUID(), ModbusBindingConstants.CHANNEL_STRING)),
                is(equalTo(Arrays.asList(new StringType("foobar")))));
        assertThat(stateUpdates.get(new ChannelUID(readwriteThingHandler.getThing().getUID(),
                ModbusBindingConstants.CHANNEL_ROLLERSHUTTER)), is(equalTo(Arrays.asList(UpDownType.DOWN))));

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testOnRegistersEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        doReturn(Optional.empty()).when(readHandler).getLastState();

        readwriteThingHandler.onRegisters(request, registers);

        verify(readHandler).onRegisters(request, registers);

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testOnRegistersNonEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        doReturn(
                Optional.of(ImmutableMap.<ChannelUID, State> builder()
                        .put(new ChannelUID(readHandler.getThing().getUID(),
                                ModbusBindingConstants.CHANNEL_ROLLERSHUTTER), UpDownType.DOWN)
                        .put(new ChannelUID(readHandler.getThing().getUID(), ModbusBindingConstants.CHANNEL_STRING),
                                new StringType("foobar"))
                        .build())).when(readHandler).getLastState();

        readwriteThingHandler.onRegisters(request, registers);

        verify(readHandler).onRegisters(request, registers);

        // In this state updates happened only in ReadWrite due to the fact that read handler is mocked and does not
        // update its channels
        assertThat(stateUpdates.size(), is(equalTo(2)));
        assertThat(stateUpdates
                .get(new ChannelUID(readwriteThingHandler.getThing().getUID(), ModbusBindingConstants.CHANNEL_STRING)),
                is(equalTo(Arrays.asList(new StringType("foobar")))));
        assertThat(stateUpdates.get(new ChannelUID(readwriteThingHandler.getThing().getUID(),
                ModbusBindingConstants.CHANNEL_ROLLERSHUTTER)), is(equalTo(Arrays.asList(UpDownType.DOWN))));

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testOnErrorEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        Exception error = Mockito.mock(Exception.class);
        doReturn(Optional.empty()).when(readHandler).getLastState();

        readwriteThingHandler.onError(request, error);

        verify(readHandler).onError(request, error);

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testOnErrorNonEmptyStateFromReadHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1");
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());
        ModbusWriteThingHandler write2Handler = createWriteHandler("write2", readwriteThingHandler.getThing());
        ModbusReadThingHandler readHandler = createReadHandler("read1", readwriteThingHandler.getThing());

        ModbusReadWriteThingHandler readwriteThing2Handler = createInitializedReadWriteWithHandler("readwrite2");
        ModbusWriteThingHandler write3Handler = createWriteHandler("write3", readwriteThing2Handler.getThing());
        ModbusReadThingHandler read2Handler = createReadHandler("read2", readwriteThing2Handler.getThing());

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        Exception error = Mockito.mock(Exception.class);
        doReturn(
                Optional.of(ImmutableMap.<ChannelUID, State> builder()
                        .put(new ChannelUID(readHandler.getThing().getUID(),
                                ModbusBindingConstants.CHANNEL_ROLLERSHUTTER), UpDownType.DOWN)
                        .put(new ChannelUID(readHandler.getThing().getUID(), ModbusBindingConstants.CHANNEL_STRING),
                                new StringType("foobar"))
                        .build())).when(readHandler).getLastState();

        readwriteThingHandler.onError(request, error);

        verify(readHandler).onError(request, error);

        // Errors do not trigger state updates
        assertThat(stateUpdates.size(), is(equalTo(0)));

        verify(writeHandler, never()).handleCommand(any(), any());
        verify(write2Handler, never()).handleCommand(any(), any());
        verify(write3Handler, never()).handleCommand(any(), any());
        verify(read2Handler, never()).onBits(any(), any());
    }

    @Test
    public void testInitializeWithNoBridge()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1", null,
                null);

        assertThat(readwriteThingHandler.getThing().getStatus(), is(equalTo(ThingStatus.OFFLINE)));
        assertThat(readwriteThingHandler.getThing().getStatusInfo().getStatusDetail(),
                is(equalTo(ThingStatusDetail.BRIDGE_OFFLINE)));
    }

    @Test
    public void testInitializeWithOfflineBridge()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler("readwrite1",
                poller.getUID(), handler -> poller
                        .setStatusInfo(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "")));

        assertThat(readwriteThingHandler.getThing().getStatus(), is(equalTo(ThingStatus.OFFLINE)));
        assertThat(readwriteThingHandler.getThing().getStatusInfo().getStatusDetail(),
                is(equalTo(ThingStatusDetail.BRIDGE_OFFLINE)));
    }
}
