/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ModbusBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusBindingConstants {

    public static final String BINDING_ID = "modbus";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_MODBUS_WRITE = new ThingTypeUID(BINDING_ID, "writeDefinition");
    public final static ThingTypeUID THING_TYPE_MODBUS_READ = new ThingTypeUID(BINDING_ID, "readDefinition");
    public final static ThingTypeUID THING_TYPE_MODBUS_READ_WRITE = new ThingTypeUID(BINDING_ID, "readWriteDefinition");

    // List of all Channel ids
    public final static String CHANNEL_SWITCH = "switch";
    public final static String CHANNEL_STRING = "string";

}
