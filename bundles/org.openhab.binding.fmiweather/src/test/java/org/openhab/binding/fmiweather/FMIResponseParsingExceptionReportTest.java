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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.openhab.binding.fmiweather.internal.AbstractWeatherHandler;
import org.openhab.binding.fmiweather.internal.client.exception.FMIResponseException;

/**
 * Test cases for {@link AbstractWeatherHandler}. The tests provide mocks for supporting entities using Mockito.
 *
 * @author Sami Salonen - Initial contribution
 */
public class FMIResponseParsingExceptionReportTest extends AbstractFMIResponseParsingTest {

    private Path error1 = getTestResource("error1.xml");

    @Test
    public void testErrorResponse() throws IOException, Throwable {
        try {
            parseMultiPointCoverageXml(new String(Files.readAllBytes(error1)));
        } catch (FMIResponseException e) {
            // OK
            assertThat(e.getMessage(), is(
                    "Exception report (OperationParsingFailed): [Invalid time interval!, The start time is later than the end time., URI: /wfs?endtime=1900-03-10T20%3A10%3A00Z&fmisid=101023&parameters=t2m%2Crh%2Cwd_10min%2Cws_10min%2Cwg_10min%2Cp_sea&request=getFeature&service=WFS&starttime=2019-03-10T10%3A10%3A00Z&storedquery_id=fmi%3A%3Aobservations%3A%3Aweather%3A%3Amultipointcoverage&timestep=60&version=2.0.0]"));
            return;
        } catch (Exception e) {
            fail("Wrong exception, was " + e.getClass().getName());
        }
        fail("FMIResponseException expected");
    }

}
