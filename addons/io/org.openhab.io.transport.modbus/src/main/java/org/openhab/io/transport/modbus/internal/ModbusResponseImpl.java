package org.openhab.io.transport.modbus.internal;

import org.openhab.io.transport.modbus.ModbusResponse;

import net.wimpi.modbus.msg.ModbusMessage;

public class ModbusResponseImpl implements ModbusResponse {

    private int responseFunctionCode;

    public ModbusResponseImpl(ModbusMessage response) {
        super();
        this.responseFunctionCode = response.getFunctionCode();
    }

    @Override
    public int getResponseFunctionCode() {
        return responseFunctionCode;
    }

}
