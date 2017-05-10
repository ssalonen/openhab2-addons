package org.openhab.binding.modbus.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;

public abstract class AbstractModbusBridgeThing extends BaseBridgeHandler {

    public AbstractModbusBridgeThing(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        getThing().getThings().stream().filter(thing -> thing.getHandler() != null)
                .forEach(thing -> (thing.getHandler()).bridgeStatusChanged(bridgeStatusInfo));
    }

}
