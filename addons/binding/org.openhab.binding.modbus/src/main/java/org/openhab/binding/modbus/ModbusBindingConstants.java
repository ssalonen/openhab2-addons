/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;

/**
 * The {@link ModbusBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusBindingConstants {

    public static final String BINDING_ID = "modbus";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_MODBUS_TCP = new ThingTypeUID(BINDING_ID, "tcp");
    public final static ThingTypeUID THING_TYPE_MODBUS_POLLER = new ThingTypeUID(BINDING_ID, "poller");
    public final static ThingTypeUID THING_TYPE_MODBUS_WRITE = new ThingTypeUID(BINDING_ID, "write");
    public final static ThingTypeUID THING_TYPE_MODBUS_READ = new ThingTypeUID(BINDING_ID, "read");
    public final static ThingTypeUID THING_TYPE_MODBUS_READ_WRITE = new ThingTypeUID(BINDING_ID, "readwrite");

    // List of all Channel ids
    public final static String CHANNEL_SWITCH = "switch";
    public final static String CHANNEL_NUMBER = "number";
    public final static String CHANNEL_STRING = "string";

    public final static Map<String, ModbusReadFunctionCode> READ_FUNCTION_CODES = new HashMap<String, ModbusReadFunctionCode>();
    static {
        READ_FUNCTION_CODES.put("coil", ModbusReadFunctionCode.READ_COILS);
        READ_FUNCTION_CODES.put("discrete", ModbusReadFunctionCode.READ_INPUT_DISCRETES);
        READ_FUNCTION_CODES.put("input", ModbusReadFunctionCode.READ_INPUT_REGISTERS);
        READ_FUNCTION_CODES.put("holding", ModbusReadFunctionCode.READ_MULTIPLE_REGISTERS);
    }
}
