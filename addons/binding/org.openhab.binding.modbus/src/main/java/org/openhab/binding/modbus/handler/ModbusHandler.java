/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.handler;

import static org.openhab.binding.modbus.ModbusBindingConstants.CHANNEL_STRING;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(ModbusHandler.class);
    private ChannelUID stringChannelUid;
    private ScheduledFuture<?> refreshJob;
    private String readDefinitions;

    public ModbusHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_STRING)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        Configuration config = getThing().getConfiguration();
        readDefinitions = (String) config.get("readDefinitions");
        if (readDefinitions == null) {
            return;
        }
        // hostname = (String) config.get(PROPERTY_NTP_SERVER);
        // refreshInterval = (BigDecimal) config.get(PROPERTY_REFRESH_INTERVAL);
        // refreshNtp = (BigDecimal) config.get(PROPERTY_REFRESH_NTP);
        List<String> readThings = Stream.of(readDefinitions.split(",")).map(String::trim).collect(Collectors.toList());
        for (String readThing : readThings) {
            logger.info("modbus read " + readThing);
            logger.info("modbus got thing: " + this.thingRegistry.get(new ThingUID(readThing)).toString());
        }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        stringChannelUid = new ChannelUID(getThing().getUID(), CHANNEL_STRING);

        refreshJob = scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                List<String> readThings = Stream.of(readDefinitions.split(",")).map(String::trim)
                        .collect(Collectors.toList());
                for (String readThing : readThings) {
                    Thing thing = thingRegistry.get(new ThingUID(readThing));
                    BigDecimal start = (BigDecimal) thing.getConfiguration().get("start");
                    logger.info("read thing {} has start={}", thing.getUID().getId(), start);
                }
                updateState(stringChannelUid, new StringType("state"));
            }
        }, 0, 1, TimeUnit.SECONDS);

    }
}
