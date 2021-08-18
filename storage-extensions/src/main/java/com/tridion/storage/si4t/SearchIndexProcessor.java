/**
 * Copyright 2011-2013 Radagio & SDL
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridion.storage.si4t;

import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SearchIndexProcessor.
 * <p/>
 * Singleton which processes incoming search actions.
 * <p/>
 * Is used in all overridden Tridion factory classes
 *
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */

public final class SearchIndexProcessor {

    private static final String INDEXER_NODE = "Indexer";
    private static final String INDEXER_CLASS_ATTRIBUTE = "Class";
    private static final Logger LOG = LoggerFactory.getLogger(SearchIndexProcessor.class);
    // Stores 1 SearchIndex handler per storage ID.
    private static final ConcurrentHashMap<String, Class<? extends SearchIndex>> INDEXER_CLASSES =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Configuration> INDEXER_CONFIGURATION =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, BaseIndexData>> NOTIFICATION_REGISTER =
            new ConcurrentHashMap<>();

    // private constructor to prevent normal instantiation
    private SearchIndexProcessor() {
    }

    private static class SingletonHolder {
        public static final SearchIndexProcessor INSTANCE = new SearchIndexProcessor();
    }

    /**
     * Gets the single instance of SearchIndexProcessor.
     *
     * @return single instance of SearchIndexProcessor
     */
    public static SearchIndexProcessor getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Configure storage instance.
     *
     * @param storageId     The configured storage Id
     * @param configuration The entire
     * @throws ConfigurationException a config exception.
     */
    public void configureStorageInstance(String storageId, Configuration configuration) throws ConfigurationException {
        LOG.info("Configuration is: " + configuration.toString());
        INDEXER_CONFIGURATION.put(storageId, configuration);
        setSearchIndexClient(storageId);
    }

    /**
     * Sets the search index client.
     *
     * @param storageId The configured storage Id
     * @throws ConfigurationException a config exception.
     */
    private void setSearchIndexClient(String storageId) throws ConfigurationException {
        String searchIndexImplementation =
                INDEXER_CONFIGURATION.get(storageId).getChild(INDEXER_NODE).getAttribute(INDEXER_CLASS_ATTRIBUTE);
        if (!Utils.StringIsNullOrEmpty(searchIndexImplementation)) {
            LOG.info("Using: " + searchIndexImplementation + " as search index class for storageId: " + storageId);


            this.storeIndexerClassForStorageId(storageId, searchIndexImplementation);
            return;
        }
        throw new ConfigurationException(
                "Could not find SearchIndex class. Please add the Class=package.class attribute");
    }

    /**
     * Stores the Indexer class for a storage Id.
     *
     * @param storageId                 The configured storage Id.
     * @param searchIndexImplementation he configured concrete SearchIndex implementation class.
     */
    private void storeIndexerClassForStorageId(String storageId, String searchIndexImplementation)
            throws ConfigurationException {
        if (INDEXER_CLASSES.get(storageId) == null) {
            LOG.info("Loading " + searchIndexImplementation);

            ClassLoader classLoader = this.getClass().getClassLoader();
            Class<? extends SearchIndex> indexerClass;

            try {

                indexerClass = (Class<? extends SearchIndex>) classLoader.loadClass(searchIndexImplementation);


                // is probably useless code.
                if (INDEXER_CLASSES.containsKey(storageId)) {
                    LOG.warn(
                            "This storage instance has an Indexer Class present. Probably " +
                                    "storage configuration is wrong.");
                    LOG.warn("Reloading search index class.");

                    INDEXER_CLASSES.remove(storageId);
                }
                INDEXER_CLASSES.put(storageId, indexerClass);
//				searchIndex = (SearchIndex) indexerClass.newInstance();
//				searchIndex.configure(indexerConfiguration);
                LOG.info("Stored: {}, for storage Id: {} ", searchIndexImplementation, storageId);


                LOG.info("Loaded: " + searchIndexImplementation);
            } catch (ClassNotFoundException e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new ConfigurationException("Could not find class: " + searchIndexImplementation, e);
            }
        }
    }

    /**
     * Instantiates an Indexer for the given storage Id.
     *
     * @param storageId The configured storage Id
     * @throws IndexingException error thrown inside a commit.
     */
    private SearchIndex loadIndexer(String storageId) throws IndexingException {
        if (!INDEXER_CLASSES.containsKey(storageId) || INDEXER_CLASSES.get(storageId) == null) {
            throw new IndexingException("No Indexer class found for the storage Id: " + storageId);
        }

        if (!INDEXER_CONFIGURATION.containsKey(storageId) || INDEXER_CONFIGURATION.get(storageId) == null) {
            throw new IndexingException("No configuration found for Indexer for storage Id: " + storageId);
        }

        Class<? extends SearchIndex> indexerClass = INDEXER_CLASSES.get(storageId);
        String indexerClassName = indexerClass.getName();

        try {

            LOG.info("Loading {}", indexerClassName);

            SearchIndex searchIndex;
            searchIndex = indexerClass.newInstance();
            searchIndex.configure(INDEXER_CONFIGURATION.get(storageId));

            LOG.info("Configured: " + indexerClass.getName());
            return searchIndex;

        } catch (InstantiationException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new IndexingException("Could instantiate class: " + indexerClassName, e);
        } catch (IllegalAccessException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new IndexingException("IllegalAccessException: " + indexerClassName, e);
        } catch (ConfigurationException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new IndexingException("Could not load SearchIndexer. Check your configuration.", e);
        }
    }


    /**
     * Gets the indexer configuration.
     *
     * @param storageId The configured storage Id
     * @return the indexer configuration node
     * @throws ConfigurationException config exception.
     */
    public static Configuration getIndexerConfiguration(String storageId) throws ConfigurationException {

        Configuration configuration = INDEXER_CONFIGURATION.get(storageId);
        if (configuration != null) {
            return configuration;
        }
        throw new ConfigurationException("Indexer configuration not set.");
    }

    /**
     * Register search action.
     *
     * @param transactionId The local thread transaction id. Might correspond with the Tridion transaction Id
     * @param indexData     The data object to index.
     */
    public static void registerAction(String transactionId, BaseIndexData indexData) {
        LOG.info("Registering " + indexData.getUniqueIndexId() + ", for: " + indexData.getAction());

        if (!NOTIFICATION_REGISTER.containsKey(transactionId)) {
            NOTIFICATION_REGISTER.put(transactionId, new ConcurrentHashMap<>());
        }
        ConcurrentHashMap<String, BaseIndexData> transactionActions = NOTIFICATION_REGISTER.get(transactionId);


        if (!transactionActions.containsKey(indexData.getUniqueIndexId())) {
            transactionActions.put(indexData.getUniqueIndexId(), indexData);

        } else {
            // Special case where a publish transaction contains a renamed file
            // plus a file
            // with the same name as the renamed file's old name, we ensure that
            // it is not
            // removed, but only re-persisted (a rename will trigger a remove
            // and a persist)

            if (indexData.getAction() == FactoryAction.PERSIST || indexData.getAction() == FactoryAction.UPDATE) {
                // Special case where a publish transaction contains a renamed
                // file
                // plus a file
                // with the same name as the renamed file's old name, we ensure
                // that
                // it is not
                // removed, but only re-persisted (a rename will trigger a
                // remove
                // and a persist)
                // TODO: this might be removed completely.
                LOG.debug(">>> Special case.");
                transactionActions.put(indexData.getUniqueIndexId(), indexData);
            }
        }
    }

    /**
     * Trigger indexing.
     *
     * @param transactionId the Transaction Id
     * @throws IndexingException indexingException
     */
    public void triggerIndexing(String transactionId, String storageId) throws IndexingException {
        if (NOTIFICATION_REGISTER.containsKey(transactionId)) {
            LOG.info("Triggering Indexing for transaction: " + transactionId);
            LOG.info("Indexing was requested for Storage Id: " + storageId);
            ConcurrentHashMap<String, BaseIndexData> indexableItems = NOTIFICATION_REGISTER.get(transactionId);

            for (Iterator<Entry<String, BaseIndexData>> iter = indexableItems.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<String, BaseIndexData> actionEntry = iter.next();
                String itemId = actionEntry.getKey();
                BaseIndexData data = actionEntry.getValue();

                if (data.getStorageId().equalsIgnoreCase(storageId)) {
                    LOG.trace("Data is: {} ", data);
                    LOG.debug("Obtaining SearchIndex class for: " + data.getStorageId());
                    SearchIndex searchIndexer = this.loadIndexer(storageId);

                    LOG.debug(data.getStorageId() + "::" + searchIndexer.getClass().getName() + "::" +
                            INDEXER_CONFIGURATION.get(data.getStorageId()).toString());
                    try {
                        processAction(searchIndexer, indexableItems, itemId);

                        String pubId = data.getPublicationItemId();
                        LOG.debug("Trigger action for item: " + itemId + ", action: " + data.getAction() +
                                ", storageId: " + data.getStorageId());
                        LOG.debug("Setting Publication Id to: " + pubId);
                        searchIndexer.commit(pubId);
                    } finally {
                        // will trigger commits by default 10 times.
                        // remove from notification register.
                        LOG.debug(
                                "Removing + " + itemId + " for storageId: " + data.getStorageId() + " from register.");
                        // removing like this may mean that other threads running concurrently
                        // will not see this change.
                        // It is expected that one factory will run as Singleton, so this is no problem
                        // as other factories using the same notification register will not read this entry,
                        // because they have a different storageId.
                        // The main reason to remove it here, is so that other configured DAOFactories will not run
                        // it again.
                        iter.remove();

                        LOG.debug("Destroying this indexer.");
                        searchIndexer.destroy();
                    }
                } else {
                    LOG.debug(
                            "Not processing, this entry is for another factory to process. This factory belongs to {}" +
                                    " and the transaction belongs to: {}", storageId, data.getStorageId());
                }
            }
        }
    }

    public static void debugLogRegister() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Register currently contains:");
            for (Entry<String, ConcurrentHashMap<String, BaseIndexData>> x : NOTIFICATION_REGISTER.entrySet()) {
                LOG.debug(x.getKey());
                for (Entry<String, BaseIndexData> c : x.getValue().entrySet()) {
                    LOG.trace(c.getKey() + ":: " + c.getValue().toString());
                }
            }
        }
    }

    public static void cleanupRegister(String transactionId, String storageId) {
        LOG.debug("Start clearing register for transaction:" + transactionId);
        if (NOTIFICATION_REGISTER.containsKey(transactionId)) {

            ConcurrentHashMap<String, BaseIndexData> indexableItems = NOTIFICATION_REGISTER.get(transactionId);

            boolean canClear = true;
            if (indexableItems != null && !indexableItems.isEmpty()) {
                for (Entry<String, BaseIndexData> actionEntry : indexableItems.entrySet()) {
                    BaseIndexData data = actionEntry.getValue();

                    if (!data.getStorageId().equalsIgnoreCase(storageId)) {
                        canClear = false;
                        LOG.info("Not clearing out transaction yet for storageId: {}. " +
                                "There are items for another storage Id ({})", storageId, data.getStorageId());
                    }
                }
            }

            if (canClear) {
                NOTIFICATION_REGISTER.remove(transactionId);
                LOG.info("Cleared out transaction with transactionId: {}.", transactionId);
            }

        }
    }

    private void processAction(SearchIndex s, ConcurrentHashMap<String, BaseIndexData> actions, String itemId)
            throws IndexingException {

        BaseIndexData data = actions.get(itemId);
        switch (data.getIndexType()) {
            case BINARY:
                this.processBinaryAction(s, data);
                break;
            case PAGE:
                this.processItemAction(s, data);
                break;
            case COMPONENT_PRESENTATION:
                this.processItemAction(s, data);
                break;
        }
    }

    private void processBinaryAction(SearchIndex s, BaseIndexData data) throws IndexingException {
        LOG.trace("Search Data type is: " + data.getClass().getName());
        switch (data.getAction()) {
            case PERSIST:
                s.addBinaryToIndex((BinaryIndexData) data);
                break;
            case REMOVE:
                LOG.debug("Removing!");
                s.removeBinaryFromIndex(data);
                break;
            case UPDATE:
                s.addBinaryToIndex((BinaryIndexData) data);
                break;
            default:
                break;
        }
    }

    private void processItemAction(SearchIndex s, BaseIndexData data) throws IndexingException {
        switch (data.getAction()) {
            case PERSIST:
                s.addItemToIndex((SearchIndexData) data);
                break;
            case REMOVE:
                s.removeItemFromIndex(data);
                break;
            case UPDATE:
                s.updateItemInIndex((SearchIndexData) data);
                break;
        }
    }

    /**
     * Log search index instances.
     */
    public void logSearchIndexInstances() {
        if (LOG.isTraceEnabled()) {
            for (Entry<String, Class<? extends SearchIndex>> e : INDEXER_CLASSES.entrySet()) {
                LOG.trace(e.getKey() + "::" + e.getValue().getName());
            }
        }
    }
}
