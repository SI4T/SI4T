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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tridion.broker.StorageException;
import com.tridion.data.CharacterData;
import com.tridion.storage.filesystem.FSEntityManager;
import com.tridion.storage.filesystem.FSPageDAO;
import com.tridion.storage.si4t.FactoryAction;
import com.tridion.storage.si4t.IndexType;
import com.tridion.storage.si4t.TridionBaseItemProcessor;
import com.tridion.storage.si4t.TridionPublishableItemProcessor;
import com.tridion.storage.si4t.Utils;

/**
 * FSSearchPageDAO.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class FSSearchPageDAO extends FSPageDAO
{
	private Logger log = LoggerFactory.getLogger(FSSearchPageDAO.class);
	private String storageId;

	public FSSearchPageDAO(String storageId, String storageName, File storageLocation, FSEntityManager entityManager)
	{
		super(storageId, storageName, storageLocation, entityManager);
		this.storageId = storageId;
		log.debug("FSSearchPageDAO init. (EM)");
	}
	
	public FSSearchPageDAO(String storageId, String storageName, File storageLocation)
	{
		super(storageId, storageName, storageLocation);
		this.storageId = storageId;
		log.debug("FSSearchPageDAO init.");
	}
	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSPageDAO#create(com.tridion.data.CharacterData, java.lang.String)
	 * 
	 * An Update also always triggers a create
	 */
	@Override
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
					"tcm:" + page.getPublicationId() +"-" + page.getId()+"-64", this.storageId);
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
	// Note: An update triggers a create always. So this might not needed

	@Override
	public void update(CharacterData page, String originalRelativePath, String newRelativePath) throws StorageException
	{
		super.update(page, originalRelativePath, newRelativePath);
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSPageDAO#remove(int, int, java.lang.String)
	 */
	@Override
	public void remove(int publicationId, int pageId, String relativePath) throws StorageException
	{
		super.remove(publicationId, relativePath);
		TridionBaseItemProcessor.registerItemRemoval(
				"tcm:"+publicationId+"-"+pageId+"-64", IndexType.PAGE, log, Integer.toString(publicationId), this.storageId);
	}
}
