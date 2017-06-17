package org.openhab.binding.modbus;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.eclipse.smarthome.core.thing.internal.BridgeImpl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandlerImpl;
import org.openhab.binding.modbus.handler.ModbusReadWriteThingHandler;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.binding.modbus.internal.ModbusManagerReference;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusManager;
import org.openhab.io.transport.modbus.ModbusManager.PollTask;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;

@RunWith(MockitoJUnitRunner.class)
public class ModbusPollerThingHandlerTest implements ModbusManagerReference {

    @Mock
    private ModbusManager modbusManager;

    @Mock
    private ThingRegistry thingRegistry;

    private Bridge endpoint;
    private Bridge poller;
    private List<Thing> things = new ArrayList<>();

    private ModbusTcpThingHandler tcpThingHandler;
    private ThingHandlerCallback thingCallback;

    private static BridgeBuilder createTcpThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_TCP,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_TCP, id)).withLabel("label for " + id);
    }

    private static BridgeBuilder createPollerThingBuilder(String id) {
        return BridgeBuilder.create(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER,
                new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_POLLER, id)).withLabel("label for " + id);
    }

    private static BridgeBuilder createReadWriteThingBuilder(String id) {
        return BridgeBuilder
                .create(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE,
                        new ThingUID(ModbusBindingConstants.THING_TYPE_MODBUS_READ_WRITE, id))
                .withLabel("label for " + id);
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

    /**
     * Before each test, setup TCP endpoint thing, configure mocked item registry
     */
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

        thingCallback = Mockito.mock(ThingHandlerCallback.class);
        hookStatusUpdates(endpoint);

        tcpThingHandler = new ModbusTcpThingHandler(endpoint, this);
        tcpThingHandler.setCallback(thingCallback);
        endpoint.setHandler(tcpThingHandler);
        registerThingToMockRegistry(endpoint);
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

    private void hookStatusUpdates(Thing thing) {
        Mockito.doAnswer(invocation -> {
            thing.setStatusInfo(invocation.getArgumentAt(1, ThingStatusInfo.class));
            return null;
        }).when(thingCallback).statusUpdated(Matchers.same(thing), Matchers.any());
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
        registerThingToMockRegistry(poller);
        hookStatusUpdates(poller);

        ModbusPollerThingHandlerImpl pollerThingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        pollerThingHandler.setCallback(thingCallback);
        poller.setHandler(pollerThingHandler);
        pollerThingHandler.initialize();
        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        // polling not setup
        verifyZeroInteractions(modbusManager);
    }

    public void testPollingGeneric(String type, Supplier<Matcher<PollTask>> pollTaskMatcherSupplier)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", type);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        registerThingToMockRegistry(poller);

        hookStatusUpdates(poller);

        ModbusPollerThingHandlerImpl thingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        thingHandler.setCallback(thingCallback);
        poller.setHandler(thingHandler);
        hookItemRegistry(thingHandler);

        thingHandler.initialize();
        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        // polling not setup
        verify(modbusManager).registerRegularPoll(argThat(pollTaskMatcherSupplier.get()), eq(150l), eq(0L));
        verifyNoMoreInteractions(modbusManager);
    }

    private boolean checkPollTask(PollTask item, ModbusReadFunctionCode functionCode) {
        return item.getEndpoint().equals(tcpThingHandler.asSlaveEndpoint()) && item.getRequest().getDataLength() == 13
                && item.getRequest().getFunctionCode() == functionCode && item.getRequest().getProtocolID() == 0
                && item.getRequest().getReference() == 5 && item.getRequest().getUnitID() == 9;
    }

    Matcher<PollTask> isRequestOkGeneric(ModbusReadFunctionCode functionCode) {
        return new TypeSafeMatcher<PollTask>() {
            @Override
            public boolean matchesSafely(PollTask item) {
                return checkPollTask(item, functionCode);
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

    @Test
    public void testReadCallback()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        registerThingToMockRegistry(poller);
        hookStatusUpdates(poller);

        ModbusPollerThingHandlerImpl thingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        thingHandler.setCallback(thingCallback);
        poller.setHandler(thingHandler);
        hookItemRegistry(thingHandler);

        Bridge readwrite1 = createReadWriteThingBuilder("readwriter1").withBridge(poller.getUID()).build();
        ModbusReadWriteThingHandler readwrite1Handler = Mockito.mock(ModbusReadWriteThingHandler.class);
        readwrite1.setHandler(readwrite1Handler);
        registerThingToMockRegistry(readwrite1);
        hookStatusUpdates(readwrite1);

        Bridge readwrite2 = createReadWriteThingBuilder("readwriter2").withBridge(poller.getUID()).build();
        ModbusReadWriteThingHandler readwrite2Handler = Mockito.mock(ModbusReadWriteThingHandler.class);
        readwrite2.setHandler(readwrite2Handler);
        registerThingToMockRegistry(readwrite2);
        hookStatusUpdates(readwrite2);

        thingHandler.initialize();

        final AtomicReference<ModbusReadCallback> callbackRef = new AtomicReference<>();
        verify(modbusManager).registerRegularPoll(argThat(new TypeSafeMatcher<PollTask>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(PollTask item) {
                callbackRef.set(item.getCallback());
                return checkPollTask(item, ModbusReadFunctionCode.READ_COILS);

            }
        }), eq(150l), eq(0L));

        verifyNoMoreInteractions(modbusManager);
        verifyNoMoreInteractions(readwrite1Handler);
        verifyNoMoreInteractions(readwrite2Handler);

        ModbusReadCallback callback = callbackRef.get();
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        BitArray bits = Mockito.mock(BitArray.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        Exception error = Mockito.mock(Exception.class);

        // verify that each callback is propagated to readwrite items
        callback.onBits(request, bits);
        verify(readwrite1Handler).onBits(request, bits);
        verify(readwrite2Handler).onBits(request, bits);
        verifyNoMoreInteractions(readwrite1Handler);
        verifyNoMoreInteractions(readwrite2Handler);

        callback.onRegisters(request, registers);
        verify(readwrite1Handler).onRegisters(request, registers);
        verify(readwrite2Handler).onRegisters(request, registers);
        verifyNoMoreInteractions(readwrite1Handler);
        verifyNoMoreInteractions(readwrite2Handler);

        callback.onError(request, error);
        verify(readwrite1Handler).onError(request, error);
        verify(readwrite2Handler).onError(request, error);
        verifyNoMoreInteractions(readwrite1Handler);
        verifyNoMoreInteractions(readwrite2Handler);
    }

    @Test
    public void testDisconnectReconnectOnBridgeStatusChange()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        registerThingToMockRegistry(poller);
        hookStatusUpdates(poller);

        ModbusPollerThingHandlerImpl thingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        thingHandler.setCallback(thingCallback);
        poller.setHandler(thingHandler);
        hookItemRegistry(thingHandler);

        thingHandler.initialize();

        // verify registration
        final AtomicReference<ModbusReadCallback> callbackRef = new AtomicReference<>();
        Consumer<InOrder> verifyRegistration = (InOrder order) -> {
            ModbusManager verifier;
            if (order == null) {
                verifier = verify(modbusManager);
            } else {
                verifier = order.verify(modbusManager);
            }
            verifier.registerRegularPoll(argThat(new TypeSafeMatcher<PollTask>() {

                @Override
                public void describeTo(Description description) {
                }

                @Override
                protected boolean matchesSafely(PollTask item) {
                    callbackRef.set(item.getCallback());
                    return checkPollTask(item, ModbusReadFunctionCode.READ_COILS);

                }
            }), eq(150l), eq(0L));
        };
        verifyRegistration.accept(null);
        verifyNoMoreInteractions(modbusManager);

        // reset call counts for easy assertions
        reset(modbusManager);

        // bridge status changed
        thingHandler.bridgeStatusChanged(Mockito.mock(ThingStatusInfo.class));

        InOrder orderedVerify = Mockito.inOrder(modbusManager);

        // 1) should first unregister poll task
        orderedVerify.verify(modbusManager).unregisterRegularPoll(argThat(new TypeSafeMatcher<PollTask>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(PollTask item) {
                assertThat(item.getCallback(), is(sameInstance(callbackRef.get())));
                return checkPollTask(item, ModbusReadFunctionCode.READ_COILS);

            }
        }));

        // 2) then register new
        verifyRegistration.accept(orderedVerify);

        verifyNoMoreInteractions(modbusManager);

    }

    @Test
    public void testDisconnectOnDispose()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        registerThingToMockRegistry(poller);
        hookStatusUpdates(poller);

        ModbusPollerThingHandlerImpl thingHandler = new ModbusPollerThingHandlerImpl(poller, this);
        thingHandler.setCallback(thingCallback);
        poller.setHandler(thingHandler);
        hookItemRegistry(thingHandler);

        thingHandler.initialize();

        // verify registration
        final AtomicReference<ModbusReadCallback> callbackRef = new AtomicReference<>();
        verify(modbusManager).registerRegularPoll(argThat(new TypeSafeMatcher<PollTask>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(PollTask item) {
                callbackRef.set(item.getCallback());
                return checkPollTask(item, ModbusReadFunctionCode.READ_COILS);

            }
        }), eq(150l), eq(0L));
        verifyNoMoreInteractions(modbusManager);

        // reset call counts for easy assertions
        reset(modbusManager);

        thingHandler.dispose();

        // 1) should first unregister poll task
        verify(modbusManager).unregisterRegularPoll(argThat(new TypeSafeMatcher<PollTask>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(PollTask item) {
                assertThat(item.getCallback(), is(sameInstance(callbackRef.get())));
                return checkPollTask(item, ModbusReadFunctionCode.READ_COILS);

            }
        }));

        verifyNoMoreInteractions(modbusManager);

    }

    @Override
    public ModbusManager getManager() {
        return modbusManager;
    }

}
