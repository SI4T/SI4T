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

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SearchIndexData.
 * 
 * POJO which holds all data necessary for an indexing action
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class SearchIndexData extends BaseIndexData
{
	private ConcurrentHashMap<String, ArrayList<Object>> _indexFields = new ConcurrentHashMap<String, ArrayList<Object>>();

	public SearchIndexData(FactoryAction action, IndexType itemType, String publicationId, String storageId)
	{
		super(action, itemType, publicationId, storageId);
	}

	public void addIndexField(String name, Object value)
	{
		ArrayList<Object> field = this._indexFields.get(name);
		if (field == null)
		{
			field = new ArrayList<Object>();
		}
		field.add(value);
		this._indexFields.put(name, field);
	}

	public ConcurrentHashMap<String, ArrayList<Object>> getIndexFields()
	{
		return this._indexFields;
	}

	public ArrayList<Object> getIndexField(String name)
	{
		return this._indexFields.get(name);
	}

	public Object getIndexField(String name, int index)
	{
		ArrayList<Object> list = this._indexFields.get(name);
		if (index < list.size() && index > 0)
		{
			return list.get(index);
		}
		return null;
	}

	public int getFieldSize()
	{
		return this._indexFields.size();
	}

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
		r.append(",{");
		for (Entry<String, ArrayList<Object>> entry : this._indexFields.entrySet())
		{
			r.append(entry.getKey());
			r.append("{");
			for (Object o : entry.getValue())
			{
				r.append((String) o);
				r.append(",");
			}
			r.append("}");
		}
		r.append("}]");
		return r.toString();
	}
}
