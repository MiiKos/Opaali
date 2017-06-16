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

import java.util.Base64;
import java.util.HashMap;

/*
 * OpaaliAPI: Authorization
 * Implementation based on document: OMA Authorization REST API Guide, Revision 5.0
 */

public class AuthAPI {

	
	// requestAccessToken()
	public static AccessToken requestAccessToken(String username, String password) {

		String[] authTemplate = {
			// API request for obtaining access_token
			"POST https://${API_HOST}/autho4api/v1/token HTTP/1.1",
			"Content-Type: application/x-www-form-urlencoded", 
			"Connection: keep-alive", 
			"Accept: application/json",
			"Authorization: Basic ${BASIC_AUTH}", 
			"", 
			"grant_type=client_credentials" 
		};

		AccessToken access_token = new AccessToken(authTemplate, username, password);
		access_token.authenticate();
		return access_token;
	}


	
	
	// revokeAccessToken()
	public static void revokeAccessToken(String username, String password, AccessToken access_token) {

		String[] authTemplate = {
			// API request for obtaining access_token
			"POST https://${API_HOST}/autho4api/v1/revoke HTTP/1.1",
			"Content-Type: application/x-www-form-urlencoded", 
			"Connection: keep-alive", 
			"Authorization: Basic ${BASIC_AUTH}", 
			"", 
			"token=${ACCESS_TOKEN}" 
		};

		Template tmpl = new Template(authTemplate);
		
		String temp = username+":"+password;
		String authString = Base64.getEncoder().encodeToString(temp.getBytes());

		HashMap <String, String> vars = Config.getConfig();
		vars.put("ACCESS_TOKEN", access_token.toString());
		vars.put("BASIC_AUTH", authString);
		
		ApiCall.makeRequest(access_token, tmpl, vars);
		
	}
	
}
