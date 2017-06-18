package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandlerImpl;
import org.openhab.binding.modbus.handler.ModbusReadWriteThingHandler;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.binding.modbus.handler.ModbusWriteThingHandler;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ModbusReadWriteThingHandlerTest {

    @Mock
    private ModbusManager modbusManager;

    @Mock
    private ThingRegistry thingRegistry;

    private ItemChannelLinkRegistry linkRegistry = new ItemChannelLinkRegistry();

    private ModbusTcpThingHandler tcpThingHandler;
    private Bridge endpoint;
    private Bridge poller;
    private List<Thing> things = new ArrayList<>();

    @Mock
    private ThingHandlerCallback thingCallback;

    private Map<String, String> channelToAcceptedType = ImmutableMap.<String, String> builder()
            .put(ModbusBindingConstants.CHANNEL_SWITCH, "Switch").put(ModbusBindingConstants.CHANNEL_CONTACT, "Contact")
            .put(ModbusBindingConstants.CHANNEL_DATETIME, "DateTime")
            .put(ModbusBindingConstants.CHANNEL_DIMMER, "Dimmer").put(ModbusBindingConstants.CHANNEL_NUMBER, "Number")
            .put(ModbusBindingConstants.CHANNEL_STRING, "String")
            .put(ModbusBindingConstants.CHANNEL_ROLLERSHUTTER, "Rollershutter").build();

    //
    //
    // el id="switch" typeId="switch-type" />
    // <channel id="contact" typeId="contact-type" />
    // <channel id="datetime" typeId="datetime-type" />
    // <channel id="dimmer" typeId="dimmer-type" />
    // <channel id="number" typeId="number-type" />
    // <channel id="string" typeId="string-type" />
    // <channel id="rollershutter" typeId="rollershutter-type" />

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

    private void hookItemRegistry(ThingHandler thingHandler)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field thingRegisteryField = BaseThingHandler.class.getDeclaredField("thingRegistry");
        thingRegisteryField.setAccessible(true);
        thingRegisteryField.set(thingHandler, thingRegistry);
    }

    private void hookLinkRegistry(ThingHandler thingHandler)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field linkRegistryField = BaseThingHandler.class.getDeclaredField("linkRegistry");
        linkRegistryField.setAccessible(true);
        linkRegistryField.set(thingHandler, linkRegistry);
    }

    private ModbusReadWriteThingHandler createInitializedReadWriteWithHandler()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        ThingUID thingUID = new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, "readwrite1");
        BridgeBuilder builder = BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, thingUID)
                .withLabel("label for readwrite");
        for (Entry<String, String> entry : channelToAcceptedType.entrySet()) {
            String channelId = entry.getKey();
            String channelAcceptedType = entry.getValue();
            builder = builder.withChannel(new Channel(new ChannelUID(thingUID, channelId), channelAcceptedType));
        }
        Bridge readwrite = builder.withBridge(poller.getUID()).build();
        registerThingToMockRegistry(readwrite);
        hookStatusUpdates(readwrite);

        ModbusReadWriteThingHandler readwriteThingHandler = new ModbusReadWriteThingHandler(readwrite);
        hookItemRegistry(readwriteThingHandler);
        hookLinkRegistry(readwriteThingHandler);
        readwriteThingHandler.setCallback(thingCallback);
        readwrite.setHandler(readwriteThingHandler);
        readwriteThingHandler.initialize();
        assertThat(readwrite.getStatus(), is(equalTo(ThingStatus.ONLINE)));
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

        ModbusWriteThingHandler writeThingHandler = Mockito.mock(ModbusWriteThingHandler.class);
        doReturn(write).when(writeThingHandler).getThing();
        // ModbusWriteThingHandler writeThingHandler = new ModbusWriteThingHandler(write);
        // hookItemRegistry(writeThingHandler);
        // hookLinkRegistry(writeThingHandler);
        // writeThingHandler.setCallback(thingCallback);
        write.setHandler(writeThingHandler);
        // writeThingHandler.initialize();
        // assertThat(write.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        return writeThingHandler;
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
        ModbusReadWriteThingHandler readwriteThingHandler = createInitializedReadWriteWithHandler();
        ModbusWriteThingHandler writeHandler = createWriteHandler("write1", readwriteThingHandler.getThing());

        readwriteThingHandler.handleCommand(
                readwriteThingHandler.getThing().getChannel(ModbusBindingConstants.CHANNEL_NUMBER).getUID(), command);

        Thing writeThing = writeHandler.getThing();
        verify(writeHandler).handleCommand(new ChannelUID(writeThing.getUID(), ModbusBindingConstants.CHANNEL_NUMBER),
                command);
    }

}
