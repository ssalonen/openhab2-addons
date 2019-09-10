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
package org.openhab.binding.fmiweather;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.openhab.binding.fmiweather.internal.client.Client;
import org.openhab.binding.fmiweather.internal.client.Data;
import org.openhab.binding.fmiweather.internal.client.FMIResponse;
import org.openhab.binding.fmiweather.internal.client.Location;

/**
 * Test cases for {@link Client.parseMultiPointCoverageXml} with a xml response having multiple places, parameters
 * and timestamps
 *
 * @author Sami Salonen - Initial contribution
 */
public class FMIResponseParsingMultiplePlacesTest extends AbstractFMIResponseParsingTest {

    private Path observationsMultiplePlaces = getTestResource("observations_multiple_places.xml");
    private Path forecastsMultiplePlaces = getTestResource("forecast_multiple_places.xml");

    private FMIResponse observationsMultiplePlacesResponse;
    private FMIResponse observationsMultiplePlacesNaNResponse;
    private FMIResponse forecastsMultiplePlacesResponse;

    // observation station points (observations_multiple_places.xml) have fmisid as their id
    private Location emasalo = new Location("Porvoo Emäsalo", "101023", new BigDecimal("60.20382"),
            new BigDecimal("25.62546"));
    private Location kilpilahti = new Location("Porvoo Kilpilahti satama", "100683", new BigDecimal("60.30373"),
            new BigDecimal("25.54916"));
    private Location harabacka = new Location("Porvoo Harabacka", "101028", new BigDecimal("60.39172"),
            new BigDecimal("25.60730"));

    // forecast points (forecast_multiple_places.xml) have latitude,longitude as their id
    private Location maarianhamina = new Location("Mariehamn", "60.09726,19.93481", new BigDecimal("60.09726"),
            new BigDecimal("19.93481"));
    private Location pointWithNoName = new Location("19.9,61.0973", "61.09726,19.90000", new BigDecimal("61.09726"),
            new BigDecimal("19.90000"));

    @Before
    public void setUp() throws Throwable {
        observationsMultiplePlacesResponse = parseMultiPointCoverageXml(
                new String(Files.readAllBytes(observationsMultiplePlaces)));
        observationsMultiplePlacesNaNResponse = parseMultiPointCoverageXml(
                new String(Files.readAllBytes(observationsMultiplePlaces)).replace("276.0", "NaN"));
        forecastsMultiplePlacesResponse = parseMultiPointCoverageXml(
                new String(Files.readAllBytes(forecastsMultiplePlaces)));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLocationsMultiplePlacesObservations() throws Throwable {
        // locations
        assertThat(observationsMultiplePlacesResponse.getLocations().size(), is(3));
        assertThat(observationsMultiplePlacesResponse.getLocations(),
                hasItems(deeplyEqualTo(emasalo), deeplyEqualTo(kilpilahti), deeplyEqualTo(harabacka)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLocationsMultiplePlacesForecasts() throws Throwable {
        // locations
        assertThat(forecastsMultiplePlacesResponse.getLocations().size(), is(2));
        assertThat(forecastsMultiplePlacesResponse.getLocations(),
                hasItems(deeplyEqualTo(maarianhamina), deeplyEqualTo(pointWithNoName)));
    }

    @Test
    public void testParametersMultipleObservations() throws Throwable {
        for (Location location : new Location[] { emasalo, kilpilahti, harabacka }) {
            Optional<@NonNull Set<@NonNull String>> parametersOptional = observationsMultiplePlacesResponse
                    .getParameters(location);
            Set<@NonNull String> parameters = parametersOptional.get();
            assertThat(parameters.size(), is(6));
            assertThat(parameters, hasItems("wd_10min", "wg_10min", "rh", "p_sea", "ws_10min", "t2m"));
        }
    }

    @Test
    public void testParametersMultipleForecasts() throws Throwable {
        for (Location location : new Location[] { maarianhamina, pointWithNoName }) {
            Optional<@NonNull Set<@NonNull String>> parametersOptional = forecastsMultiplePlacesResponse
                    .getParameters(location);
            Set<@NonNull String> parameters = parametersOptional.get();
            assertThat(parameters.size(), is(2));
            assertThat(parameters, hasItems("Temperature", "Humidity"));
        }
    }

    @Test
    public void testParseObservationsMultipleData() throws Throwable {
        Data wd_10min = observationsMultiplePlacesResponse.getData(emasalo, "wd_10min").get();
        assertThat(wd_10min, is(deeplyEqualTo(1552215600L, 60, "312.0", "286.0", "295.0", "282.0", "271.0", "262.0",
                "243.0", "252.0", "262.0", "276.0")));
        Data rh = observationsMultiplePlacesResponse.getData(kilpilahti, "rh").get();
        assertThat(rh, is(deeplyEqualTo(1552215600L, 60, "73.0", "65.0", "60.0", "59.0", "57.0", "64.0", "66.0", "65.0",
                "71.0", "77.0")));
    }

    @Test
    public void testParseForecastsMultipleData() throws Throwable {
        Data temperature = forecastsMultiplePlacesResponse.getData(maarianhamina, "Temperature").get();
        assertThat(temperature, is(deeplyEqualTo(1553688000, 360, "3.84", "2.62", "2.26", "1.22", "5.47", "5.52",
                "5.42", "4.78", "8.34", "7.15", null, null, null, null)));
        Data temperature2 = forecastsMultiplePlacesResponse.getData(pointWithNoName, "Temperature").get();
        assertThat(temperature2, is(deeplyEqualTo(1553688000, 360, "1.54", "2.91", "2.41", "2.36", "4.22", "5.28",
                "4.58", "4.0", "4.79", "5.4", null, null, null, null)));

        Data humidity = forecastsMultiplePlacesResponse.getData(maarianhamina, "Humidity").get();
        assertThat(humidity, is(deeplyEqualTo(1553688000, 360, "66.57", "87.38", "85.77", "96.3", "75.74", "81.7",
                "86.78", "87.96", "70.86", "76.35", null, null, null, null)));
        Data humidity2 = forecastsMultiplePlacesResponse.getData(pointWithNoName, "Humidity").get();
        assertThat(humidity2, is(deeplyEqualTo(1553688000, 360, "90.18", "86.22", "89.18", "89.43", "77.26", "78.55",
                "83.36", "85.83", "80.82", "76.92", null, null, null, null)));
    }

    @Test
    public void testParseObservations1NaN() throws Throwable {
        // last value is null, due to NaN measurement value
        Data wd_10min = observationsMultiplePlacesNaNResponse.getData(emasalo, "wd_10min").get();
        assertThat(wd_10min, is(deeplyEqualTo(1552215600L, 60, "312.0", "286.0", "295.0", "282.0", "271.0", "262.0",
                "243.0", "252.0", "262.0", null)));
    }

}
