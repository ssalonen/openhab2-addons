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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.openhab.binding.fmiweather.internal.client.Client;
import org.openhab.binding.fmiweather.internal.client.FMIResponse;

/**
 * Test cases for {@link Client.parseMultiPointCoverageXml} with an "empty" (no data) XML response
 *
 * @author Sami Salonen - Initial contribution
 */
public class FMIResponseParsingEmptyTest extends AbstractFMIResponseParsingTest {

    private Path observations = getTestResource("observations_empty.xml");

    private FMIResponse observationsResponse;

    @Before
    public void setUp() throws Throwable {
        client = new Client();
        observationsResponse = parseMultiPointCoverageXml(new String(Files.readAllBytes(observations)));
        assertNotNull(observationsResponse);
    }

    @Test
    public void testLocationsSinglePlace() throws Throwable {
        assertThat(observationsResponse.getLocations().size(), is(0));
    }

}
