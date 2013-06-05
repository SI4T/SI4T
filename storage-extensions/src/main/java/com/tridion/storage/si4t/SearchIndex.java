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

import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.si4t.SearchIndexData;

/**
 * SearchIndex.
 * 
 * Interface which is used to inject search index implementations in 
 * the factory classes.
 * 
 * Configured classes, which are configured in cd_storage_conf.xml,
 * based on this interface are called by the configured factory class upon
 * commit of Tridion item deployment.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public interface SearchIndex
{

	/**
	 * Configure.
	 * 
	 * Method to load the Indexer configuration element. This element has to be placed
	 * inside each Storage node in cd_storage_conf.xml for which indexing has to be
	 * enabled. 
	 * 
	 * @param configuration the Storage node configuration element
	 * @throws ConfigurationException
	 */
	public void configure(Configuration configuration) throws ConfigurationException;

	/**
	 * Adds a SearchIndexData item to be indexed.
	 * 
	 * @param data the data
	 * @throws IndexingException the indexing exception
	 */
	public void addItemToIndex(SearchIndexData data) throws IndexingException;

	/**
	 * Removes the item from index.
	 * 
	 * @param data the data
	 * @throws IndexingException the indexing exception
	 */
	public void removeItemFromIndex(BaseIndexData data) throws IndexingException;

	/**
	 * Update an item in the search index.
	 * 
	 * @param data the data
	 * @throws IndexingException the indexing exception
	 */
	public void updateItemInIndex(SearchIndexData data) throws IndexingException;

	/**
	 * Adds a binary item to be indexed.
	 * 
	 * @param data the data
	 * @throws IndexingException the indexing exception
	 */
	public void addBinaryToIndex(BinaryIndexData data) throws IndexingException;

	/**
	 * Removes the binary from index.
	 * 
	 * @param data the data
	 * @throws IndexingException the indexing exception
	 */
	public void removeBinaryFromIndex(BaseIndexData data) throws IndexingException;

	/**
	 * Commit. 
	 * 
	 * Handles the actual sending of to be indexed or removed items from a search index.
	 * 
	 * @param publicationId the publication id
	 * @throws IndexingException the indexing exception
	 */
	public void commit(String publicationId) throws IndexingException;

	/**
	 * Destroy.
	 * 
	 * Should destroy any open clients to free resources.
	 */
	public void destroy();
}
