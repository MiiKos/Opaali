/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2018 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */


package CgwCompatibility;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import OpaaliAPI.HttpRequest;
import OpaaliAPI.HttpResponse;
import OpaaliAPI.Template;

public class CgwHttpRequest {

    private static final String[] MOTemplate = {
            // CGW style HTTP GET request
            "GET ${TARGET_URL} HTTP/1.1",
            "Accept: */*" ,
            "Character-set: ${TARGET_CHARSET}",
            "User-Agent: CGW Provider Server 4.0 http_adapter",
            "Host: ${TARGET_HOST}",
            ""
        };

    private static final String defaultCharset = "iso8859-1";


    public static HttpResponse get(String url) {
        return get(url, defaultCharset);
    }

    public static HttpResponse get(String url, String charset) {


        // get host for host - http header
        String targetHost = extractHost(url);

        // make the actual HTTP request, but first, fill in the template
        Template tmpl = new Template(MOTemplate);

        HashMap <String, String> vars = new HashMap<String, String>();
        vars.put("TARGET_URL", url);
        vars.put("TARGET_HOST", targetHost);
        vars.put("TARGET_CHARSET", charset);

        return HttpRequest.makeRequest(tmpl.expand(vars).toStrings());

    }


    /*
     * extract host part from URL
     */
    private static String extractHost(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            return url.getHost();
        }
        catch (MalformedURLException e) {

        }
        return null;
    }

}
