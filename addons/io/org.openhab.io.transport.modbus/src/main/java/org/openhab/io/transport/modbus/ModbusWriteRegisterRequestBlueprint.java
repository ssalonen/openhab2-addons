package org.openhab.io.transport.modbus;

public interface ModbusWriteRegisterRequestBlueprint extends ModbusWriteRequestBlueprint {

    public ModbusRegisterArray getRegisters();

    @Override
    public default void accept(ModbusWriteRequestBlueprintVisitor visitor) {
        visitor.visit(this);
    }
}
