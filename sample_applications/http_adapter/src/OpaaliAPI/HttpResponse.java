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

import java.net.HttpURLConnection;
import java.util.HashMap;

public class HttpResponse {
    public final int rc;
    public final HashMap<String, String> headers;
    public final String responseBody;
    
    public HttpResponse() {
        rc = HttpURLConnection.HTTP_INTERNAL_ERROR;
        headers = null;
        responseBody = null;
    }
    
    public HttpResponse(int rc, HashMap<String, String> headers, String responseBody) {
        this.rc = rc;
        this.headers = headers;
        this.responseBody = responseBody;
    }
}
