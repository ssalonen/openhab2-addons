package org.openhab.binding.modbus.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;

public abstract class AbstractModbusBridgeThing extends BaseThingHandler implements BridgeRefreshListener {

    public AbstractModbusBridgeThing(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        doInitialize();
        onBridgeRefresh();
    }

    abstract protected void doInitialize();

    @Override
    public void onBridgeRefresh() {
        refreshChildren();
    }

    private void refreshChildren() {
        ((Bridge) getThing()).getThings().stream()
                .filter(thing -> thing.getHandler() != null && thing.getHandler() instanceof BridgeRefreshListener)
                .forEach(thing -> ((BridgeRefreshListener) thing.getHandler()).onBridgeRefresh());
    }

}
