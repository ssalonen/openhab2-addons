package org.openhab.io.transport.modbus;

import org.apache.commons.lang.StringUtils;

/**
 * <p>ModbusRegisterArrayImpl class.</p>
 *
 * @author Sami Salonen
 */
public class ModbusRegisterArrayImpl implements ModbusRegisterArray {

    private ModbusRegister[] registers;

    public ModbusRegisterArrayImpl(ModbusRegister[] registers) {
        this.registers = registers;
    }

    @Override
    public ModbusRegister getRegister(int index) {
        return registers[index];
    }

    @Override
    public int size() {
        return registers.length;
    }

    @Override
    public String toString() {
        return new StringBuffer("ModbusRegisterArrayImpl(").append(StringUtils.join(registers, ',')).append(')')
                .toString();
    }

}
