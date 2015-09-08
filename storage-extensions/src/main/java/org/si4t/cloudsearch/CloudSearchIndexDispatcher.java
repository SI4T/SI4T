package org.si4t.cloudsearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import com.amazonaws.services.cloudsearchdomain.model.ContentType;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import com.google.gson.Gson;
import com.tridion.storage.si4t.BinaryIndexData;

public enum CloudSearchIndexDispatcher {

	INSTANCE;	
	
	private static final Logger log = LoggerFactory.getLogger(CloudSearchIndexDispatcher.class);
	
	private static Map<String, AmazonCloudSearchDomainClient> _AmazonCloudSearchDomainClients = new ConcurrentHashMap<String, AmazonCloudSearchDomainClient>();
	
	private AmazonCloudSearchDomainClient getAmazonCloudSearchDomainClient(CloudSearchClientRequest clientRequest)
	{
		if (_AmazonCloudSearchDomainClients.get(clientRequest.getEndpointUrl()) == null)
		{
			log.info("Obtaining AWS Domain Client [" + clientRequest.getEndpointUrl() + ": " + clientRequest.getEndpointUrl());
			if(clientRequest.getAuthentication().equals("explicit"))
			{
				this.createAmazonCloudSearchDomainClient(clientRequest.getEndpointUrl(),
						clientRequest.getAccess_key_id(),
						clientRequest.getSecret_access_key());
			}
			else
			{
				this.createAmazonCloudSearchDomainClient(clientRequest.getEndpointUrl());
			}
		}
		return _AmazonCloudSearchDomainClients.get(clientRequest.getEndpointUrl());
	}

	private void createAmazonCloudSearchDomainClient(String endpointUrl)
	{
		AmazonCloudSearchDomainClient csd = new AmazonCloudSearchDomainClient();
		csd.setEndpoint(endpointUrl);
		
		_AmazonCloudSearchDomainClients.put(endpointUrl, csd);
	}

	private void createAmazonCloudSearchDomainClient(String endpointUrl, String access_key_id, String secret_access_key)
	{
		AWSCredentials credentials = null;
		try {
			credentials = new BasicAWSCredentials(access_key_id, secret_access_key);
		} catch (Exception e) {
			throw new AmazonClientException("Could not create BasicAWSCredentials instance",e);
		}

		AmazonCloudSearchDomainClient csd = new AmazonCloudSearchDomainClient(credentials);
		csd.setEndpoint(endpointUrl);

		_AmazonCloudSearchDomainClients.put(endpointUrl, csd);
	}

	public String addDocuments(DispatcherPackage dispatcherPackage) throws ParserConfigurationException, IOException, SAXException, DocumentServiceException
	{
		UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
		AmazonCloudSearchDomainClient server = this.getAmazonCloudSearchDomainClient(dispatcherPackage.getClientRequest());
		if (server == null)
		{
			throw new AmazonClientException("Amazon Client not Instantiated");
		}
		
		DocumentBatch documentBatch = dispatcherPackage.getDocumentBatch();
		if (documentBatch == null)
		{
			throw new NullPointerException("Document batch is null");
		}		
		
		Gson gson = new Gson();
		String dJson = gson.toJson(documentBatch.getItems());
		log.info("Json Content: \n" + dJson);
		
		//"UTF-8"
		byte[] dJsonByteArray = StandardCharsets.UTF_8.encode(dJson).array();
		uploadDocumentsRequest.setDocuments(new ByteArrayInputStream(dJsonByteArray));
		uploadDocumentsRequest.setContentLength((long)dJsonByteArray.length);
		uploadDocumentsRequest.setContentType(ContentType.Applicationjson);

		UploadDocumentsResult results =  server.uploadDocuments(uploadDocumentsRequest);
		return results.getStatus();
	}

	public String addBinaries(Map<String, BinaryIndexData> binaryAdds, CloudSearchClientRequest clientRequest) throws IOException, DocumentServiceException, ParserConfigurationException, SAXException
	{
		//TODO:NOT IMPLEMENTED
		/*AmazonCloudSearchDomainClient server = null;
		server = this.getAmazonCloudSearchDomainClient(clientRequest);
		if (server == null)
		{
			throw new AmazonClientException("Amazon Client not Instantiated");
		}
		StringBuilder rsp = new StringBuilder();
		String rspResponse = " path not found";

		for (Map.Entry<String, BinaryIndexData> entry : binaryAdds.entrySet())
		{


		}
		return ("Adding binaries had the following response: " + rspResponse);*/
		return "";
	}

	public String removeFromCloudSearch(DispatcherPackage dispatcherPackage) throws ParserConfigurationException, IOException, SAXException, DocumentServiceException
	{
		UploadDocumentsRequest uploadDocumentsRequest = new UploadDocumentsRequest();
		AmazonCloudSearchDomainClient server = this.getAmazonCloudSearchDomainClient(dispatcherPackage.getClientRequest());
		if (server == null)
		{
			throw new AmazonClientException("Amazon Client not Instantiated");
		}
		
		DocumentBatch documentBatch = dispatcherPackage.getDocumentBatch();
		if (documentBatch == null)
		{
			throw new NullPointerException("Document batch is null");
		}
		
		Gson gson = new Gson();
		String dJson = gson.toJson(documentBatch.getItems());
		log.info("Json Content: \n" + dJson);
		
		//"UTF-8"
		byte[] dJsonByteArray = StandardCharsets.UTF_8.encode(dJson).array();
		uploadDocumentsRequest.setDocuments(new ByteArrayInputStream(dJsonByteArray));
		uploadDocumentsRequest.setContentLength((long)dJsonByteArray.length);
		uploadDocumentsRequest.setContentType(ContentType.Applicationjson);
		
		UploadDocumentsResult results =  server.uploadDocuments(uploadDocumentsRequest);
		return results.getStatus();
	}

	public void destroyServers()
	{
		/*
		for (Entry<String, AmazonCloudSearchDomainClient> clients : _AmazonCloudSearchDomainClients.entrySet())
		{
			HttpClient client = clients.getValue();
			if (client != null)
			{
				log.info("Closing down HttpClient for url: " + clients.getKey());
				client.getConnectionManager().shutdown();
			}
		}*/
	}
	
}
