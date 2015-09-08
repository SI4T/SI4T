package org.si4t.cloudsearch;

public class DispatcherPackage {

	private DispatcherAction action;
	
	private CloudSearchClientRequest clientRequest;
	
	private DocumentBatch documentBatch; // We may want to abstract this to an intermediate class

	public DispatcherPackage(DispatcherAction action, CloudSearchClientRequest request, DocumentBatch documentBatch)
	{
		super();
		this.setAction(action);
		this.setClientRequest(request);
		this.setDocumentBatch(documentBatch);
	}

	public DispatcherAction getAction() {
		return action;
	}

	public void setAction(DispatcherAction action) {
		this.action = action;
	}

	public CloudSearchClientRequest getClientRequest() {
		return clientRequest;
	}

	public void setClientRequest(CloudSearchClientRequest clientRequest) {
		this.clientRequest = clientRequest;
	}

	public DocumentBatch getDocumentBatch() {
		return documentBatch;
	}

	public void setDocumentBatch(DocumentBatch documentBatch) {
		this.documentBatch = documentBatch;
	}	
	
}
