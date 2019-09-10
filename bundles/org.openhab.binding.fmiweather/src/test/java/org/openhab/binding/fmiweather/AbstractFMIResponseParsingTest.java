/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.fmiweather;

import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.openhab.binding.fmiweather.internal.client.Client;
import org.openhab.binding.fmiweather.internal.client.Data;
import org.openhab.binding.fmiweather.internal.client.FMIResponse;
import org.openhab.binding.fmiweather.internal.client.Location;

public class AbstractFMIResponseParsingTest {

    protected Client client;

    @Before
    public void setUpClient() throws Throwable {
        client = new Client();
    }

    protected Path getTestResource(String filename) {
        try {
            return Paths.get(getClass().getResource(filename).toURI());
        } catch (URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected static TypeSafeMatcher<Location> deeplyEqualTo(Location location) {
        return new ResponseLocationMatcher(location);
    }

    protected static Matcher<Data> deeplyEqualTo(long start, int intervalMinutes, String... values) {
        return new TypeSafeMatcher<Data>() {

            private TimestampMatcher timestampMatcher = new TimestampMatcher(start, intervalMinutes, values.length);
            private ValuesMatcher valuesMatcher = new ValuesMatcher(values);

            @Override
            public void describeTo(Description description) {
                description.appendDescriptionOf(timestampMatcher);
                description.appendText(" and ");
                description.appendDescriptionOf(valuesMatcher);
            }

            @Override
            protected boolean matchesSafely(Data dataValues) {
                return timestampMatcher.matches(dataValues.timestampsEpochSecs)
                        && valuesMatcher.matches(dataValues.values);
            }
        };

    }

    protected FMIResponse parseMultiPointCoverageXml(String content) throws Throwable {
        try {
            Method parseMethod = Client.class.getDeclaredMethod("parseMultiPointCoverageXml", String.class);
            parseMethod.setAccessible(true);
            return (FMIResponse) parseMethod.invoke(client, content);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            fail(String.format("Unexpected reflection error (code changed?) %s: %s", e.getClass().getName(),
                    e.getMessage()));
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<Location> parseStations(String content) throws Throwable {
        try {
            Method parseMethod = Client.class.getDeclaredMethod("parseStations", String.class);
            parseMethod.setAccessible(true);
            return (Set<Location>) parseMethod.invoke(client, content);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            fail(String.format("Unexpected reflection error (code changed?) %s: %s", e.getClass().getName(),
                    e.getMessage()));
            return null;
        }
    }
}
