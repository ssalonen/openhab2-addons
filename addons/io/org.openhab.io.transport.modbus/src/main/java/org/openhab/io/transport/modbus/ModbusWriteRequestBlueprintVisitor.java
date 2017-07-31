package org.openhab.io.transport.modbus;

/**
 * <p>ModbusWriteRequestBlueprintVisitor interface.</p>
 *
 * @author Sami Salonen
 */
public interface ModbusWriteRequestBlueprintVisitor {

    public void visit(ModbusWriteCoilRequestBlueprint blueprint);

    public void visit(ModbusWriteRegisterRequestBlueprint blueprint);

}
