/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.persistence.jdbc.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Helmut Lehmeyer
 *
 */
public class JdbcPersistenceService extends JdbcMapper implements QueryablePersistenceService {
    static final Logger logger = LoggerFactory.getLogger(JdbcPersistenceService.class);

    protected ItemRegistry itemRegistry;

    /**
     * The BundleContext. This is only valid when the bundle is ACTIVE. It is
     * set in the activate() method and must not be accessed anymore once the
     * deactivate() method was called or before activate() was called.
     */
    @SuppressWarnings("unused")
    private BundleContext bundleContext;

    /**
     * Called by the SCR to activate the component with its configuration read
     * from CAS
     *
     * @param bundleContext
     *            BundleContext of the Bundle that defines this component
     * @param configuration
     *            Configuration properties for this component obtained from the
     *            ConfigAdmin service
     */
    public void activate(BundleContext bundleContext, Map<Object, Object> configuration) {
        logger.debug("JDBC::activate: persistence service activated");
        this.bundleContext = bundleContext;
        updateConfig(configuration);
    }

    /**
     * Called by the SCR to deactivate the component when either the
     * configuration is removed or mandatory references are no longer satisfied
     * or the component has simply been stopped.
     *
     * @param reason
     *            Reason code for the deactivation:<br>
     *            <ul>
     *            <li>0 – Unspecified
     *            <li>1 – The component was disabled
     *            <li>2 – A reference became unsatisfied
     *            <li>3 – A configuration was changed
     *            <li>4 – A configuration was deleted
     *            <li>5 – The component was disposed
     *            <li>6 – The bundle was stopped
     *            </ul>
     */
    public void deactivate(final int reason) {
        logger.debug("JDBC::deactivate:  persistence bundle stopping. Disconnecting from database. reason={}", reason);
        // closeConnection();
        this.bundleContext = null;
        initialized = false;
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        logger.debug("JDBC::setItemRegistry");
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        logger.debug("JDBC::unsetItemRegistry");
        // closeConnection();
        this.itemRegistry = null;
    }

    @Override
    public String getId() {
        logger.debug("JDBC::getName: returning name 'jdbc' for queryable persistence service.");
        return "jdbc";
    }

    @Override
    public String getLabel(Locale locale) {
        return "JDBC";
    }

    @Override
    public void store(Item item) {
        store(item, null);
    }

    /**
     * @{inheritDoc
     */
    @Override
    public void store(Item item, @Nullable String alias) {
        // Don not store undefined/uninitialised data
        if (item.getState() instanceof UnDefType) {
            logger.debug("JDBC::store: ignore Item '{}' because it is UnDefType", item.getName());
            return;
        }
        if (!checkDBAccessability()) {
            logger.warn(
                    "JDBC::store:  No connection to database. Cannot persist item '{}'! Will retry connecting to database when error count:{} equals errReconnectThreshold:{}",
                    item, errCnt, conf.getErrReconnectThreshold());
            return;
        }
        long timerStart = System.currentTimeMillis();
        storeItemValue(item);
        logger.debug("JDBC: Stored item '{}' as '{}' in SQL database at {} in {} ms.", item.getName(),
                item.getState().toString(), (new java.util.Date()).toString(), System.currentTimeMillis() - timerStart);
    }

    @Override
    public @NonNull Set<@NonNull PersistenceItemInfo> getItemInfo() {
        return Collections.emptySet();
    }

    /**
     * Queries the {@link PersistenceService} for data with a given filter
     * criteria
     *
     * @param filter
     *            the filter to apply to the query
     * @return a time series of items
     */
    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {

        if (!checkDBAccessability()) {
            logger.warn("JDBC::query: database not connected, query aborted for item '{}'", filter.getItemName());
            return Collections.emptyList();
        }
        if (itemRegistry == null) {
            logger.error("JDBC::query: itemRegistry == null. Ignore and give up!");
            return Collections.emptyList();
        }

        // Get the item name from the filter
        // Also get the Item object so we can determine the type
        Item item = null;
        String itemName = filter.getItemName();
        logger.debug("JDBC::query: item is {}", itemName);
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e1) {
            logger.error("JDBC::query: unable to get item for itemName: '{}'. Ignore and give up!", itemName);
            return Collections.emptyList();
        }

        if (item instanceof GroupItem) {
            // For Group Item is BaseItem needed to get correct Type of Value.
            item = GroupItem.class.cast(item).getBaseItem();
            logger.debug("JDBC::query: item is instanceof GroupItem '{}'", itemName);
            if (item == null) {
                logger.debug("JDBC::query: BaseItem of GroupItem is null. Ignore and give up!");
                return Collections.emptyList();
            }
            if (item instanceof GroupItem) {
                logger.debug("JDBC::query: BaseItem of GroupItem is a GroupItem too. Ignore and give up!");
                return Collections.emptyList();
            }
        }

        String table = sqlTables.get(itemName);
        if (table == null) {
            logger.warn(
                    "JDBC::query: unable to find table for query, no data in database for item '{}'. Current number of tables in the database: {}",
                    itemName, sqlTables.size());
            // if enabled, table will be created immediately
            logger.warn("JDBC::query: try to generate the table for item '{}'", itemName);
            table = getTable(item);
        }

        long timerStart = System.currentTimeMillis();
        List<HistoricItem> items = new ArrayList<HistoricItem>();
        items = getHistItemFilterQuery(filter, conf.getNumberDecimalcount(), table, item);

        logger.debug("JDBC::query: query for {} returned {} rows in {} ms", item.getName(), items.size(),
                System.currentTimeMillis() - timerStart);

        // Success
        errCnt = 0;
        return items;
    }

    /**
     * @{inheritDoc
     */
    public void updateConfig(Map<Object, Object> configuration) {
        logger.debug("JDBC::updateConfig");

        conf = new JdbcConfiguration(configuration);
        if (checkDBAccessability()) {
            checkDBSchema();
            // connection has been established ... initialization completed!
            initialized = true;
        } else {
            initialized = false;
        }

        logger.debug("JDBC::updateConfig: configuration complete for service={}.", getId());
    }

}
