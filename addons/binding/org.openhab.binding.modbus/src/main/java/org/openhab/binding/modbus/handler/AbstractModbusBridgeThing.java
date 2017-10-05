package org.openhab.binding.modbus.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingStatus;
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
    public void notifyChildrenBridgeChanged(ThingStatus bridgeStatus) {
        getThing().getThings().stream().filter(thing -> thing.getHandler() != null).forEach(thing -> {
            if (thing.getHandler() instanceof BridgeChangedListener) {
                ((BridgeChangedListener) thing.getHandler()).bridgeChanged(bridgeStatus);
            }
        });
    }

}
