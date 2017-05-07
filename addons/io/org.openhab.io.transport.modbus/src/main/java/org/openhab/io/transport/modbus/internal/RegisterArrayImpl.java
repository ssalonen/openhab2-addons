package org.openhab.io.transport.modbus.internal;

import java.util.HashMap;
import java.util.Map;

import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.RegisterArray;

import net.wimpi.modbus.procimg.InputRegister;

public class RegisterArrayImpl implements RegisterArray {

    private class RegisterReference implements ModbusRegister {

        private int index;

        public RegisterReference(int index) {
            this.index = index;
        }

        @Override
        public byte[] getBytes() {
            return wrapped[index].toBytes();
        }

        @Override
        public int getValue() {
            return wrapped[index].getValue();
        }

    }

    private InputRegister[] wrapped;
    private Map<Integer, ModbusRegister> cache = new HashMap<>();

    public RegisterArrayImpl(InputRegister[] wrapped) {
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

}
