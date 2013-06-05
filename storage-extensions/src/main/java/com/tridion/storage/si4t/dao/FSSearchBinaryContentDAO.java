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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tridion.broker.StorageException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.BinaryContent;
import com.tridion.storage.filesystem.FSBinaryContentDAO;
import com.tridion.storage.filesystem.FSEntityManager;
import com.tridion.storage.si4t.IndexType;
import com.tridion.storage.si4t.SearchIndexProcessor;
import com.tridion.storage.si4t.TridionBinaryProcessor;
import com.tridion.storage.si4t.Utils;

/**
 * FSSearchBinaryContentDAO.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class FSSearchBinaryContentDAO extends FSBinaryContentDAO
{
	private Logger log = LoggerFactory.getLogger(FSSearchBinaryContentDAO.class);
	private static String DOC_EXTENSIONS_ATTRIBUTE = "DocExtensions";
	private static String INDEXER_NODE = "Indexer";
	private String[] docExtensionsToIndex = null;
	private Configuration configuration;
	private String storageId;
	
	public FSSearchBinaryContentDAO(String storageId, String storageName, File storageLocation, FSEntityManager entityManager) throws ConfigurationException
	{
		super(storageId, storageName, storageLocation, entityManager);
		log.trace("FSSearchBinaryContentDAO init. (EM)");

		this.configuration = SearchIndexProcessor.getIndexerConfiguration(storageId);
		this.storageId = storageId;
		this.setIndexableFileExtensions();	
	}

	public FSSearchBinaryContentDAO(String storageId, String storageName,	File storageLocation)
	{
		super(storageId, storageName, storageLocation);
		this.storageId = storageId;
		log.trace("FSSearchBinaryContentDAO init.");
	}
	
	private void setIndexableFileExtensions() throws ConfigurationException
	{
		if (configuration != null)
		{
			log.debug("Configuration: " + configuration.toString());
			String extensions = configuration.getChild(INDEXER_NODE).getAttribute(DOC_EXTENSIONS_ATTRIBUTE);
			if (!Utils.StringIsNullOrEmpty(extensions))
			{
				if (extensions.indexOf(",") > 0)
				{
					this.docExtensionsToIndex = extensions.split(",");
				}
				else
				{
					this.docExtensionsToIndex = new String[] { extensions };
				}
				return;
			}
		}
		throw new ConfigurationException("Indexable file extensions are not configured.");
	}
	
	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSBinaryContentDAO#create(com.tridion.storage.BinaryContent, java.lang.String)
	 */
	@Override
	public void create(BinaryContent binaryContent, String relativePath) throws StorageException
	{
		super.create(binaryContent, relativePath);		

		if (Utils.StringArrayContains(docExtensionsToIndex, Utils.GetBinaryFileExtension(relativePath)))
		{
			log.info("Found a binary to index (Create): " + relativePath);
			TridionBinaryProcessor.registerAddition(binaryContent, relativePath, relativePath, log, this.storageId);
		}
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSBinaryContentDAO#update(com.tridion.storage.BinaryContent, java.lang.String, java.lang.String)
	 */
	@Override
	public void update(BinaryContent binaryContent, String originalRelativePath, String newRelativePath) throws StorageException
	{
		super.update(binaryContent, originalRelativePath, newRelativePath);
		log.info("Checking update for: " + originalRelativePath);
		
		String fileExtension = Utils.GetBinaryFileExtension(newRelativePath);
		
		if (Utils.StringArrayContains(docExtensionsToIndex,fileExtension.toLowerCase()))
		{
			log.info("Found a binary to index (Update): " + newRelativePath);
			TridionBinaryProcessor.registerAddition(binaryContent, originalRelativePath, newRelativePath, log, this.storageId);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSBinaryContentDAO#remove(int, int, java.lang.String, java.lang.String)
	 */
	@Override
	public void remove(int publicationId, int binaryId, String variantId, String relativePath) throws StorageException
	{
		super.remove(publicationId, relativePath);
		TridionBinaryProcessor.registerItemRemoval("binary:" + publicationId + "-" + binaryId, IndexType.BINARY, log, Integer.toString(publicationId), this.storageId);
	}
}
