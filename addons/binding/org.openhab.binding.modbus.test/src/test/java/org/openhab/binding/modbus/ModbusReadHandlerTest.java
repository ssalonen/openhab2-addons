package org.openhab.binding.modbus;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openhab.binding.modbus.handler.ModbusReadThingHandler;

import com.google.common.collect.ImmutableMap;

public class ModbusReadHandlerTest {
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

    private ModbusReadThingHandler createReadHandler(String id, Bridge bridge,
            Consumer<ThingBuilder> builderConfigurator)
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

    @Test
    public void testOnBits() {

    }

    @Test
    public void testOnRegistersSpecificTriggerMatchingAndTransformation() {
        // create (mock?) bridge
        // createReadHandler, with configuration
        // createReadHandler("read1")
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
