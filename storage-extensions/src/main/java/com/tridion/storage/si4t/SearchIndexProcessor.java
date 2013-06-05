/**
 * Copyright 2011-2013 Radagio & SDL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridion.storage.si4t;

import java.text.ParseException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;

/**
 * SearchIndexProcessor.
 * 
 * Singleton which processes incoming search actions.
 * 
 * Is used in all overridden Tridion factory classes
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */

public final class SearchIndexProcessor
{

	private static String INDEXER_NODE = "Indexer";
	private static String INDEXER_CLASS_ATTRIBUTE = "Class";
	private Logger log = LoggerFactory.getLogger(SearchIndexProcessor.class);
	private static ConcurrentHashMap<String, Configuration> indexerConfiguration = new ConcurrentHashMap<String, Configuration>();
	private ConcurrentHashMap<String, SearchIndex> searchIndexer = new ConcurrentHashMap<String, SearchIndex>();
	private static ConcurrentHashMap<String, ConcurrentHashMap<String, BaseIndexData>> notificationRegister = new ConcurrentHashMap<String, ConcurrentHashMap<String, BaseIndexData>>();
	// Test variable
	private static int instanceNumber = 0;
	// private constructor to prevent normal instantiation
	private SearchIndexProcessor()
	{
	}

	/**
	 * SingletonHolder.
	 * 
	 * @author R.S. Kempees
	 * @version 1.20
	 * @since 1.00
	 */
	private static class SingletonHolder
	{
		public static final SearchIndexProcessor INSTANCE = new SearchIndexProcessor();
	}

	/**
	 * Gets the single instance of SearchIndexProcessor.
	 * 
	 * @return single instance of SearchIndexProcessor
	 */
	public static SearchIndexProcessor getInstance()
	{
		instanceNumber++;
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Configure storage instance.
	 * 
	 * @param storageId
	 * @param configuration
	 * @throws ConfigurationException
	 */
	public void configureStorageInstance(String storageId, Configuration configuration) throws ConfigurationException
	{
		log.info("Configuration is: " + configuration.toString());
		indexerConfiguration.put(storageId, configuration);
		setSearchIndexClient(storageId);
	}

	/**
	 * Sets the search index client.
	 * 
	 * @param storageId
	 * @throws ConfigurationException
	 */
	private void setSearchIndexClient(String storageId) throws ConfigurationException
	{
		if (indexerConfiguration != null)
		{
			String searchIndexImplementation = indexerConfiguration.get(storageId).getChild(INDEXER_NODE).getAttribute(INDEXER_CLASS_ATTRIBUTE);
			if (!Utils.StringIsNullOrEmpty(searchIndexImplementation))
			{
				log.info("Using: " + searchIndexImplementation + " as search index class for storageId: " + storageId);

				this.loadIndexer(storageId, searchIndexImplementation, indexerConfiguration.get(storageId));
				return;
			}
			throw new ConfigurationException("Could not find SearchIndex class. Please add the Class=package.class attribute");
		}
		throw new ConfigurationException("Could not find Indexer configuration node. Please add the indexing class. Minimal format is: <Indexer Class=\"package.classname\" />");
	}

	/**
	 * Load indexer.
	 * 
	 * @param storageId
	 * @param searchIndexImplementation
	 * @param indexerConfiguration
	 * @throws ConfigurationException
	 */
	private void loadIndexer(String storageId, String searchIndexImplementation, Configuration indexerConfiguration) throws ConfigurationException
	{
		if (searchIndexer.get(storageId) == null)
		{
			log.info("Loading " + searchIndexImplementation);

			ClassLoader classLoader = this.getClass().getClassLoader();
			Class<?> indexerClass;
			SearchIndex s = null;
			try
			{
				indexerClass = classLoader.loadClass(searchIndexImplementation);
				s = (SearchIndex) indexerClass.newInstance();
				s.configure(indexerConfiguration);
				log.info("Configured: " + searchIndexImplementation);

				if (searchIndexer.containsKey(storageId))
				{
					log.warn("This storage instance already has a configured and loaded Search Index client. Probably storage configuration is wrong.");
					log.warn("Reloading search index instance");
					searchIndexer.remove(storageId);
				}
				searchIndexer.put(storageId, s);
				log.info("Loaded: " + searchIndexImplementation);
			}
			catch (ClassNotFoundException e)
			{
				this.logException(e);
				throw new ConfigurationException("Could not find class: " + searchIndexImplementation, e);
			}
			catch (InstantiationException e)
			{
				this.logException(e);
				throw new ConfigurationException("Could instantiate class: " + searchIndexImplementation, e);
			}
			catch (IllegalAccessException e)
			{
				this.logException(e);
				throw new ConfigurationException("IllegalAccessException: " + searchIndexImplementation, e);
			}
		}
	}

	/**
	 * Gets the indexer configuration.
	 * 
	 * @param storageId
	 * @return the indexer configuration node
	 * @throws ConfigurationException
	 */
	public static Configuration getIndexerConfiguration(String storageId) throws ConfigurationException
	{
		if (indexerConfiguration != null)
		{
			return indexerConfiguration.get(storageId);
		}
		throw new ConfigurationException("Indexer configuration not set.");
	}

	/**
	 * Register search action.
	 * 
	 * @param transactionId
	 * @param indexData
	 * @param _log
	 */
	public static void registerAction(String transactionId, BaseIndexData indexData, Logger _log)
	{
		_log.info("Registering " + indexData.getUniqueIndexId() + ", for: " + indexData.getAction());

		if (!notificationRegister.containsKey(transactionId))
		{
			notificationRegister.put(transactionId, new ConcurrentHashMap<String, BaseIndexData>());
		}
		ConcurrentHashMap<String, BaseIndexData> transactionActions = notificationRegister.get(transactionId);

		if (!transactionActions.containsKey(indexData.getUniqueIndexId()))
		{
			transactionActions.put(indexData.getUniqueIndexId(), indexData);
		}

		else
		{
			// Special case where a publish transaction contains a renamed file
			// plus a file
			// with the same name as the renamed file's old name, we ensure that
			// it is not
			// removed, but only re-persisted (a rename will trigger a remove
			// and a persist)

			if (indexData.getAction() == FactoryAction.PERSIST || indexData.getAction() == FactoryAction.UPDATE)
			{
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
				_log.debug(">>> Special case.");
				transactionActions.put(indexData.getUniqueIndexId(), indexData);
			}
		}
	}

	/**
	 * Trigger indexing.
	 * 
	 * @param transactionId
	 * @throws IndexingException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ConfigurationException
	 * @throws ParseException
	 */
	public void triggerIndexing(String transactionId) throws IndexingException, ClassNotFoundException, InstantiationException, IllegalAccessException, ConfigurationException, ParseException
	{
		if (notificationRegister.containsKey(transactionId))
		{
			log.info("Triggering Indexing for transaction: " + transactionId);

			ConcurrentHashMap<String, BaseIndexData> actions = notificationRegister.get(transactionId);

			SearchIndex s = null;
			String pubId = "";
			for (String itemId : actions.keySet())
			{
				BaseIndexData data = actions.get(itemId);
				log.trace("Data is: " + data.toString());
				log.debug("Obtaining SearchIndex class for: " + data.getStorageId());
				s = this.searchIndexer.get(data.getStorageId());
				if (s == null)
				{
					throw new IndexingException("Could not load SearchIndexer. Check your configuration.");
				}
				log.debug(data.getStorageId() + "::" + s.getClass().getName() + "::" + indexerConfiguration.get(data.getStorageId()).toString());
				processAction(s, actions, itemId);

				pubId = data.getPublicationItemId();
				log.debug("Trigger indexing for item: " + itemId + ", action: " + data.getAction() + ", storageId: " + data.getStorageId());
			}

			log.debug("Setting Publication Id to: " + pubId);
			if (s != null)
			{
				s.commit(pubId);
			}
		}
	}

	public static void debugLogRegister(Logger log)
	{
		if (log.isDebugEnabled())
		{
			log.debug("Register currently contains:");
			for (Entry<String, ConcurrentHashMap<String, BaseIndexData>> x : notificationRegister.entrySet())
			{
				log.debug(x.getKey());
				for (Entry<String, BaseIndexData> c : x.getValue().entrySet())
				{
					log.trace(c.getKey() + ":: " + c.getValue().toString());
				}
			}
		}
	}

	public static void cleanupRegister(String transactionId, Logger log)
	{
		log.debug("Clearing register for transaction:" + transactionId);
		if (notificationRegister.containsKey(transactionId))
		{
			notificationRegister.remove(transactionId);
		}
	}

	private void processAction(SearchIndex s, ConcurrentHashMap<String, BaseIndexData> actions, String itemId) throws IndexingException
	{

		BaseIndexData data = actions.get(itemId);
		switch (data.getIndexType())
		{
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

	private void processBinaryAction(SearchIndex s, BaseIndexData data) throws IndexingException
	{
		log.trace("Search Data type is: " + data.getClass().getName());
		switch (data.getAction())
		{
			case PERSIST:
				s.addBinaryToIndex((BinaryIndexData)data);
				break;
			case REMOVE:
				log.debug("Removing!");
				s.removeBinaryFromIndex(data);
				break;
			case UPDATE:
				s.addBinaryToIndex((BinaryIndexData)data);
				break;
			default:
				break;
		}
	}

	public void shutDownFactory(String storageId)
	{
		if (searchIndexer != null)
		{
			log.info("Destroying indexer instance for: " + storageId);
			SearchIndex s = searchIndexer.get(storageId);
			if (s != null)
			{
				s.destroy();
			}
			searchIndexer.clear();
		}
	}

	private void processItemAction(SearchIndex s, BaseIndexData data) throws IndexingException
	{
		switch (data.getAction())
		{
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

	// Test method
	public int getInstanceNumber()
	{
		return instanceNumber;
	}

	/**
	 * Log search index instances.
	 */
	public void logSearchIndexInstances()
	{
		if (log.isTraceEnabled())
		{
			for (Entry<String, SearchIndex> e : searchIndexer.entrySet())
			{
				log.trace(e.getKey() + "::" + e.getValue().getClass().getName());
			}
		}
	}

	private void logException(Exception e)
	{
		log.error(e.getMessage());
		log.error(Utils.stacktraceToString(e.getStackTrace()));
	}

}
