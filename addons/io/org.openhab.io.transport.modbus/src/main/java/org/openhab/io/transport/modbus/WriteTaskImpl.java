package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.ModbusManager.WriteTask;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public class WriteTaskImpl implements WriteTask {

    private ModbusSlaveEndpoint endpoint;
    private ModbusWriteRequestBlueprint request;
    private ModbusWriteCallback callback;

    public WriteTaskImpl(ModbusSlaveEndpoint endpoint, ModbusWriteRequestBlueprint request,
            ModbusWriteCallback callback) {
        super();
        this.endpoint = endpoint;
        this.request = request;
        this.callback = callback;
    }

    @Override
    public ModbusSlaveEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public ModbusWriteRequestBlueprint getRequest() {
        return request;
    }

    @Override
    public ModbusWriteCallback getCallback() {
        return callback;
    }

}