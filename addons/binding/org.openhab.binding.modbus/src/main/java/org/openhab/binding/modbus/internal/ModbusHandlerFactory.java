/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.modbus.internal;

import static org.openhab.binding.modbus.ModbusBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandler;
import org.openhab.binding.modbus.handler.ModbusReadThingHandler;
import org.openhab.binding.modbus.handler.ModbusReadWriteThingHandler;
import org.openhab.binding.modbus.handler.ModbusTcpThingHandler;
import org.openhab.binding.modbus.handler.ModbusWriteThingHandler;
import org.openhab.io.transport.modbus.ModbusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sami Salonen - Initial contribution
 */
public class ModbusHandlerFactory extends BaseThingHandlerFactory implements ModbusManagerReference {

    private final Logger logger = LoggerFactory.getLogger(ModbusHandlerFactory.class);

    private ModbusManager manager;

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MODBUS_TCP);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MODBUS_POLLER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MODBUS_READ_WRITE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MODBUS_WRITE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MODBUS_READ);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_MODBUS_TCP)) {
            logger.debug("createHandler Modbus tcp");
            return new ModbusTcpThingHandler((Bridge) thing, this);
        } else if (thingTypeUID.equals(THING_TYPE_MODBUS_POLLER)) {
            logger.debug("createHandler Modbus poller");
            return new ModbusPollerThingHandler((Bridge) thing, this);
        } else if (thingTypeUID.equals(THING_TYPE_MODBUS_READ_WRITE)) {
            logger.debug("createHandler read-write");
            return new ModbusReadWriteThingHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_MODBUS_READ)) {
            logger.debug("createHandler read");
            return new ModbusReadThingHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_MODBUS_WRITE)) {
            logger.debug("createHandler write");
            return new ModbusWriteThingHandler(thing);
        }
        logger.info("createHandler for unknown thing");

        return null;
    }

    public void setManager(ModbusManager manager) {
        logger.info("Setting manager: " + manager.toString());
        this.manager = manager;
    }

    public void unsetManager(ModbusManager manager) {
        this.manager = null;
    }

    @Override
    public ModbusManager getManager() {
        return manager;
    }
}
