/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.modbus.tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.thing.binding.builder.BridgeBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandler;
import org.openhab.binding.modbus.internal.ModbusBindingConstantsInternal;
import org.openhab.binding.modbus.internal.handler.ModbusDataThingHandler;
import org.openhab.binding.modbus.internal.handler.ModbusTcpThingHandler;
import org.openhab.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusReadCallback;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.PollTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sami Salonen - Initial contribution
 */
@RunWith(MockitoJUnitRunner.class)
public class ModbusPollerThingHandlerTest extends AbstractModbusOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(ModbusPollerThingHandlerTest.class);

    private Bridge endpoint;
    private Bridge poller;

    @Mock
    private ThingHandlerCallback thingCallback;

    public static BridgeBuilder createTcpThingBuilder(String id) {
        return BridgeBuilder
                .create(ModbusBindingConstantsInternal.THING_TYPE_MODBUS_TCP,
                        new ThingUID(ModbusBindingConstantsInternal.THING_TYPE_MODBUS_TCP, id))
                .withLabel("label for " + id);
    }

    public static BridgeBuilder createPollerThingBuilder(String id) {
        return BridgeBuilder
                .create(ModbusBindingConstantsInternal.THING_TYPE_MODBUS_POLLER,
                        new ThingUID(ModbusBindingConstantsInternal.THING_TYPE_MODBUS_POLLER, id))
                .withLabel("label for " + id);
    }

    /**
     * Verify that basic poller <-> endpoint interaction has taken place (on poller init)
     */
    private void verifyEndpointBasicInitInteraction() {
        verify(mockedModbusManager, times(1)).addListener(any());
        verify(mockedModbusManager, times(1)).setEndpointPoolConfiguration(any(), any());
    }

    public ModbusReadCallback getPollerCallback(ModbusPollerThingHandler handler)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field callbackField = ModbusPollerThingHandler.class.getDeclaredField("callbackDelegator");
        callbackField.setAccessible(true);
        return (ModbusReadCallback) callbackField.get(handler);
    }

    /**
     * Before each test, setup TCP endpoint thing, configure mocked item registry
     */
    @Before
    public void setUp() {
        Configuration tcpConfig = new Configuration();
        tcpConfig.put("host", "thisishost");
        tcpConfig.put("port", 44);
        tcpConfig.put("id", 9);
        endpoint = createTcpThingBuilder("tcpendpoint").withConfiguration(tcpConfig).build();
        addThing(endpoint);

        assertThat(endpoint.getStatus(), is(equalTo(ThingStatus.ONLINE)));
    }

    @After
    public void tearDown() {
        if (endpoint != null) {
            thingProvider.remove(endpoint.getUID());
        }
        if (poller != null) {
            thingProvider.remove(poller.getUID());
        }
    }

    @Test
    public void testInitializeNonPolling()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L); // 0 -> non polling
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 9);
        pollerConfig.put("type", ModbusBindingConstantsInternal.READ_TYPE_HOLDING_REGISTER);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();

        logger.info("Poller created, registering to registry...");
        addThing(poller);
        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));
        logger.info("Poller registered");

        verifyEndpointBasicInitInteraction();
        // polling is _not_ setup
        verifyNoMoreInteractions(mockedModbusManager);
    }

    public void testPollingGeneric(String type, ModbusReadFunctionCode expectedFunctionCode)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        PollTask pollTask = Mockito.mock(PollTask.class);
        doReturn(pollTask).when(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                notNull());

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", type);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verifyEndpointBasicInitInteraction();
        verify(mockedModbusManager).registerRegularPoll(argThat(new TypeSafeMatcher<ModbusSlaveEndpoint>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("correct endpoint");
            }

            @Override
            protected boolean matchesSafely(ModbusSlaveEndpoint endpoint) {
                return checkEndpoint(endpoint);
            }
        }), argThat(new TypeSafeMatcher<ModbusReadRequestBlueprint>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("correct request");
            }

            @Override
            protected boolean matchesSafely(ModbusReadRequestBlueprint request) {
                return checkRequest(request, expectedFunctionCode);
            }
        }), eq(150l), eq(0L), notNull());
        verifyNoMoreInteractions(mockedModbusManager);
    }

    private boolean checkEndpoint(ModbusSlaveEndpoint endpointParam) {
        ModbusTcpThingHandler endPointHandler = (ModbusTcpThingHandler) endpoint.getHandler();
        assertNotNull(endPointHandler);
        return endpointParam.equals(endPointHandler.asSlaveEndpoint());
    }

    private boolean checkRequest(ModbusReadRequestBlueprint request, ModbusReadFunctionCode functionCode) {
        return request.getDataLength() == 13 && request.getFunctionCode() == functionCode
                && request.getProtocolID() == 0 && request.getReference() == 5 && request.getUnitID() == 9;
    }

    @Test
    public void testInitializePollingWithCoils()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("coil", ModbusReadFunctionCode.READ_COILS);
    }

    @Test
    public void testInitializePollingWithDiscrete()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("discrete", ModbusReadFunctionCode.READ_INPUT_DISCRETES);
    }

    @Test
    public void testInitializePollingWithInputRegisters()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("input", ModbusReadFunctionCode.READ_INPUT_REGISTERS);
    }

    @Test
    public void testInitializePollingWithHoldingRegisters()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testPollingGeneric("holding", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS);
    }

    @Test
    public void testPollUnregistrationOnDispose()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        PollTask pollTask = Mockito.mock(PollTask.class);
        doReturn(pollTask).when(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                notNull());

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        // verify registration
        final AtomicReference<ModbusReadCallback> callbackRef = new AtomicReference<>();

        verify(mockedModbusManager).registerRegularPoll(argThat(new TypeSafeMatcher<ModbusSlaveEndpoint>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(ModbusSlaveEndpoint endpoint) {
                return checkEndpoint(endpoint);
            }
        }), argThat(new TypeSafeMatcher<ModbusReadRequestBlueprint>() {

            @Override
            public void describeTo(Description description) {
            }

            @Override
            protected boolean matchesSafely(ModbusReadRequestBlueprint request) {
                return checkRequest(request, ModbusReadFunctionCode.READ_COILS);
            }
        }), eq(150l), eq(0L), argThat(new TypeSafeMatcher<ModbusReadCallback>() {

            @Override
            public void describeTo(Description description) {

            }

            @Override
            protected boolean matchesSafely(ModbusReadCallback callback) {
                callbackRef.set(callback);
                return true;
            }

        }));
        verifyNoMoreInteractions(mockedModbusManager);

        // reset call counts for easy assertions
        reset(mockedModbusManager);

        // remove the thing
        disposeThing(poller);

        // 1) should first unregister poll task
        verify(mockedModbusManager).unregisterRegularPoll(eq(pollTask));

        verifyNoMoreInteractions(mockedModbusManager);
    }

    @Test
    public void testInitializeWithNoBridge()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
        assertThat(poller.getStatusInfo().getStatusDetail(), is(equalTo(ThingStatusDetail.BRIDGE_OFFLINE)));

        verifyNoMoreInteractions(mockedModbusManager);
    }

    @Test
    public void testInitializeWithOfflineBridge()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");

        endpoint.setStatusInfo(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, ""));
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.OFFLINE)));
        assertThat(poller.getStatusInfo().getStatusDetail(), is(equalTo(ThingStatusDetail.BRIDGE_OFFLINE)));

        verifyNoMoreInteractions(mockedModbusManager);
    }

    @Test
    public void testRegistersPassedToChildDataThings()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        PollTask pollTask = Mockito.mock(PollTask.class);
        doReturn(pollTask).when(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                notNull());

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        ArgumentCaptor<ModbusReadCallback> callbackCapturer = ArgumentCaptor.forClass(ModbusReadCallback.class);
        verify(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                callbackCapturer.capture());
        ModbusReadCallback readCallback = callbackCapturer.getValue();

        assertNotNull(readCallback);

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);

        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        ModbusDataThingHandler child2 = Mockito.mock(ModbusDataThingHandler.class);

        AsyncModbusReadResult result = new AsyncModbusReadResult(request, registers);

        // has one data child
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);

        // two children (one child initialized)
        thingHandler.childHandlerInitialized(child2, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);
        reset(child2);

        // one child disposed
        thingHandler.childHandlerDisposed(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);
    }

    @Test
    public void testBitsPassedToChildDataThings()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        PollTask pollTask = Mockito.mock(PollTask.class);
        doReturn(pollTask).when(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                notNull());

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        ArgumentCaptor<ModbusReadCallback> callbackCapturer = ArgumentCaptor.forClass(ModbusReadCallback.class);
        verify(mockedModbusManager).registerRegularPoll(any(), any(), eq(150l), eq(0L), callbackCapturer.capture());
        ModbusReadCallback readCallback = callbackCapturer.getValue();

        assertNotNull(readCallback);

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        BitArray bits = Mockito.mock(BitArray.class);

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);

        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        ModbusDataThingHandler child2 = Mockito.mock(ModbusDataThingHandler.class);

        AsyncModbusReadResult result = new AsyncModbusReadResult(request, bits);

        // has one data child
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);

        // two children (one child initialized)
        thingHandler.childHandlerInitialized(child2, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);
        reset(child2);

        // one child disposed
        thingHandler.childHandlerDisposed(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);
    }

    @Test
    public void testErrorPassedToChildDataThings()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        PollTask pollTask = Mockito.mock(PollTask.class);
        doReturn(pollTask).when(mockedModbusManager).registerRegularPoll(notNull(), notNull(), eq(150l), eq(0L),
                notNull());

        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 150L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        ArgumentCaptor<ModbusReadCallback> callbackCapturer = ArgumentCaptor.forClass(ModbusReadCallback.class);
        verify(mockedModbusManager).registerRegularPoll(any(), any(), eq(150l), eq(0L), callbackCapturer.capture());
        ModbusReadCallback readCallback = callbackCapturer.getValue();

        assertNotNull(readCallback);

        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        Exception error = Mockito.mock(Exception.class);

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);

        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        ModbusDataThingHandler child2 = Mockito.mock(ModbusDataThingHandler.class);

        AsyncModbusReadResult result = new AsyncModbusReadResult(request, error);

        // has one data child
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);

        // two children (one child initialized)
        thingHandler.childHandlerInitialized(child2, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child1).handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);

        reset(child1);
        reset(child2);

        // one child disposed
        thingHandler.childHandlerDisposed(child1, Mockito.mock(Thing.class));
        readCallback.handle(result);
        verify(child2).handle(result);
        verifyNoMoreInteractions(child1);
        verifyNoMoreInteractions(child2);
    }

    @Test
    public void testRefresh()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());
        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);
        thingHandler.refresh();
        verify(mockedModbusManager).submitOneTimePoll(any(), any(), any());
    }

    /**
     * When there's no recently received data, refresh() will re-use that instead
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @Test
    public void testRefreshWithPreviousData()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        pollerConfig.put("cacheMillis", 10000L);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data is received
        ModbusReadCallback pollerReadCallback = getPollerCallback(thingHandler);
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        AsyncModbusReadResult result = new AsyncModbusReadResult(request, registers);
        pollerReadCallback.handle(result);

        // data child receives the data
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        reset(child1);

        // call refresh
        // cache is still valid, we should not have real data poll this time
        thingHandler.refresh();
        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data child receives the cached data
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
    }

    /**
     * When there's no recently received data, refresh() will re-use that instead
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @Test
    public void testRefreshWithPreviousDataCacheDisabled()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        pollerConfig.put("cacheMillis", 0L);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);
        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data is received
        ModbusReadCallback pollerReadCallback = getPollerCallback(thingHandler);
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        AsyncModbusReadResult result = new AsyncModbusReadResult(request, registers);

        pollerReadCallback.handle(result);

        // data child receives the data
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        reset(child1);

        // call refresh
        // caching disabled, should poll from manager
        thingHandler.refresh();
        verify(mockedModbusManager).submitOneTimePoll(any(), any(), any());
        verifyNoMoreInteractions(mockedModbusManager);

        // data child receives the cached data
        verifyNoMoreInteractions(child1);
    }

    /**
     * Testing again caching, such that most recently received data is propagated to children
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws InterruptedException
     */
    @Test
    public void testRefreshWithPreviousData2() throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, SecurityException, InterruptedException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        pollerConfig.put("cacheMillis", 10000L);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);
        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data is received
        ModbusReadCallback pollerReadCallback = getPollerCallback(thingHandler);
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusReadRequestBlueprint request2 = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        Exception error = Mockito.mock(Exception.class);
        AsyncModbusReadResult registersResult = new AsyncModbusReadResult(request, registers);
        AsyncModbusReadResult errorResult = new AsyncModbusReadResult(request2, error);

        pollerReadCallback.handle(registersResult);

        // data child should receive the data
        verify(child1).handle(registersResult);
        verifyNoMoreInteractions(child1);
        reset(child1);

        // Sleep to have time between the data
        Thread.sleep(5L);

        // error is received
        pollerReadCallback.handle(errorResult);

        // data child should receive the error
        verify(child1).handle(errorResult);
        verifyNoMoreInteractions(child1);
        reset(child1);

        // call refresh, should return latest data (that is, error)
        // cache is still valid, we should not have real data poll this time
        thingHandler.refresh();
        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data child receives the cached error
        verify(child1).handle(errorResult);
        verifyNoMoreInteractions(child1);
    }

    @Test
    public void testRefreshWithOldPreviousData() throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, SecurityException, InterruptedException {
        Configuration pollerConfig = new Configuration();
        pollerConfig.put("refresh", 0L);
        pollerConfig.put("start", 5);
        pollerConfig.put("length", 13);
        pollerConfig.put("type", "coil");
        pollerConfig.put("cacheMillis", 10L);
        poller = createPollerThingBuilder("poller").withConfiguration(pollerConfig).withBridge(endpoint.getUID())
                .build();
        addThing(poller);
        verifyEndpointBasicInitInteraction();

        ModbusPollerThingHandler thingHandler = (ModbusPollerThingHandler) poller.getHandler();
        assertNotNull(thingHandler);
        ModbusDataThingHandler child1 = Mockito.mock(ModbusDataThingHandler.class);
        thingHandler.childHandlerInitialized(child1, Mockito.mock(Thing.class));

        assertThat(poller.getStatus(), is(equalTo(ThingStatus.ONLINE)));

        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());

        // data is received
        ModbusReadCallback pollerReadCallback = getPollerCallback(thingHandler);
        ModbusReadRequestBlueprint request = Mockito.mock(ModbusReadRequestBlueprint.class);
        ModbusRegisterArray registers = Mockito.mock(ModbusRegisterArray.class);
        AsyncModbusReadResult result = new AsyncModbusReadResult(request, registers);

        pollerReadCallback.handle(result);

        // data child should receive the data
        verify(child1).handle(result);
        verifyNoMoreInteractions(child1);
        reset(child1);

        // Sleep to ensure cache expiry
        Thread.sleep(15L);

        // call refresh. Since cache expired, will poll for more
        verify(mockedModbusManager, never()).submitOneTimePoll(any(), any(), any());
        thingHandler.refresh();
        verify(mockedModbusManager).submitOneTimePoll(any(), any(), any());
    }
}
