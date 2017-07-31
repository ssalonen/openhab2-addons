package org.openhab.io.transport.modbus;

/**
 *
 * @author Sami Salonen
 *
 */
public interface ModbusWriteCoilRequestBlueprint extends ModbusWriteRequestBlueprint {

    public BitArray getCoils();

    @Override
    public default void accept(ModbusWriteRequestBlueprintVisitor visitor) {
        visitor.visit(this);
    }
}
