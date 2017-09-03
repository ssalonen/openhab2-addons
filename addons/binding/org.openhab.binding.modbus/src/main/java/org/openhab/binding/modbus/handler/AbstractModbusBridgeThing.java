package org.openhab.binding.modbus.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;

/**
 *
 * @author Sami Salonen
 *
 */
public abstract class AbstractModbusBridgeThing extends BaseBridgeHandler {

    public AbstractModbusBridgeThing(@NonNull Bridge bridge) {
        super(bridge);
    }

    @SuppressWarnings("null")
    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        getThing().getThings().stream().filter(thing -> thing.getHandler() != null)
                .forEach(thing -> (thing.getHandler()).bridgeStatusChanged(bridgeStatusInfo));
    }

}
