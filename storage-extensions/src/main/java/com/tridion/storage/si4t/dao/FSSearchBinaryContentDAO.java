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
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.BinaryContent;
import com.tridion.storage.filesystem.FSBinaryContentDAO;
import com.tridion.storage.filesystem.FSEntityManager;
import com.tridion.storage.si4t.IndexType;
import com.tridion.storage.si4t.SearchIndexProcessor;
import com.tridion.storage.si4t.TridionBinaryProcessor;
import com.tridion.storage.si4t.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * FSSearchBinaryContentDAO.
 * 
 * @author R.S. Kempees
 */
public class FSSearchBinaryContentDAO extends FSBinaryContentDAO
{
	private static final Logger LOG = LoggerFactory.getLogger(FSSearchBinaryContentDAO.class);
	private static final String DOC_EXTENSIONS_ATTRIBUTE = "DocExtensions";
	private static final String INDEXER_NODE = "Indexer";
	private String[] docExtensionsToIndex = null;
	private Configuration configuration;
	private String storageId;
	
	public FSSearchBinaryContentDAO(String storageId, String storageName, File storageLocation, FSEntityManager entityManager) throws ConfigurationException
	{
		super(storageId, storageName, storageLocation, entityManager);

		LOG.trace("FSSearchBinaryContentDAO init. (EM)");

		this.configuration = SearchIndexProcessor.getIndexerConfiguration(storageId);
		this.storageId = storageId;
		this.setIndexableFileExtensions();

	}

	public FSSearchBinaryContentDAO(String storageId, String storageName,	File storageLocation)
	{
		super(storageId, storageName, storageLocation);
		this.storageId = storageId;
		LOG.trace("FSSearchBinaryContentDAO init.");
	}
	
	private void setIndexableFileExtensions() throws ConfigurationException
	{
		if (configuration != null)
		{
			LOG.debug("Configuration: " + configuration.toString());
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
			}
		}
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
			LOG.info("Found a binary to index (Create): " + relativePath);
			TridionBinaryProcessor.registerAddition(binaryContent, relativePath, relativePath, this.storageId);
		}
	}

	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSBinaryContentDAO#update(com.tridion.storage.BinaryContent, java.lang.String, java.lang.String)
	 */
	@Override
	public void update(BinaryContent binaryContent, String originalRelativePath, String newRelativePath) throws StorageException
	{
		super.update(binaryContent, originalRelativePath, newRelativePath);
		LOG.info("Checking update for: " + originalRelativePath);
		
		String fileExtension = Utils.GetBinaryFileExtension(newRelativePath);
		
		if (Utils.StringArrayContains(docExtensionsToIndex,fileExtension.toLowerCase()))
		{
			LOG.info("Found a binary to index (Update): " + newRelativePath);
			TridionBinaryProcessor.registerAddition(binaryContent, originalRelativePath, newRelativePath, this.storageId);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.tridion.storage.filesystem.FSBinaryContentDAO#remove(int, int, java.lang.String, java.lang.String)
	 */
	@Override
	public void remove(int publicationId, int binaryId, String variantId, String relativePath) throws StorageException
	{
		super.remove(publicationId, relativePath);
		TridionBinaryProcessor.registerItemRemoval("binary:" + publicationId + "-" + binaryId, IndexType.BINARY, LOG, Integer.toString(publicationId), this.storageId);
	}
}
