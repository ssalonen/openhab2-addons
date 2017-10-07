package org.openhab.binding.modbus.handler;

import org.eclipse.smarthome.core.thing.ThingStatus;

@Deprecated
public interface BridgeChangedListener {
    @Deprecated
    public void bridgeChanged(ThingStatus bridgeStatus);
}
