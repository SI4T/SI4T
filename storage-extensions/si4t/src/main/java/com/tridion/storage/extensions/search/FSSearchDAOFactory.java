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

package com.tridion.storage.extensions.search;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tridion.broker.StorageException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.filesystem.FSDAOFactory;

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
	private Logger log = LoggerFactory.getLogger(FSSearchDAOFactory.class);
	private String storageId = "";
	private SearchIndexProcessor _processor = SearchIndexProcessor.getInstance();

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
		_processor.configureStorageInstance(storageId, configuration);
		log.debug("Processor instance number: " + _processor.getInstanceNumber());
		log.debug("Instances of Search Index: ");
		_processor.logSearchIndexInstances();
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
			log.info("Start committing transaction: " + transactionId);
			long start = System.currentTimeMillis();
			super.commitTransaction(transactionId);
			long searchStart = System.currentTimeMillis();
			log.debug("Commit Indexing Start");
			_processor.triggerIndexing(transactionId);
			log.info("End committing transaction: " + transactionId);
			log.info("Committing Search took: " + (System.currentTimeMillis() - searchStart) + " ms.");
			log.info("Total Commit Time was: " + (System.currentTimeMillis() - start) + " ms.");
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
		catch (ClassNotFoundException e)
		{
			this.logException(e);
			throw new StorageException(e);
		}
		catch (InstantiationException e)
		{
			this.logException(e);
			throw new StorageException(e);
		}
		catch (IllegalAccessException e)
		{
			this.logException(e);
			throw new StorageException(e);
		}
		catch (ConfigurationException e)
		{
			this.logException(e);
			throw new StorageException(e);
		}
		catch (ParseException e)
		{
			this.logException(e);
			throw new StorageException(e);
		}

		finally
		{
			SearchIndexProcessor.debugLogRegister(log);
			SearchIndexProcessor.cleanupRegister(transactionId, log);
		}
	}

	private void logException(Exception e)
	{
		log.error(e.getMessage());
		log.error(Utils.stacktraceToString(e.getStackTrace()));
	}

	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSDAOFactory#shutdownFactory()
	 */
	@Override
	public void shutdownFactory()
	{
		_processor.shutDownFactory(storageId);
		super.shutdownFactory();
	}
}
