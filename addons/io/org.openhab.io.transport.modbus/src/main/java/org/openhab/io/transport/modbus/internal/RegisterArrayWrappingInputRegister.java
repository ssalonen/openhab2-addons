package org.openhab.io.transport.modbus.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArray;

import net.wimpi.modbus.procimg.InputRegister;

/**
 * <p>RegisterArrayWrappingInputRegister class.</p>
 *
 * @author Sami Salonen
 */
public class RegisterArrayWrappingInputRegister implements ModbusRegisterArray {

    private class RegisterReference implements ModbusRegister {

        private InputRegister wrappedRegister;

        public RegisterReference(int index) {
            this.wrappedRegister = wrapped[index];
        }

        @Override
        public byte[] getBytes() {
            return wrappedRegister.toBytes();
        }

        @Override
        public int getValue() {
            return wrappedRegister.getValue();
        }

        @Override
        public int toUnsignedShort() {
            return wrappedRegister.toUnsignedShort();
        }

    }

    private InputRegister[] wrapped;
    private Map<Integer, ModbusRegister> cache = new HashMap<>();

    public RegisterArrayWrappingInputRegister(InputRegister[] wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ModbusRegister getRegister(int index) {
        return cache.computeIfAbsent(index, i -> new RegisterReference(i));
    }

    @Override
    public int size() {
        return wrapped.length;
    }

    @Override
    public String toString() {
        return new StringBuilder("RegisterArrayWrappingInputRegister(hiLowBytes=[ ").append(StringUtils.join(
                Stream.of(wrapped).map(reg -> reg.toBytes()).map(bytes -> Arrays.toString(bytes)).iterator(), ", "))
                .append(" ])").toString();
    }

}
