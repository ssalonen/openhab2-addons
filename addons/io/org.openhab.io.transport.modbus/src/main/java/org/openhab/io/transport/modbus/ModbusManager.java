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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.transport.modbus.endpoint.EndpointPoolConfiguration;
import org.openhab.io.transport.modbus.endpoint.ModbusSlaveEndpoint;

/**
 * ModbusManager is the main interface for interacting with Modbus slaves
 *
 * @author Sami Salonen
 */
public interface ModbusManager {

    /**
     * Common base interface for read and write tasks.
     *
     * @author Sami Salonen
     *
     * @param <R> request type
     * @param <C> callback type
     */
    public interface Task<R> {
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

        int getMaxTries();
    }

    /**
     * Common base interface for read and write tasks.
     *
     * @author Sami Salonen
     *
     * @param <R> request type
     * @param <C> callback type
     */
    public interface TaskWithCallback<R, C extends ModbusCallback> extends Task<R> {
        WeakReference<C> getCallback();

    }

    /**
     * Poll task represents Modbus read request
     *
     * Must be hashable. HashCode and equals should be defined such that no two poll tasks are registered that are
     * equal.
     *
     * @author Sami Salonen
     *
     * @see ModbusManager.registerRegularPoll
     */
    public interface PollTask extends Task<ModbusReadRequestBlueprint> {
        @Override
        default int getMaxTries() {
            return getRequest().getMaxTries();
        }
    }

    /**
     * Poll task represents Modbus read request
     *
     * Must be hashable. HashCode and equals should be defined such that no two poll tasks are registered that are
     * equal.
     *
     * @author Sami Salonen
     *
     * @see ModbusManager.registerRegularPoll
     */
    public interface PollTaskWithCallback
            extends TaskWithCallback<ModbusReadRequestBlueprint, ModbusReadCallback>, PollTask {
    }

    /**
     * Poll task represents Modbus write request
     *
     * Unlike {@link PollTaskWithCallback}, this does not have to be hashable.
     *
     * @author Sami Salonen
     *
     */
    public interface WriteTask extends Task<ModbusWriteRequestBlueprint> {
        @Override
        default int getMaxTries() {
            return getRequest().getMaxTries();
        }
    }

    /**
     * Poll task represents Modbus write request
     *
     * Unlike {@link PollTaskWithCallback}, this does not have to be hashable.
     *
     * @author Sami Salonen
     *
     */
    public interface WriteTaskWithCallback
            extends TaskWithCallback<ModbusWriteRequestBlueprint, ModbusWriteCallback>, WriteTask {
    }

    /**
     * Submit one-time poll task. The method returns immediately, and the execution of the poll task will happen in
     * background.
     *
     * {@link CompletableFuture} can be used to access the polled data or errors during execution. When successful, the
     * polled data is either {@link ModbusRegisterArray} or {@link BitArray}.
     *
     * @param task
     * @return future representing the polled task
     */
    public CompletableFuture<Object> submitOneTimePoll(@NonNull PollTask task);

    /**
     * Submit one-time poll task. The method returns immediately, and the execution of the poll task will happen in
     * background.
     *
     * Callback of task receives polled data and errors.
     *
     * @param task
     * @return future representing the polled task
     */
    public ScheduledFuture<?> submitOneTimePoll(@NonNull PollTaskWithCallback task);

    /**
     * Register regularly polled task. The method returns immediately, and the execution of the poll task will happen in
     * the background.
     *
     * @param task
     * @return
     */
    public void registerRegularPoll(@NonNull PollTaskWithCallback task, long pollPeriodMillis, long initialDelayMillis);

    /**
     * Unregister regularly polled task
     *
     * @param task poll task to unregister
     * @return whether poll task was unregistered. Poll task is not unregistered in case of unexpected errors or
     *         in the case where the poll task is not registered in the first place
     */
    public boolean unregisterRegularPoll(@NonNull PollTaskWithCallback task);

    /**
     * Submit one-time write task. The method returns immediately, and the execution of the task will happen in
     * background.
     *
     * {@link CompletableFuture} can be used to access the polled data or errors during execution.
     *
     * @param task
     * @return future representing the task
     */
    public CompletableFuture<ModbusResponse> submitOneTimeWrite(@NonNull WriteTask task);

    /**
     * Submit one-time write task. The method returns immediately, and the execution of the task will happen in
     * background.
     *
     * Callback of task receives {@link ModbusResponse} or errors.
     *
     * @param task
     * @return future representing the task
     */
    public ScheduledFuture<?> submitOneTimeWrite(@NonNull WriteTaskWithCallback task);

    /**
     * Configure general connection settings with a given endpoint
     *
     * @param endpoint endpoint to configure
     * @param configuration configuration for the endpoint. Use null to reset the configuration to default settings.
     */
    public void setEndpointPoolConfiguration(@NonNull ModbusSlaveEndpoint endpoint,
            EndpointPoolConfiguration configuration);

    /**
     * Get general configuration settings applied to a given endpoint
     *
     * Note that default configuration settings are returned in case the endpoint has not been configured.
     *
     * @param endpoint endpoint to query
     * @return general connection settings of the given endpoint
     */
    public @NonNull EndpointPoolConfiguration getEndpointPoolConfiguration(@NonNull ModbusSlaveEndpoint endpoint);

    /**
     * Register listener for changes
     *
     * @param listener
     */
    public void addListener(@NonNull ModbusManagerListener listener);

    /**
     * Remove listener for changes
     *
     * @param listener
     */
    public void removeListener(@NonNull ModbusManagerListener listener);

    /**
     * Get registered regular polls
     *
     * @return set of registered regular polls
     */
    public Set<@NonNull PollTaskWithCallback> getRegisteredRegularPolls();

}
