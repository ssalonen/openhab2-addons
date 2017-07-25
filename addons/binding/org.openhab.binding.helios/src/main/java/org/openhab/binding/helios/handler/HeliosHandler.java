/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.helios.handler;

import static org.openhab.binding.helios.HeliosBindingConstants.*;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.helios.internal.HeliosCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HeliosHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernhard Bauer - Initial contribution
 */
public class HeliosHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(HeliosHandler.class);

    /**
     * The IP Address of the Helios device
     */
    private String host;

    /**
     * The port of the Helios device
     */
    private int port;

    /**
     * The unit address of the Helios device
     */
    private int unit;

    /**
     * The start address when reading/writing from/to the modbus
     */
    private int startAddress;


	public HeliosHandler(Thing thing) {
		super(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {

	    HeliosCommunicator heliosComm = new HeliosCommunicator(this.host, this.port, this.unit, this.startAddress);


        if(channelUID.getId().equals("asdf")) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
	}

    @Override
    public void initialize() {

        this.host = this.getConfig().get(CONFIG_HOST).toString();
        this.port = Integer.parseInt(this.getConfig().get(CONFIG_PORT).toString());
        this.unit = Integer.parseInt(this.getConfig().get(CONFIG_UNIT).toString());
        this.startAddress = Integer.parseInt(this.getConfig().get(CONFIG_START_ADDRESS).toString());

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}
