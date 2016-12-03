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
package com.tridion.storage.si4t.dao;

import com.tridion.broker.StorageException;
import com.tridion.data.CharacterData;
import com.tridion.storage.dao.PageDAO;
import com.tridion.storage.persistence.JPAPageDAO;
import com.tridion.storage.si4t.FactoryAction;
import com.tridion.storage.si4t.IndexType;
import com.tridion.storage.si4t.TridionBaseItemProcessor;
import com.tridion.storage.si4t.TridionPublishableItemProcessor;
import com.tridion.storage.si4t.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.IOException;

// TODO: Auto-generated Javadoc

/**
 * JPASearchPageDAO.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
@Component("JPASearchPageDAO")
@Scope("prototype")
public class JPASearchPageDAO extends JPAPageDAO implements PageDAO
{
	private Logger log = LoggerFactory.getLogger(JPASearchPageDAO.class);
	private String storageId;
	
	public JPASearchPageDAO(String storageId, EntityManagerFactory entityManagerFactory, EntityManager entityManager, String storageName)
	{
		super(storageId, entityManagerFactory, entityManager, storageName);
		this.storageId = storageId;
		log.debug("JPASearchPageDAO init. (EM)");

	}

	public JPASearchPageDAO(String storageId, EntityManagerFactory entityManagerFactory, String storageName)
	{
		super(storageId, entityManagerFactory, storageName);
		this.storageId = storageId;
		log.debug("JPASearchPageDAO init.");
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.persistence.JPAPageDAO#create(com.tridion.data.CharacterData, java.lang.String)
	 */
	public void create(CharacterData page, String relativePath) throws StorageException
	{
		log.debug("Create.");
		TridionPublishableItemProcessor tp;
		try
		{
			tp = new TridionPublishableItemProcessor(
					page.getString(),
					FactoryAction.UPDATE,
					IndexType.PAGE,
					Integer.toString(page.getPublicationId()),
					"tcm:" + page.getPublicationId() +"-" + page.getId() + "-64"
					, this.storageId);
			CharacterData c = tp.processPageSource(page);
			if (c != null)
			{
				super.create(c, relativePath);
			}
			else
			{
				log.error("Error processing page: " + relativePath + ", proceeding with deployment of original page");
				super.create(page, relativePath);
			}
		}
		catch (IOException e)
		{
			log.error(Utils.stacktraceToString(e.getStackTrace()));
			throw new StorageException("IO Exception: " + e.getLocalizedMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSPageDAO#update(com.tridion.data.CharacterData, java.lang.String, java.lang.String)
	 */
	// Note: An update triggers a create always. So this might not be needed
	@Override
	public void update(CharacterData page, String originalRelativePath, String newRelativePath) throws StorageException
	{
		log.debug("Update. Orgpath=" + originalRelativePath);
		
		TridionPublishableItemProcessor tp;
		try
		{
			tp = new TridionPublishableItemProcessor(
					page.getString(),
					FactoryAction.UPDATE,
					IndexType.PAGE,
					Integer.toString(page.getPublicationId()),
					"tcm:" + page.getPublicationId() +"-" + page.getId() + "-64"
					, this.storageId);
			CharacterData c = tp.processPageSource(page);
			if (c != null)
			{
				super.update(c, originalRelativePath, newRelativePath);
			}
			else
			{
				log.error("Error processing page: " + newRelativePath + ", proceeding with deployment of original page");
				super.update(page, originalRelativePath, newRelativePath);
			}
		}
		catch (IOException e)
		{
			log.error(Utils.stacktraceToString(e.getStackTrace()));
			throw new StorageException("IO Exception: " + e.getLocalizedMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.persistence.JPAPageDAO#remove(int, int, java.lang.String)
	 */
	@Override
	public void remove(int publicationId, int pageId, String relativePath) throws StorageException
	{
		super.remove(publicationId, pageId, relativePath);
		TridionBaseItemProcessor.registerItemRemoval(
				"tcm:"+publicationId+"-"+pageId+"-64", IndexType.PAGE, log, Integer.toString(publicationId), this.storageId);
	}
}
