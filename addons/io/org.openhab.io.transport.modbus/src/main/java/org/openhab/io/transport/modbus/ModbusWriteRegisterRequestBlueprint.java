package org.openhab.io.transport.modbus;

public interface ModbusWriteRegisterRequestBlueprint extends ModbusWriteRequestBlueprint {

    public ModbusRegisterArray getRegisters();
}
