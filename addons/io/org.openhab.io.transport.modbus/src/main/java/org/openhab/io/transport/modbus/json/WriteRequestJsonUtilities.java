package org.openhab.io.transport.modbus.json;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.NotImplementedException;
import org.openhab.io.transport.modbus.ModbusRegister;
import org.openhab.io.transport.modbus.ModbusRegisterArrayImpl;
import org.openhab.io.transport.modbus.ModbusRegisterImpl;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprintImpl;
import org.openhab.io.transport.modbus.ModbusWriteFunctionCode;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprintImpl;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.internal.BitArrayWrappingBitVector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.wimpi.modbus.util.BitVector;

public final class WriteRequestJsonUtilities {
    public final static String JSON_FUNCTION_CODE = "functionCode";
    public final static String JSON_ADDRESS = "address";
    public final static String JSON_VALUE = "value";

    private final static JsonParser parser = new JsonParser();

    private WriteRequestJsonUtilities() {
        throw new NotImplementedException();
    }

    public static Collection<ModbusWriteRequestBlueprint> fromJson(int unitId, String jsonString) {
        JsonArray jsonArray = parser.parse(jsonString).getAsJsonArray();
        if (jsonArray.size() == 0) {
            return new LinkedList<>();
        }
        Deque<ModbusWriteRequestBlueprint> writes = new LinkedList<>();
        jsonArray.forEach(writeElem -> {
            writes.add(constructBluerint(unitId, writeElem));
        });
        return writes;
    }

    private static ModbusWriteRequestBlueprint constructBluerint(int unitId, JsonElement arrayElement) {
        JsonObject writeObject = arrayElement.getAsJsonObject();
        JsonElement functionCode = writeObject.get(JSON_FUNCTION_CODE);
        JsonElement address = writeObject.get(JSON_ADDRESS);
        JsonArray valuesElem = writeObject.get(JSON_VALUE).getAsJsonArray();
        return constructBluerint(unitId, functionCode, address, valuesElem);
    }

    private static ModbusWriteRequestBlueprint constructBluerint(int unitId, JsonElement functionCodeElem,
            JsonElement addressElem, JsonArray valuesElem) {
        int functionCodeNumeric = functionCodeElem.getAsInt();
        ModbusWriteFunctionCode functionCode = ModbusWriteFunctionCode.fromFunctionCode(functionCodeNumeric);
        int address = addressElem.getAsInt();

        AtomicBoolean writeSingle = new AtomicBoolean(false);
        switch (functionCode) {
            case WRITE_COIL:
                writeSingle.set(true);
                if (valuesElem.size() != 1) {
                    throw new IllegalArgumentException(String
                            .format("Expecting single value with functionCode=%s, got: %d", functionCode, valuesElem));
                }
                // fall-through to WRITE_MULTIPLE_COILS
            case WRITE_MULTIPLE_COILS:
                BitVector bits = new BitVector(valuesElem.size());
                if (bits.size() == 0) {
                    throw new IllegalArgumentException("Must provide at least one coil");
                }
                // TODO: how does true/false work?
                for (int i = 0; i < valuesElem.size(); i++) {
                    bits.setBit(i, valuesElem.get(i).getAsInt() != 0);
                }
                return new ModbusWriteCoilRequestBlueprintImpl(unitId, address, new BitArrayWrappingBitVector(bits),
                        !writeSingle.get());
            case WRITE_SINGLE_REGISTER:
                writeSingle.set(true);
                if (valuesElem.size() != 1) {
                    throw new IllegalArgumentException(String
                            .format("Expecting single value with functionCode=%s, got: %d", functionCode, valuesElem));
                }
                // fall-through to WRITE_MULTIPLE_REGISTERS
            case WRITE_MULTIPLE_REGISTERS: {
                ModbusRegister[] registers = new ModbusRegister[valuesElem.size()];
                if (registers.length == 0) {
                    throw new IllegalArgumentException("Must provide at least one register");
                }
                for (int i = 0; i < valuesElem.size(); i++) {
                    registers[i] = new ModbusRegisterImpl(valuesElem.get(i).getAsInt());
                }
                return new ModbusWriteRegisterRequestBlueprintImpl(unitId, address,
                        new ModbusRegisterArrayImpl(registers), !writeSingle.get());
            }
            default:
                throw new IllegalStateException("Unknown function code");
        }
    }

}
