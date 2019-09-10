/**
 * Copyright (c) 2014,2019 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.fmiweather.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.fmiweather.internal.client.Client;
import org.openhab.binding.fmiweather.internal.client.Data;
import org.openhab.binding.fmiweather.internal.client.FMIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractWeatherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractWeatherHandler extends BaseThingHandler {

    private static final ZoneId UTC = ZoneId.of("UTC");
    protected static final String PROP_LONGITUDE = "longitude";
    protected static final String PROP_LATITUDE = "latitude";
    protected static final String PROP_NAME = "name";
    protected static final String PROP_REGION = "region";

    protected static int TIMEOUT_MILLIS = 30_000;
    private final Logger logger = LoggerFactory.getLogger(AbstractWeatherHandler.class);

    protected @NonNullByDefault({}) Client client;
    private @Nullable ScheduledFuture<?> future;
    protected @Nullable FMIResponse response;
    protected int pollIntervalSeconds = 120;

    public AbstractWeatherHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH == command) {
            logger.debug("REFRESH received. Updating channels from last response if possible");
            updateChannels();
        }
    }

    @Override
    public void initialize() {
        client = new Client();
        updateStatus(ThingStatus.UNKNOWN);
        future = scheduler.scheduleWithFixedDelay(this::update, 0, pollIntervalSeconds, TimeUnit.SECONDS);

    }

    abstract protected void update();

    abstract protected void updateChannels();

    @Override
    public void dispose() {
        super.dispose();
        response = null;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    protected int lastValidIndex(Data data) {
        if (data.values.length < 2) {
            throw new IllegalStateException("Excepted at least two data items");
        }
        if (data.values[0] == null) {
            return -1;
        }
        for (int i = 1; i < data.values.length; i++) {
            if (data.values[i] == null) {
                return i - 1;
            }
        }
        if (data.values[data.values.length - 1] == null) {
            return -1;
        }
        return data.values.length - 1;

    }

    protected long floorToEvenMinutes(long epochSeconds, int roundMinutes) {
        long roundSecs = roundMinutes * 60;
        return (epochSeconds / roundSecs) * roundSecs;
    }

    protected long ceilToEvenMinutes(long epochSeconds, int roundMinutes) {
        double epochDouble = epochSeconds;
        long roundSecs = roundMinutes * 60;
        double roundSecsDouble = (roundMinutes * 60);
        return (long) Math.ceil(epochDouble / roundSecsDouble) * roundSecs;
    }

    /**
     * Update QuantityType channel state
     *
     * @param channelUID  channel UID
     * @param epochSecond value to update
     * @param unit        unit associated with the value
     */
    protected <T extends Quantity<T>> void updateEpochSecondStateIfLinked(ChannelUID channelUID, long epochSecond) {
        if (isLinked(channelUID)) {
            updateState(channelUID, new DateTimeType(ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), UTC)
                    .withZoneSameInstant(ZoneId.systemDefault())));
        }
    }

    /**
     * Update QuantityType or DecimalType channel state
     *
     * Updates UNDEF state when value is null
     *
     * @param channelUID channel UID
     * @param value      value to update
     * @param unit       unit associated with the value
     */
    protected void updateStateIfLinked(ChannelUID channelUID, @Nullable BigDecimal value, @Nullable Unit<?> unit) {
        if (isLinked(channelUID)) {
            if (value == null) {
                updateState(channelUID, UnDefType.UNDEF);
            } else if (unit == null) {
                updateState(channelUID, new DecimalType(value));
            } else {
                updateState(channelUID, new QuantityType<>(value, unit));
            }
        }
    }

    /**
     * Unwrap optional value and log with ERROR if value is not present
     *
     * @param optional            optional to unwrap
     * @param messageIfNotPresent logging message
     * @param args                arguments to logging
     * @throws IllegalStateException when value is not present
     * @return unwrapped value of the optional
     */
    protected <T> T unwrap(Optional<T> optional, String messageIfNotPresent, Object... args) {
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.error(messageIfNotPresent, args);
            throw new IllegalStateException("unwrapping");
        }
    }

}
