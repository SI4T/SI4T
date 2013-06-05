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

import com.tridion.storage.BinaryContent;

/**
 * BinaryIndexData.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 * @see BaseIndexData.java
 */
public class BinaryIndexData extends BaseIndexData
{

	private String _fileName;
	private String _fileSize;
	private String _fileType;
	private BinaryContent _content;
	private String _url;

	public BinaryIndexData(FactoryAction action, IndexType itemType, String publicationId, String storageId)
	{
		super(action, itemType, publicationId, storageId);
	}

	/**
	 * Gets the file size.
	 * 
	 * @return the file size
	 */
	public String getFileSize()
	{
		return _fileSize;
	}

	/**
	 * Sets the file size.
	 * 
	 * @param _fileSize the new file size
	 */
	public void setFileSize(String _fileSize)
	{
		this._fileSize = _fileSize;
	}

	/**
	 * Gets the file type.
	 * 
	 * @return the file type
	 */
	public String getFileType()
	{
		return _fileType;
	}

	/**
	 * Sets the file type.
	 * 
	 * @param _fileType the new file type
	 */
	public void setFileType(String _fileType)
	{
		this._fileType = _fileType;
	}

	/**
	 * Gets the content.
	 * 
	 * @return the content
	 */
	public BinaryContent getContent()
	{
		return _content;
	}

	/**
	 * Sets the content.
	 * 
	 * @param _content the new content
	 */
	public void setContent(BinaryContent _content)
	{
		this._content = _content;
	}

	/**
	 * Gets the index url.
	 * 
	 * @return the index url
	 */
	public String getIndexUrl()
	{
		return _url;
	}

	/**
	 * Sets the index url.
	 * 
	 * @param _url the new index url
	 */
	public void setIndexUrl(String _url)
	{
		this._url = _url;
	}

	/**
	 * Gets the file name.
	 * 
	 * @return the file name
	 */
	public String getFileName()
	{
		return _fileName;
	}

	/**
	 * Sets the file name.
	 * 
	 * @param _fileName the new file name
	 */
	public void setFileName(String _fileName)
	{
		this._fileName = _fileName;
	}

	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.extensions.search.BaseIndexData#toString()
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
		r.append(this._fileName);
		r.append(",");
		r.append(this._fileSize);
		r.append(",");
		r.append(this._url);
		r.append("]");
		return r.toString();
	}

}
