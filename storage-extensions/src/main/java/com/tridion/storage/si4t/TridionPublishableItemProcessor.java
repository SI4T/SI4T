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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.tridion.broker.StorageException;
import com.tridion.data.CharacterData;
import com.tridion.data.CharacterDataString;

/**
 * TridionPublishableItemProcessor.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class TridionPublishableItemProcessor extends TridionBaseItemProcessor
{
	private static Pattern SEARCH_DIRECTIVE_PATTERN = Pattern.compile("(?ims)<!--\\s*INDEX-DATA-START:(.*?):INDEX-DATA-END\\s*-->");
	private String tridionItem;
	private String storageId;
	private FactoryAction action;
	private IndexType indexType;
	private String publicationId;
	private String uniqueIndexId;
	private Logger log = LoggerFactory.getLogger(TridionPublishableItemProcessor.class);

	public TridionPublishableItemProcessor(String tridionItem, FactoryAction action, IndexType type, String publicationId, String uniqueIndexId, String storageId)
	{
		this.tridionItem = tridionItem;
		this.action = action;
		this.indexType = type;
		this.publicationId = publicationId;
		this.uniqueIndexId = uniqueIndexId;
		this.storageId = storageId;
	}

	public CharacterDataString processPageSource(CharacterData page) throws StorageException
	{
		CharacterDataString c = null;
		String source = this.tridionItem;
		this.process();
		c = new CharacterDataString(page.getPublicationId(), page.getId(), TridionPublishableItemProcessor.removeTags(source));
		return c;
	}

	public String processComponentPresentationSource() throws StorageException
	{
		this.process();
		return removeTags(this.tridionItem);
	}

	private void process() throws StorageException
	{
		try
		{
			SearchIndexData data = this.getSearchDataDirectives();
			if (data == null)
			{
				log.info("No search data found.");
				this.RegisterRemovalOnNoIndexData();
				return;
			}
			if (data.getFieldSize() == 0)
			{
				log.info("No fields found to index.");
				this.RegisterRemovalOnNoIndexData();
				return;
			}
			registerItemAddition(data, log);
		}
		catch (SAXException e)
		{
			throw new StorageException("SAXException: " + e.getMessage(),e);
		}
		catch (IOException e)
		{
			throw new StorageException("IOException: " + e.getMessage(),e);
		}
		catch (ParserConfigurationException e)
		{
			throw new StorageException("ParserConfigurationException: " + e.getMessage(),e);
		}
	}

	private void RegisterRemovalOnNoIndexData()
	{
		log.info("Registering removal attempt for: " + uniqueIndexId);
		registerItemRemoval(uniqueIndexId, indexType, log, publicationId, storageId);
	}

	private SearchIndexData getSearchDataDirectives() throws SAXException, IOException, ParserConfigurationException
	{
		Matcher m = SEARCH_DIRECTIVE_PATTERN.matcher(this.tridionItem);
		log.info("Finding search directives.");
		while (m.find())
		{
			if (m.groupCount() == 1)
			{
				String searchDataXml = m.group(1).toString();
				if (!Utils.StringIsNullOrEmpty(searchDataXml))
				{
					log.debug("Search Directive string: " + searchDataXml);
					Document d = getXmlDocumentForSearchData(searchDataXml);
					return registerSearchDataFields(d);
				}
				log.info("No searchDataXml markers found.");
			}
		}
		return null;
	}

	private Document getXmlDocumentForSearchData(String searchDataXml) throws SAXException, IOException, ParserConfigurationException
	{
		return XMLHelpers.getXMLDocumentFromString(searchDataXml);
	}

	/*
	 * Currently supports first child elements and everything under a Custom
	 * node
	 * the 'id' field should always be present.
	 */

	private SearchIndexData registerSearchDataFields(Document searchDataDocument)
	{
		SearchIndexData data = new SearchIndexData(this.action, this.indexType, this.publicationId, this.storageId);
		data.setUniqueIndexId(this.uniqueIndexId);
		NodeList nodeList = searchDataDocument.getFirstChild().getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++)
		{
			Node n = nodeList.item(i);

			if (n.getNodeName().equalsIgnoreCase("id"))
			{
				log.trace("NOT Adding: " + n.getNodeName() + "::" + n.getTextContent());
			}
			else if (n.getNodeName().equalsIgnoreCase("custom"))
			{
				if (n.hasChildNodes())
				{
					NodeList customNodes = n.getChildNodes();
					for (int j = 0; j < customNodes.getLength(); j++)
					{
						Node customNode = customNodes.item(j);
						log.trace("Adding: " + customNode.getNodeName() + "::" + customNode.getTextContent());
						data.addIndexField(customNode.getNodeName(), customNode.getTextContent());
					}
				}
			}
			else
			{
				if (n.getFirstChild() != null)
				{
					if (!n.getFirstChild().hasChildNodes())
					{
						log.trace("Adding: " + n.getNodeName() + "::" + n.getTextContent());
						data.addIndexField(n.getNodeName(), n.getTextContent());
					}
					else
					{
						log.trace("Adding: " + n.getNodeName() + "::" + XMLHelpers.nodeToString(n, false));
						data.addIndexField(n.getNodeName(), XMLHelpers.nodeToString(n, false));
					}
				}
			}
		}

		return data;
	}

	public static String removeTags(String tridionItem)
	{
		String toReturn = tridionItem.replaceAll(SEARCH_DIRECTIVE_PATTERN.pattern(), "");
		return toReturn;
	}
}
