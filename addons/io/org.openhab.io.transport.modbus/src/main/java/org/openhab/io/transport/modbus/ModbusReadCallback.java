package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.internal.ModbusUnexpectedTransactionIdException;

import net.wimpi.modbus.ModbusException;

public interface ModbusReadCallback {

    /**
     * Posts update event to OpenHAB bus for "holding" and "input register" type slaves
     *
     * @param ModbusReadRequestBlueprint representing the request
     * @param registers data received from slave device in the last pollInterval
     */
    void internalUpdateItem(ModbusReadRequestBlueprint request, ModbusRegisterArray registers);

    /**
     * Posts update event to OpenHAB bus for "coil" and "discrete input" type slaves
     *
     * @param ModbusReadRequestBlueprint representing the request
     * @param registers data received from slave device in the last pollInterval
     */
    void internalUpdateItem(ModbusReadRequestBlueprint request, BitArray coils);

    /**
     * Posts update event to OpenHAB bus for all types of slaves when there is a read error
     *
     * @request ModbusRequestBlueprint representing the request
     * @param Exception representing the issue with the request. Instance of
     *            {@link ModbusUnexpectedTransactionIdException} or {@link ModbusException}.
     */
    void internalUpdateReadErrorItem(ModbusReadRequestBlueprint request, Exception error);

}