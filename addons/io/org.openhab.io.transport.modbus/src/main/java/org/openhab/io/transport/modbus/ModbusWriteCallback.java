package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.internal.ModbusUnexpectedTransactionIdException;

import net.wimpi.modbus.ModbusException;

/**
 * <p>ModbusWriteCallback interface.</p>
 *
 * @author Sami Salonen
 */
public interface ModbusWriteCallback {

    /**
     * Callback handler method for cases when an error occurred with write
     *
     * Note that only one of the two is called: onError, onResponse
     *
     * @request ModbusWriteRequestBlueprint representing the request
     * @param Exception representing the issue with the request. Instance of
     *            {@link ModbusUnexpectedTransactionIdException} or {@link ModbusException}.
     */
    void onError(ModbusWriteRequestBlueprint request, Exception error);

    /**
     * Callback handler method for successful writes
     *
     * Note that only one of the two is called: onError, onResponse
     *
     * @param request ModbusWriteRequestBlueprint representing the request
     * @param response response matching the write request
     */
    void onWriteResponse(ModbusWriteRequestBlueprint request, ModbusResponse response);
}
