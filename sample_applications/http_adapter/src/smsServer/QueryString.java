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


package smsServer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import OpaaliAPI.Log;

/*
 * a HTTP query string 
 */
public class QueryString {
        
    QueryString(String queryString) {
        try {
            parameters = parseQuery(queryString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /*
     * returns true if query string contains the key
     */
    public boolean has(String key) {
        try {
            return parameters.containsKey(key);
        } catch (NullPointerException ex) {
            return false;
        }
    }
    
    /*
     * returns a single value key value
     */
    public String getParam(String key) {
        try {
            return parameters.get(key).get(0);
        } catch (NullPointerException ex) {
            return null;
        }
    }
    
    /*
     * returns a multivalue key value as an array
     */
    public String[] getParamList(String key) {
        try {
            return parameters.get(key).toArray(new String[0]);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    // = end of public part ===============================================

    // parsed parameters and their values
    private HashMap<String, List<String>> parameters = null;

    /*
     * parse query string
     */
    private HashMap<String, List<String>> parseQuery(String queryString) throws UnsupportedEncodingException {
        HashMap <String, List<String>> params = new HashMap <String, List<String>>();
        Log.logDebug("query string: " + (queryString != null ? queryString : "no query string"));
        if (queryString == null || queryString.length() == 0) {
            // no query string!
            return null;
        }
        String[] fields =  queryString.split("&");
        for (String field : fields) {
            Log.logDebug(field);
            try {
                int i = field.indexOf('=');
                String key = (i > 0 ? field.substring(0, i).toLowerCase() : field);
                String value = (i > 0 ? field.substring(i+1) : null);
                List<String> list;
                if (params.containsKey(key)) {
                    list = params.get(key);
                } else {
                    list = new ArrayList<String>();
                    params.put(key,  list);
                }
                list.add(value);
            } catch (IndexOutOfBoundsException ex) {
                // ignore invalid parameters
                Log.logWarning("invalid parameter: "+field);
            }
        }
        return params;        
    }
    
}
