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
import com.tridion.storage.persistence.JPADAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.text.ParseException;

/**
 * An extended factory class responsible for deploying Tridion Items 
 * and indexing content. 
 * 
 * Used in case Tridion JPA storage is configured in the storage layer. 
 * This class hooks into the Spring Loader, which is used by Tridion to
 * create JPA based DAO objects.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
@Component("JPASearchDAOFactory")
@Scope("singleton")
public class JPASearchDAOFactory extends JPADAOFactory implements ApplicationContextAware
{
	private Logger log = LoggerFactory.getLogger(JPASearchDAOFactory.class);
	private String storageId = "";
	private SearchIndexProcessor _processor;

	/*
	 * Spring specific, thanks to DN
	 */
	private static ApplicationContext APPLICATION_CONTEXT;

	public JPASearchDAOFactory()
	{
		super(null, "MSSQL");// not important what we sent. This instance is
								// never going to be used
		log.trace("Spring Constructor init.");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.tridion.storage.persistence.JPADAOFactory#configureBundle(com.tridion
	 * .configuration.Configuration)
	 */
	public void configureBundle(Configuration storageDAOBundleConfiguration) throws ConfigurationException
	{
		// first set the right value for the private field called
		// 'applicationContext'.
		try
		{
			setPrivateField(this, "applicationContext", APPLICATION_CONTEXT, log);
		}
		catch (IllegalAccessException e)
		{
			log.error(e.getMessage());
		}

		// configure the bundle like we normally do
		super.configureBundle(storageDAOBundleConfiguration);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.springframework.context.ApplicationContextAware#setApplicationContext
	 * (org.springframework.context.ApplicationContext)
	 */

	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
	{
		APPLICATION_CONTEXT = applicationContext;
		log.trace("Setting app context from spring.");

	}

	private static void setPrivateField(final Object fieldOwner, final String fieldName, final Object value, Logger log) throws IllegalAccessException
	{
		final Field privateField = getPrivateFieldRec(fieldOwner.getClass(), fieldName, log);

		if (privateField != null)
		{

			final boolean accessible = privateField.isAccessible();
			privateField.setAccessible(true);

			privateField.set(fieldOwner, value);

			privateField.setAccessible(accessible);
		}
	}

	private static Field getPrivateFieldRec(final Class<?> clazz, final String fieldName, Logger log)
	{
		for (Field field : clazz.getDeclaredFields())
		{
			if (fieldName.equals(field.getName()))
			{
				return field;
			}
		}
		final Class<?> superClazz = clazz.getSuperclass();

		if (superClazz != null)
		{
			return getPrivateFieldRec(superClazz, fieldName, log);
		}

		return null;
	}

	public JPASearchDAOFactory(String storageId, String dialect)
	{
		super(storageId, dialect);
		// Needed to correctly instantiate a searchindexer
		this.storageId = storageId;
	}

	/*
	 * End Spring specific
	 */
	/*
	 * Extension specific
	 */
	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.persistence.JPADAOFactory#configure(com.tridion.
	 * configuration.Configuration)
	 */
	@Override
	public void configure(Configuration storageDAOBundleConfiguration) throws ConfigurationException
	{
		super.configure(storageDAOBundleConfiguration);
		// Get the instance here, because Spring instantiates the JPADAOFactory
		// twice.
		log.trace("Fetching SearchProcessor instance.");
		_processor = SearchIndexProcessor.getInstance();
		_processor.configureStorageInstance(storageId, storageDAOBundleConfiguration);
		log.trace("Processor instance number: " + _processor.getInstanceNumber());
		log.trace("Instances of Search Index: ");
		_processor.logSearchIndexInstances();
	}

	/*
	 * Overridden entry point for Tridion deploy commits
	 * 
	 * (non-Javadoc)
	 * @see
	 * com.tridion.storage.persistence.JPADAOFactory#commitTransaction(java.
	 * lang.String)
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
			_processor.triggerIndexing(transactionId, this.storageId);
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

	@Override
	public void shutdownFactory()
	{
		_processor.shutDownFactory(storageId);
		super.shutdownFactory();
	}
}
