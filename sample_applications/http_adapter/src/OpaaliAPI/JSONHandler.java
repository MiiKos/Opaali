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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import OpaaliAPI.ApiCall.DeliveryInfo;


/*
 * tools for processing JSON formatted data
 */
public class JSONHandler {
    
    
    
    /*
     * processes the HttpResponse from an API Call and parses 
     * the JSON Data returned as body. 
     * depending on the type of data extracts and returns the
     * desired string value or null if fails
     */
    public static String processResponseData(HttpResponse resp) {
        if (resp != null && resp.responseBody != null && resp.rc != 400) {
            try {
                   JSONObject json = new JSONObject(resp.responseBody);  // Whole json
                   Log.logDebug("JSON:"+json); 
                   // try to detect which known JSON object this is
                   if (json.has("resourceReference")) {
                       // response to outboundMessageRequest
                       json = descendInto(json, "resourceReference");
                       return getStringValue(json, "resourceURL");
                   } else if (json.has("deliveryInfoList")) {
                       // response to Delivery Status request
                       // return JSON content as is to be parsed by application
                       return resp.responseBody;
                   } else if (json.has("inboundMessageList")) {
                       // response to Retrieve and Delete Inbound Messages request
                       // return JSON content as is to be parsed by application
                       return resp.responseBody;
                   } else  {
                       // nothing we expected - fall through and return null;
                   }
            } catch (JSONException e) {
                // not a valid JSON response
                e.printStackTrace();
            }
        }
        return null;
    }
    

    
    
    /*
     * processes the body returned in HttpResponse from an API Call 
     * and parses the JSON Data. 
     * if the JSON Data contains a deliveryInfoList then it parses and
     * returns an array of DeliveryInfo objects.
     */ 
    public static DeliveryInfo[] processDeliveryInfoList(String resp) {
        if (resp != null) {
            try {
                   JSONObject json = new JSONObject(resp);  // Whole json
                   Log.logDebug("JSON:"+json); 
                   // try to detect which known JSON object this is
                   if (json.has("deliveryInfoList")) {
                       // response to Delivery Status request
                       json = descendInto(json, "deliveryInfoList");
                       
                       if (json.has("deliveryInfo")) {
                           Log.logDebug("deliveryInfo exists");
                           JSONArray list = json.getJSONArray("deliveryInfo");
                           DeliveryInfo[] dI = new DeliveryInfo[list.length()];
                           for (int i = 0, j = 0; i < list.length(); i++) {
                               dI[i] = null;
                               json = list.getJSONObject(i);
                               String address = getStringValue(json, "address");
                               String deliveryStatus = getStringValue(json, "deliveryStatus");
                               String description = getStringValue(json, "description");
                               dI[j++] = new DeliveryInfo(address, deliveryStatus, description);
                           }
                           return dI;
                       }
                   } else  {
                       // nothing we expected - fall through and return null;
                   }
            } catch (JSONException e) {
                // not a valid JSON response
                e.printStackTrace();
            }
        }
        return null;
    }

    // = end of public part ===================================================


    /*
     * returns the string value of an object or null if it does not exist
     */
    private static String getStringValue(JSONObject json, String elementName) {
        String value = null;
        if (json.has(elementName)) {
            Log.logDebug(elementName+" exists");
            value = json.getString(elementName);
        }
        return value;
    }

    /*
     * dives deeper in a JSON object and returns the inner JSON object or
     * the original JSON Object if cannot find the specified object
     * (it is assumed that the caller just will not find any following
     * objects and will fail gracefully...)
     */
    private static JSONObject descendInto(JSONObject json, String elementName) {
        if (json.has(elementName)) {
            Log.logDebug(elementName+" exists");
            json = json.getJSONObject(elementName);
        }
        return json;
    }

}
