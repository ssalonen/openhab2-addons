/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.transport.modbus;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * <p>
 * ModbusManager interface.
 * </p>
 *
 * @author Sami Salonen
 */
public interface ModbusManager {

    public interface TaskWithEndpoint<R, C extends ModbusCallback> {
        /**
         * Gets endpoint associated with this task
         *
         * @return
         */
        ModbusSlaveEndpoint getEndpoint();

        /**
         * Gets request associated with this task
         *
         * @return
         */
        R getRequest();

        /**
         * Gets callback associated with this task, will be called with response
         *
         * @return
         */
        WeakReference<C> getCallback();

        int getMaxTries();
    }

    /**
     * Poll task represents modbus read request
     *
     * Must be hashable. HashCode and equals should be defined such that no two poll tasks are registered that are
     * equal.
     *
     * @author Sami Salonen
     *
     * @see ModbusManager.registerRegularPoll
     */
    public interface PollTask extends TaskWithEndpoint<ModbusReadRequestBlueprint, ModbusReadCallback> {
        @Override
        default int getMaxTries() {
            return getRequest().getMaxTries();
        }
    }

    /**
     * Poll task represents modbus write request
     *
     * Unlike {@link PollTask}, this does not have to be hashable.
     *
     * @author Sami Salonen
     *
     */
    public interface WriteTask extends TaskWithEndpoint<ModbusWriteRequestBlueprint, ModbusWriteCallback> {
        @Override
        default int getMaxTries() {
            return getRequest().getMaxTries();
        }
    }

    /**
     * Submit one-time poll task. The method returns immediately
     *
     * @param task
     * @return
     */
    public ScheduledFuture<?> submitOneTimePoll(@NonNull PollTask task);

    /**
     * Register regularly polled task. The method returns immediately
     *
     * @param task
     * @return
     */
    public void registerRegularPoll(@NonNull PollTask task, long pollPeriodMillis, long initialDelayMillis);

    /**
     * Unregister regularly polled task
     *
     * @param task
     * @return whether poll task was unregistered. Poll task is not unregistered in case of unexpected errors or
     *         non-existing poll task
     */
    public boolean unregisterRegularPoll(@NonNull PollTask task);

    public ScheduledFuture<?> submitOneTimeWrite(@NonNull WriteTask task);

    /**
     * Configure general connection settings with a given endpoint
     *
     * @param endpoint endpoint to configure
     * @param configuration configuration for the endpoint. Use null to reset the configuration to default.
     */
    public void setEndpointPoolConfiguration(@NonNull ModbusSlaveEndpoint endpoint,
            EndpointPoolConfiguration configuration);

    public EndpointPoolConfiguration getEndpointPoolConfiguration(@NonNull ModbusSlaveEndpoint endpoint);

    public void addListener(@NonNull ModbusManagerListener listener);

    public void removeListener(@NonNull ModbusManagerListener listener);

    /**
     * Get registered regular polls
     *
     * @return set of registered regular polls
     */
    public Set<@NonNull PollTask> getRegisteredRegularPolls();

}
