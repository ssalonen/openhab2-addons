package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.ManagedThingProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingProvider;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.io.transport.modbus.ModbusManager;

public class ModbusTest extends JavaOSGiTest {

    @Mock
    private ModbusManager modbusManager;
    private ModbusTcpThingHandler tmp;
    private ManagedThingProvider managedThingProvider;
    private ThingRegistry thingRegistry;
    private ItemRegistry itemRegistry;
    private Thing thing;

    @Before
    public void setUp() {
        // registerService(modbusManager);

        managedThingProvider = getService(ThingProvider.class, ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        itemRegistry = getService(ItemRegistry.class);
        assertThat(itemRegistry, is(notNullValue()));

    }

    @After
    public void tearDown() {
        if (thing != null) {
            Thing removedThing = thingRegistry.forceRemove(thing.getUID());
            assertThat("The thing was not deleted", removedThing, is(notNullValue()));
        }

        // if (testItem != null) {
        // itemRegistry.remove(TEST_ITEM_NAME);
        // }
    }

    private static ThingBuilder createTcpThingBuilder(String id) {
        return ThingBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_TCP,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_TCP, id));
    }

    @Test
    public void test() {
        Configuration thingConfig = new Configuration();
        thingConfig.put("host", "thisishost");
        thingConfig.put("port", 44);
        thingConfig.put("id", 9);

        thing = createTcpThingBuilder("tcpendpoint").withConfiguration(thingConfig).build();
        assert thing.getStatus() == ThingStatus.ONLINE;
    }

}
