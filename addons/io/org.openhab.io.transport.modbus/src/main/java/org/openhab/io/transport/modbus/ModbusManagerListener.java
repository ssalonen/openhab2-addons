package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * <p>ModbusManagerListener interface.</p>
 *
 * @author Sami Salonen
 */
public interface ModbusManagerListener {

    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration);

}
