package org.si4t.cloudsearch;

import java.util.HashMap;
import java.util.Map;

public class DocumentData {
	
	private DocumentDataType type;
	
	private String id;
	
	private Map<String, Object> fields;	

	public DocumentData(DocumentDataType type, String id)
	{
		this.type = type;
		this.id = id;
		this.fields = new HashMap<String,Object>();
	}

	public Map<String, Object> getFields() {
		return fields;
	}

	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DocumentDataType getType() {
		return type;
	}

	public void setType(DocumentDataType type) {
		this.type = type;
	}
}
