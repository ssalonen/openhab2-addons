package org.openhab.io.transport.modbus;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusWriteRegisterRequestBlueprint extends ModbusWriteRequestBlueprint {

    public ModbusRegisterArray getRegisters();

    @Override
    public default void accept(ModbusWriteRequestBlueprintVisitor visitor) {
        visitor.visit(this);
    }
}
