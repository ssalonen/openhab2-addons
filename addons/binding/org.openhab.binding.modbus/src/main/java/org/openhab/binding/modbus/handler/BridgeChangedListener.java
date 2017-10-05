package org.openhab.binding.modbus.handler;

import org.eclipse.smarthome.core.thing.ThingStatus;

@Deprecated
public interface BridgeChangedListener {
    public void bridgeChanged(ThingStatus bridgeStatus);
}
