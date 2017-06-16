package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandlerImpl;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;

@RunWith(MockitoJUnitRunner.class)
public class ModbusPollerThingHandlerTest implements ModbusManagerReference {

    @Mock
    private ModbusManager modbusManager;

    @Mock
    private ThingRegistry thingRegistry;

    private Bridge endpoint;
    private Bridge poller;
    private ThingHandlerCallback thingCallback;

    private ModbusTcpThingHandler tcpThingHandler;

    private static BridgeBuilder createTcpThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_TCP,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_TCP, id)).withLabel("label for " + id);
    }

    private static BridgeBuilder createPollerThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER, id)).withLabel("label for " + id);
    }

    @Before
    public void setUp() {
        Mockito.when(thingRegistry.get(Matchers.any())).then(invocation -> {
            ThingUID uid = invocation.getArgumentAt(0, ThingUID.class);
            if (uid.equals(endpoint.getUID())) {
                return endpoint;
            } else if (uid.equals(poller.getUID())) {
                return poller;
            } else {
                throw new IllegalArgumentException("UID is unknown: " + uid.getAsString());
            }
        });

        Configuration tcpConfig = new Configuration();
        tcpConfig.put("host", "thisishost");
        tcpConfig.put("port", 44);
        tcpConfig.put("id", 9);
        endpoint = createTcpThingBuilder("tcpendpoint").withConfiguration(tcpConfig).build();

        thingCallback = Mockito.mock(ThingHandlerCallback.class);
        Mockito.doAnswer(invocation -> {
            endpoint.setStatusInfo(invocation.getArgumentAt(1, ThingStatusInfo.class));
            return null;
        }).when(thingCallback).statusUpdated(Matchers.same(endpoint), Matchers.any());

        tcpThingHandler = new ModbusTcpThingHandler(endpoint, this);
        tcpThingHandler.setCallback(thingCallback);
        endpoint.setHandler(tcpThingHandler);
        tcpThingHandler.initialize();

        assertThat(endpoint.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        // no need to test endpoint otherwise, see other unit tests
    }

    private void hookItemRegistry(ThingHandler thingHandler)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field thingRegisteryField = BaseThingHandler.class.getDeclaredField("thingRegistry");
        thingRegisteryField.setAccessible(true);
        thingRegisteryField.set(thingHandler, thingRegistry);
    }

    @Test
    public void testInitializeNonPolling() {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L); // 0 -> non polling
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 9);
        pollerConfig.put("type", ModbusBitUtilities.VALUE_TYPE_INT16);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();

        Mockito.doAnswer(invocation -> {
            poller.setStatusInfo(invocation.getArgumentAt(1, ThingStatusInfo.class));
            return null;
        }).when(thingCallback).statusUpdated(Matchers.same(poller), Matchers.any());

        ModbusPollerThingHandlerImpl pollerThingHandler = new ModbusPollerThingHandlerImpl(endpoint, this);
        pollerThingHandler.setCallback(thingCallback);
        poller.setHandler(pollerThingHandler);
        pollerThingHandler.initialize();

        // polling not setup
        verifyZeroInteractions(modbusManager);
    }

    public void testPollingGeneric(String type, Supplier<Matcher<PollTask>> pollTaskMatcherSupplier)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L); // 0 -> non polling
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", type);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();

        Mockito.doAnswer(invocation -> {
            poller.setStatusInfo(invocation.getArgumentAt(1, ThingStatusInfo.class));
            return null;
        }).when(thingCallback).statusUpdated(Matchers.same(poller), Matchers.any());

        ModbusPollerThingHandlerImpl thingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        thingHandler.setCallback(thingCallback);
        poller.setHandler(thingHandler);
        hookItemRegistry(thingHandler);

        thingHandler.initialize();

        // polling not setup
        verify(modbusManager).registerRegularPoll(argThat(pollTaskMatcherSupplier.get()), eq(150l), eq(0L));
        verifyNoMoreInteractions(modbusManager);
    }

    Matcher<PollTask> isRequestOkGeneric(ModbusReadFunctionCode functionCode) {
        return new TypeSafeMatcher<PollTask>() {
            @Override
            public boolean matchesSafely(PollTask item) {
                return item.getEndpoint().equals(tcpThingHandler.asSlaveEndpoint())
                        && item.getRequest().getDataLength() == 13
                        && item.getRequest().getFunctionCode() == functionCode && item.getRequest().getProtocolID() == 0
                        && item.getRequest().getReference() == 5 && item.getRequest().getUnitID() == 9;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    Matcher<PollTask> okCoilRequest() {
        return new TypeSafeMatcher<PollTask>() {
            @Override
            public boolean matchesSafely(PollTask item) {
                // we are not testing the callback at all!
                return item.getEndpoint().equals(tcpThingHandler.asSlaveEndpoint())
                        && item.getRequest().getDataLength() == 13
                        && item.getRequest().getFunctionCode() == ModbusReadFunctionCode.READ_COILS
                        && item.getRequest().getProtocolID() == 0 && item.getRequest().getReference() == 5
                        && item.getRequest().getUnitID() == 9;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    @Test
    public void testInitializePollingWithCoils()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("coil", () -> isRequestOkGeneric(ModbusReadFunctionCode.READ_COILS));
    }

    @Test
    public void testInitializePollingWithDiscrete()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("discrete", () -> isRequestOkGeneric(ModbusReadFunctionCode.READ_INPUT_DISCRETES));
    }

    @Test
    public void testInitializePollingWithInputRegisters()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("input", () -> isRequestOkGeneric(ModbusReadFunctionCode.READ_INPUT_REGISTERS));
    }

    @Test
    public void testInitializePollingWithHoldingRegisters()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("holding", () -> isRequestOkGeneric(ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS));
    }

    // TODO: have child items and verify callback go through nicely

    @Override
    public ModbusManager getManager() {
        return modbusManager;
    }

}
