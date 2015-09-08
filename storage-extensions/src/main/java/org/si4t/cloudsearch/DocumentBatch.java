package org.si4t.cloudsearch;

import java.util.List;
import java.util.ArrayList;

public class DocumentBatch {
	
	private List<DocumentData> items;

	public DocumentBatch()
	{
		items = new ArrayList<DocumentData>();
	}

	public List<DocumentData> getItems() {
		return items;
	}

	public void setItems(List<DocumentData> items) {
		this.items = items;
	}

}
