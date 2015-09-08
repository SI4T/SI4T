package org.si4t.cloudsearch;

public class CloudSearchClientRequest {
	
	private String endpointUrl;
    private String authentication;
    private String access_key_id;
    private String secret_access_key;
	
	public CloudSearchClientRequest(String endpointUrl,String authentication)
	{
		this.endpointUrl = endpointUrl;
        this.authentication = authentication;
	}

    public CloudSearchClientRequest(String endpointUrl,String authentication, String access_key_id, String secret_access_key)
    {
        this.endpointUrl = endpointUrl;
        this.authentication = authentication;
        this.access_key_id = access_key_id;
        this.secret_access_key = secret_access_key;
    }

	public String getEndpointUrl() {
		return endpointUrl;
	}
    public String getAccess_key_id() {
        return access_key_id;
    }
    public String getSecret_access_key() {
        return secret_access_key;
    }

    public String getAuthentication() {
        return authentication;
    }
}
