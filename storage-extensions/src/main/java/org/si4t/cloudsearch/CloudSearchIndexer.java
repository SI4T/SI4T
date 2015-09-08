package org.si4t.cloudsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.si4t.BaseIndexData;
import com.tridion.storage.si4t.BinaryIndexData;
import com.tridion.storage.si4t.IndexingException;
import com.tridion.storage.si4t.SearchIndex;
import com.tridion.storage.si4t.SearchIndexData;
import com.tridion.storage.si4t.Utils;

public class CloudSearchIndexer implements SearchIndex {
	
	private static final Logger log = LoggerFactory.getLogger(CloudSearchIndexer.class);
	
	private static String INDEXER_NODE = "Indexer";
	
	private static String DEFAULT_INDEX_BATCH_SIZE = "10";
	
	private Map<String, BaseIndexData> itemRemovals = new ConcurrentHashMap<String, BaseIndexData>();
	
	private Map<String, SearchIndexData> itemAdds = new ConcurrentHashMap<String, SearchIndexData>();
	
	private Map<String, BinaryIndexData> binaryAdds = new ConcurrentHashMap<String, BinaryIndexData>();
	
	private Map<String, SearchIndexData> itemUpdates = new ConcurrentHashMap<String, SearchIndexData>();
	
	private String documentdEndpoint;
	
	private String authentication;

	private String secret_access_key;
	
	private String access_key_id;
	
	private int indexBatchSize;

	@Override
	public void configure(Configuration configuration) throws ConfigurationException
	{
		log.debug("Configuration is: " + configuration.toString());

		Configuration indexerConfiguration = configuration.getChild(INDEXER_NODE);

		String documentdEndpoint = indexerConfiguration.getAttribute("documentdEndpoint");
		log.info("Setting Document Endpoint to: " + documentdEndpoint);
		this.documentdEndpoint = documentdEndpoint;

		String authentication = indexerConfiguration.getAttribute("authentication");
		log.info("Authentication method set to: " + authentication);
		this.authentication = authentication;

		if ("explicit".equals(this.authentication))
		{
			String access_key_id = indexerConfiguration.getAttribute("access_key_id", "");
			String secret_access_key = indexerConfiguration.getAttribute("secret_access_key", "");
			if (Utils.StringIsNullOrEmpty(access_key_id) && Utils.StringIsNullOrEmpty(secret_access_key))
			{
				log.info("CloudSearch Credentials should be stored at location (C:\\Users\\USERNAME\\.aws\\credentials), in valid format.");
			}
			else
			{
				log.info("CloudSearch Credentials are taken from cd_storage_conf.xml");
				this.access_key_id = access_key_id;
				this.secret_access_key = secret_access_key;
			}
		}
		
		String indexBatchSize = indexerConfiguration.getAttribute("indexBatchSize", DEFAULT_INDEX_BATCH_SIZE);
		this.indexBatchSize = Integer.valueOf(indexBatchSize);
		log.info("Index batch size set to: " + this.indexBatchSize);

		//Angel: We are only managing one document endpoint ATM
	}

	@Override
	public void addItemToIndex(SearchIndexData data) throws IndexingException
	{		
		if (Utils.StringIsNullOrEmpty(data.getUniqueIndexId()))
		{
			log.error("Addition failed. Unique ID is empty");
			return;
		}

		if (data.getFieldSize() == 0)
		{
			log.warn("To be indexed item has no data.");
			log.warn("Item is: " + data.toString());
		}

		if (!this.itemAdds.containsKey(data.getUniqueIndexId()))
		{
			this.itemAdds.put(data.getUniqueIndexId(), data);
		}
	}

	@Override
	public void removeItemFromIndex(BaseIndexData data) throws IndexingException
	{
		if (Utils.StringIsNullOrEmpty(data.getUniqueIndexId()))
		{
			log.error("Removal addition failed. Unique ID empty");
			return;
		}
		this.itemRemovals.put(data.getUniqueIndexId(), data);
	}

	@Override
	public void updateItemInIndex(SearchIndexData data) throws IndexingException
	{
		if (Utils.StringIsNullOrEmpty(data.getUniqueIndexId()))
		{
			log.error("Adding update item failed. Unique ID empty");
			return;
		}
		this.itemUpdates.put(data.getUniqueIndexId(), data);
	}

	@Override
	public void addBinaryToIndex(BinaryIndexData data) throws IndexingException {
		// TODO Auto-generated method stub
	}

	@Override
	public void removeBinaryFromIndex(BaseIndexData data) throws IndexingException {
		// TODO Auto-generated method stub
	}

	@Override
	public void commit(String publicationId) throws IndexingException
	{
		try
		{
			this.commitAddContentToCloudSearch(this.itemAdds);
			//this.commitAddBinariesToCloudSearch();
			this.removeItemsFromCloudSearch(this.itemRemovals);
			this.processItemUpdates();
		}
		catch (IOException e)
		{
			logException(e);
			throw new IndexingException("IO Exception: " + e.getMessage());
		}
		catch (ParserConfigurationException e)
		{
			logException(e);
			throw new IndexingException("ParserConfigurationException: " + e.getMessage());
		}
		catch (SAXException e)
		{
			logException(e);
			throw new IndexingException("SAXException:" + e.getMessage());
		}
		catch (DocumentServiceException e)
		{
			logException(e);
			throw new IndexingException("Configuration Exception:" + e.getMessage());
		}
		catch (AmazonClientException e)
		{
			logException(e);
			throw new IndexingException("AmazonClientException Exception:" + e.getMessage());
		}
		finally
		{
			log.info("Clearing out registers.");
			this.clearRegisters();
		}
	}
/*
	private void commitAddBinariesToCloudSearch()  throws DocumentServiceException, IOException, ParserConfigurationException, SAXException
	{
		if (this.binaryAdds.size() > 0)
		{
			log.info("Adding binaries to Solr.");

			log.info
					(
							CloudSearchIndexDispatcher.INSTANCE.
									addBinaries(binaryAdds,
											new CloudSearchClientRequest(
													this.documentdEndpoint,
													this.authentication,
													this.access_key_id,
													this.secret_access_key)
									)
					);
		}
	}
*/


	private void commitAddContentToCloudSearch(Map<String, SearchIndexData> itemsToAdd) throws DocumentServiceException, IOException, ParserConfigurationException, SAXException
	{
		if (itemsToAdd != null && itemsToAdd.size() > 0)
		{
			log.info("Adding " + itemsToAdd.size() + " documents in batches of " + indexBatchSize);

			List<DocumentBatch> groupedDocuments = new ArrayList<DocumentBatch>();
						
			int i = 0;
			DocumentBatch documentBatch = null;
			for (Entry<String, SearchIndexData> entry : itemsToAdd.entrySet())
			{
				if (i % indexBatchSize == 0)
				{
					documentBatch = new DocumentBatch();
					groupedDocuments.add(documentBatch);
				}
				SearchIndexData data = entry.getValue();
				documentBatch.getItems().add(constructInputDocument(data, log));
				i++;
			}
			//log.trace(groupedDocuments.toString());
			this.dispatchAddContentToCloudSearch(groupedDocuments);
		}
	}

	private void removeItemsFromCloudSearch(Map<String, BaseIndexData> itemsToRemove) throws DocumentServiceException, IOException, ParserConfigurationException, SAXException
	{
		if (itemsToRemove != null && itemsToRemove.size() > 0)
		{
			log.info("Removing " + itemsToRemove.size() + " documents in batches of " + indexBatchSize);

			List<DocumentBatch> groupedDocuments = new ArrayList<DocumentBatch>();
			
			int i = 0;
			DocumentBatch documentBatch = null;
			for (Entry<String, BaseIndexData> entry : itemsToRemove.entrySet())
			{
				if (i % indexBatchSize == 0)
				{
					documentBatch = new DocumentBatch();
					groupedDocuments.add(documentBatch);
				}
				documentBatch.getItems().add(
						new DocumentData(DocumentDataType.delete, entry.getValue().getUniqueIndexId())
				);
				i++;
			}
			//log.trace(groupedItems.toString());
			this.dispatchRemoveItemsFromCloudSearch(groupedDocuments);
		}
	}

	private static DocumentData constructInputDocument(SearchIndexData data, Logger log)
	{
		DocumentData document = new DocumentData(DocumentDataType.add, data.getUniqueIndexId());
		
		log.info("Adding document with ID: " + document.getId());		
		
		Map<String, ArrayList<Object>> fieldList = data.getIndexFields();
		for (Entry<String, ArrayList<Object>> fieldEntry : fieldList.entrySet())
		{
			String fieldName = fieldEntry.getKey();
			for (Object o : fieldEntry.getValue())
			{
				document.getFields().put(fieldName, o); //Right now we have just one element on the array ALWAYS
				log.trace("Adding field : " + fieldName + ": " + o);
			}
		}
		return document;
	}	

	private void dispatchAddContentToCloudSearch(List<DocumentBatch> groupedDocuments) throws DocumentServiceException, ParserConfigurationException, IOException, SAXException 
	{		
		log.info("Dispatching documents in " + groupedDocuments.size() + " batches");

		int batchIndex = 1;
		for (DocumentBatch documentBatch : groupedDocuments)
		{
			int batchSize = documentBatch.getItems().size();
			if (batchSize > 0)
			{
				DispatcherPackage dispatcherPackage = new DispatcherPackage
						(
								DispatcherAction.PERSIST,
								new CloudSearchClientRequest(
										this.documentdEndpoint,
										this.authentication,
										this.access_key_id,
										this.secret_access_key
										),
										documentBatch
						);
				String status = CloudSearchIndexDispatcher.INSTANCE.addDocuments(dispatcherPackage);
				
				log.info("Adding " + batchSize + " documents of batch " + batchIndex + " had the following response: " + status);
			}
			batchIndex++;
		}		
	}

	private void dispatchRemoveItemsFromCloudSearch(List<DocumentBatch> groupedDocuments) throws DocumentServiceException, ParserConfigurationException, IOException, SAXException
	{
		log.info("Dispatching documents in " + groupedDocuments.size() + " batches");

		int batchIndex = 1;
		for (DocumentBatch documentBatch : groupedDocuments)
		{
			int batchSize = documentBatch.getItems().size();
			if (batchSize > 0)
			{
				DispatcherPackage dispatcherPackage = new DispatcherPackage
						(
								DispatcherAction.PERSIST,
								new CloudSearchClientRequest(
										this.documentdEndpoint,
										this.authentication,
										this.access_key_id,
										this.secret_access_key
								),
								documentBatch
						);
				String status = CloudSearchIndexDispatcher.INSTANCE.removeFromCloudSearch(dispatcherPackage);
				
				log.info("Removing " + batchSize + " documents of batch " + batchIndex + " had the following response: " + status);
			}
			batchIndex++;
		}
	}

	private void processItemUpdates() throws ParserConfigurationException, IOException, SAXException, DocumentServiceException
	{
		this.commitAddContentToCloudSearch(this.itemUpdates);
	}	

	private void logException(Exception e)
	{
		log.error(e.getMessage());
		log.error(Utils.stacktraceToString(e.getStackTrace()));
	}

	private void clearRegisters()
	{
		itemAdds.clear();
		binaryAdds.clear();
		itemRemovals.clear();
		itemUpdates.clear();
	}	

	@Override
	public void destroy() {
		CloudSearchIndexDispatcher.INSTANCE.destroyServers();
	}

}
