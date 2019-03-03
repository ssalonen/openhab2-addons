/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.diagnostics.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.items.StateChangeListener;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DiagnosticsHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.diagnostics", service = ThingHandlerFactory.class)
public class DiagnosticsHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(DiagnosticsHandlerFactory.class);
    protected final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool("diagnostics");
    private @Nullable ScheduledFuture<?> scheduleWithFixedDelay;

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        // ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        // if (threadMXBean.isThreadCpuTimeSupported()) {
        // threadMXBean.setThreadCpuTimeEnabled(true);
        // }

        scheduleWithFixedDelay = scheduler.scheduleWithFixedDelay(() -> {
            ExecutorService pool = ThreadPoolManager.getPool("items");
            if (pool instanceof ThreadPoolExecutor) {
                BlockingQueue<Runnable> queue_ = ((ThreadPoolExecutor) pool).getQueue();
                Runnable[] runnables = queue_.toArray(new Runnable[0]);
                // shuffleArray(runnables);
                Map<String, Long> methodCounts = Stream.of(runnables).map(runnable -> {
                    Method enclosingMethod = runnable.getClass().getEnclosingMethod();
                    Class<?> methodClass = enclosingMethod.getDeclaringClass();
                    return String.format("%s:%s", methodClass.getCanonicalName(), enclosingMethod.getName());
                }).collect(Collectors.groupingBy(k -> k, Collectors.counting()));
                AtomicBoolean itemCountsErrorLogged = new AtomicBoolean(false);
                Map<String, Long> itemCounts = Stream.of(runnables).map(runnable -> {
                    try {
                        return getGenericItem(runnable).getName();
                    } catch (IllegalArgumentException | IllegalAccessException | ClassCastException e) {
                        if (itemCountsErrorLogged.compareAndSet(false, true)) {
                            logger.error("Runnable is not bound to GenericItem? (itemCounts)", e);
                        }
                        return "<na>";
                    }
                }).collect(Collectors.groupingBy(k -> k, Collectors.counting()));
                AtomicBoolean genericItemListenersErrorLogged = new AtomicBoolean(false);
                Map<String, Long> genericItemListeners = Stream.of(runnables).map(runnable -> {
                    try {
                        Field boundListener = runnable.getClass().getDeclaredField("val$listener");
                        boundListener.setAccessible(true);
                        StateChangeListener listener = (StateChangeListener) boundListener.get(runnable);
                        return listener.getClass().getCanonicalName();
                    } catch (IllegalArgumentException | IllegalAccessException | ClassCastException
                            | NoSuchFieldException | SecurityException e) {
                        if (genericItemListenersErrorLogged.compareAndSet(false, true)) {
                            logger.error("Runnable is not bound to GenericItem? (genericItemListeners)", e);
                        }
                        return "<na>";
                    }
                }).collect(Collectors.groupingBy(k -> k, Collectors.counting()));
                logger.info("items queue size: {}, method counts {}, itemCounts {}, listeners {}", runnables.length,
                        methodCounts, itemCounts, genericItemListeners);
            }
            // ThreadInfo[] threads = threadMXBean.dumpAllThreads(true, true);
            // Stream.of(threads).filter(threadInfo -> Stream.of(threadInfo.getStackTrace()).anyMatch(stack -> {
            // String class1 = stack.getClassName();
            // return (class1.contains("Item") || class1.contains("PersistenceManager")
            // || class1.contains("PersistItemsJob"));
            //
            // })).forEach(threadInfo -> {
            // // threadInfo.
            // });

        }, 0, 10000, TimeUnit.MILLISECONDS);
    }

    private org.eclipse.smarthome.core.items.GenericItem getGenericItem(Runnable runnable)
            throws IllegalArgumentException, IllegalAccessException, ClassCastException {
        Field thisArgToRunnable = runnable.getClass().getDeclaredFields()[0];
        thisArgToRunnable.setAccessible(true);
        return ((org.eclipse.smarthome.core.items.GenericItem) thisArgToRunnable.get(runnable));
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        super.deactivate(componentContext);
        if (scheduleWithFixedDelay != null) {
            scheduleWithFixedDelay.cancel(true);
            scheduleWithFixedDelay = null;
        }
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return false;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        return null;
    }
}
