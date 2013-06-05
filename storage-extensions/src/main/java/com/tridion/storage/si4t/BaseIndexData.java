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

/**
 * BaseIndexData.
 * 
 * POJO which holds all data necessary for an indexing action
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class BaseIndexData
{

	protected FactoryAction _action;
	protected String _uniqueIndexId;
	protected String _storageId;
	protected IndexType _itemType;
	protected String _publicationId;

	public BaseIndexData(FactoryAction action, IndexType itemType, String publicationId, String storageId)
	{
		this._action = action;
		this._itemType = itemType;
		this._publicationId = publicationId;
		this._storageId = storageId;
	}

	/**
	 * Gets the storage id configured in cd_storage_conf_xml.
	 * 
	 * @return the storage id
	 */
	public String getStorageId()
	{
		return this._storageId;
	}

	/**
	 * Sets the Indexing action.
	 * 
	 * @param _action the new action
	 */
	public void setAction(FactoryAction _action)
	{
		this._action = _action;
	}

	/**
	 * Gets the action.
	 * 
	 * @return the action
	 */
	public FactoryAction getAction()
	{
		return this._action;
	}

	/**
	 * Sets the unique index id.
	 * 
	 * @param tcmUri the new unique index id
	 */
	public void setUniqueIndexId(String tcmUri)
	{
		this._uniqueIndexId = tcmUri;
	}

	/**
	 * Gets the unique index id.
	 * 
	 * @return the unique index id
	 */
	public String getUniqueIndexId()
	{
		return this._uniqueIndexId;
	}

	/**
	 * Gets the index type.
	 * 
	 * @return the index type
	 */
	public IndexType getIndexType()
	{
		return _itemType;
	}

	/**
	 * Sets the item type.
	 * 
	 * @param itemType the new item type
	 */
	public void setItemType(IndexType itemType)
	{
		this._itemType = itemType;
	}

	/**
	 * Gets the publication item id.
	 * 
	 * @return the publication item id
	 */
	public String getPublicationItemId()
	{
		return this._publicationId;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder r = new StringBuilder();
		r.append("[");
		r.append(this._action);
		r.append(",");
		r.append(this._uniqueIndexId);
		r.append(",");
		r.append(this._itemType);
		r.append(",");
		r.append(this._storageId);
		r.append("]");
		return r.toString();
	}
}
