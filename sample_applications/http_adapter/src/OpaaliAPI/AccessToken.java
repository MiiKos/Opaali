/*
 * Opaali (Telia Operator Service Platform) sample code
 * 
 * Copyright(C) 2017 Telia Company
 * 
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 * 
 * Author: jlasanen
 * 
 */


package OpaaliAPI;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Base64;
import org.json.JSONObject;


public class AccessToken {
	/*
	 * A class representing the access_token that is needed for an API session
	 * 
	 * 
	 */

	
	
	// default template for authentication HTTP request 
	static String[] authTemplate = {
	        // API request for obtaining access_token    	
	    	"POST https://${API_HOST}/autho4api/v1/token HTTP/1.1",
	        "Content-Type: application/x-www-form-urlencoded",
	        "Connection: keep-alive",
	        "Accept: application/json",
	        "Authorization: Basic ${BASIC_AUTH}",
	    	"",
	    	"grant_type=client_credentials"
	    };
	    

	
	private Template tmpl = null;
	private String[] template = null;
	private String authString = null;
	private String access_token = null;
    private long expires_in = 0;      // original lifetime
    private LocalDateTime expires = LocalDateTime.now();
    private final int MARGIN = 30;    // seconds 
	
	/*
	 * Create an AccessToken with default template and specified credentials
	 */
	public AccessToken(String username, String password) {
		this();
		String temp = username+":"+password;
		authString = Base64.getEncoder().encodeToString(temp.getBytes());
	}

	
	/*
	 * Create an AccessToken with specified template and credentials
	 */
	public AccessToken(String[] template, String username, String password) {
		this.template = template;
		tmpl = new Template(template);
		String temp = username+":"+password;
		authString = Base64.getEncoder().encodeToString(temp.getBytes());
	}


	/*
	 * returns the string representation used in API calls
	 */
	public String getString() {
		return access_token;
	}
	
	
	/*
	 * returns true if the access_token is (almost) older than the default validity period
	 * (note that this does not notice if the token has been prematurely invalidated by 
	 *  an external call using the same credentials)
	 */
	public boolean isExpired() {
		return (expires.compareTo(LocalDateTime.now()) < 0);
	}
	
	
	/*
	 * makes an actual authentication request
	 * and returns the access_token string
	 */
	public String authenticate() {
		if (authString != null) {
			// set authentication string to BASIC_AUTH variable
			HashMap <String, String> vars = Config.getConfig(); //new HashMap <String, String>(); //Config.getConfig();
			vars.put("BASIC_AUTH", authString);
			Log.logDebug("vars:"+vars);
			// expand the request template with the variables and make a HTTP request
			HttpResponse resp = HttpRequest.makeRequest(tmpl.expand(vars).toStrings());
			// process the HTTP response and extract access_token value
			processAuthResponse(resp);
		}
		return access_token;
	}
	
	/*
	 * makes an actual authentication request if isExpired() returns true
	 */
	public String renew() {
		if (isExpired())
			return authenticate();
		else
		    return access_token;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "AccessToken:\n"+
	           "   access_token: "+access_token+"\n"+
			   "   expires_in: "+expires_in+"\n"+
	           "   expires: "+expires;
	}


	//= end of public part ======================================================
	
	private AccessToken() {
		template = authTemplate;
		tmpl = new Template(template);
	}
	


	/*
	 * parse the HTTP response and extract access_token value and lifetime
	 */
	private boolean processAuthResponse(HttpResponse resp) {
		// does not check HTTP status but assumes that the JSON body is
		// only available after a successful request
		//TODO: add status check 
		/*
		 * 200 - success
		 * 400 - Bad Request
		 * 401 - Authentication failure
		 * 403 - Policy Exception: log the error and wait to prevent further exceeding TPS 
		 */
		if (resp != null && resp.responseBody != null) {
	   		JSONObject json = new JSONObject(resp.responseBody);  // whole JSON
	   		Log.logDebug("JSON:"+json);
	   		access_token = json.getString("access_token");
	   		expires_in = json.getLong("expires_in");
			expires = LocalDateTime.now().plusSeconds(expires_in - MARGIN);
			return (access_token != null);
		}
		return false;
	}

}
