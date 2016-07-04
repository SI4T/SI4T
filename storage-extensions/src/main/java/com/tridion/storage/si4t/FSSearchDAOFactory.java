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

import com.tridion.broker.StorageException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.filesystem.FSDAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FSSearchDAOFactory
 * 
 * An extended factory class responsible for deploying Tridion Items 
 * and indexing content. 
 * 
 * Used in case File System storage is configured in the storage layer.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class FSSearchDAOFactory extends FSDAOFactory
{
	private static final Logger LOG = LoggerFactory.getLogger(FSSearchDAOFactory.class);
	private String storageId = "";
	private final SearchIndexProcessor searchIndexProcessor = SearchIndexProcessor.getInstance();

	public FSSearchDAOFactory(String storageId, String tempFileSystemTransactionLocation)
	{
		super(storageId, tempFileSystemTransactionLocation);
		this.storageId = storageId;
	}

	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSDAOFactory#configure(com.tridion.
	 * configuration.Configuration)
	 */
	@Override
	public void configure(Configuration configuration) throws ConfigurationException
	{
		super.configure(configuration);
		searchIndexProcessor.configureStorageInstance(storageId, configuration);
		LOG.debug("Instances of Search Index: ");
		searchIndexProcessor.logSearchIndexInstances();
	}

	/*
	 * Overridden entry point for Tridion deploy commits
	 * (non-Javadoc)
	 * @see
	 * com.tridion.storage.filesystem.FSDAOFactory#commitTransaction(java.lang
	 * .String)
	 */
	@Override
	public void commitTransaction(String transactionId) throws StorageException
	{
		try
		{
			LOG.info("Start committing transaction: " + transactionId);
			long start = System.currentTimeMillis();
			super.commitTransaction(transactionId);
			long searchStart = System.currentTimeMillis();
			LOG.debug("Commit Indexing Start");
			searchIndexProcessor.triggerIndexing(transactionId, this.storageId);
			LOG.info("End committing transaction: " + transactionId);
			LOG.info("Committing Search took: " + (System.currentTimeMillis() - searchStart) + " ms.");
			LOG.info("Total Commit Time was: " + (System.currentTimeMillis() - start) + " ms.");
		}
		catch (StorageException e)
		{
			this.logException(e);
			throw e;
		}
		catch (IndexingException e)
		{
			this.logException(e);
			throw new StorageException(e);

		}
		finally
		{
			SearchIndexProcessor.debugLogRegister();
			SearchIndexProcessor.cleanupRegister(transactionId);
		}
	}

	private void logException(Exception e)
	{
		LOG.error(e.getMessage());
		LOG.error(Utils.stacktraceToString(e.getStackTrace()));
	}

	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSDAOFactory#shutdownFactory()
	 */
	@Override
	public void shutdownFactory()
	{
		searchIndexProcessor.shutDownFactory(storageId);
		super.shutdownFactory();
	}
}
