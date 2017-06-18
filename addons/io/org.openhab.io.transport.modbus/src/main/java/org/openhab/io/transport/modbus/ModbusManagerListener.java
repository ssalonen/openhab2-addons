package org.openhab.io.transport.modbus;

import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

public interface ModbusManagerListener {

    public void onEndpointPoolConfigurationSet(ModbusSlaveEndpoint endpoint, EndpointPoolConfiguration configuration);

}
