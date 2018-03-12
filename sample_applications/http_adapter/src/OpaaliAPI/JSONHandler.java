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

import OpaaliAPI.ApiCall.Attachment;
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


    /*
     * processes the body returned in HttpResponse from an API Call
     * and parses the JSON Data.
     * if the JSON Data contains a deliveryInfoList then it parses and
     * returns an array of DeliveryInfo objects.
     */
    public static InboundMessage processInboundMessageNotification(String resp) {
        if (resp != null) {
            try {
                   JSONObject json = new JSONObject(resp);  // Whole json
                   Log.logDebug("JSON:"+json);
                   // try to detect which known JSON object this is
                   if (json.has("inboundMessageNotification")) {
                       json = descendInto(json, "inboundMessageNotification");
                       if (json.has("inboundMessage")) {
                           Log.logDebug("inboundMessage exists");
                           json = descendInto(json, "inboundMessage");
                           String destinationAddress = getStringValue(json, "destinationAddress");
                           String senderAddress = getStringValue(json, "senderAddress");
                           String dateTime = getStringValue(json, "dateTime");
                           String resourceURL = getStringValue(json, "resourceURL");
                           String messageId = getStringValue(json, "messageId");
                           if (json.has("inboundSMSTextMessage")) {
                               Log.logDebug("inboundSMSTextMessage exists");
                               json = descendInto(json, "inboundSMSTextMessage");
                               String message = getStringValue(json, "message");

                               return new InboundMessage(destinationAddress,
                                                      senderAddress,
                                                      dateTime,
                                                      resourceURL,
                                                      messageId,
                                                      new InboundSMSTextMessage(message));
                           }
                           else if (json.has("inboundMMSMessage")) {
                               Log.logDebug("inboundMMSMessage exists");
                               //Log.logInfo("inboundMMSMessage not really supported...");
                               /*
                                * -subject
                                * -priority
                                * -attachment
                                *   -contentType
                                *   -link
                                *     -rel
                                *     -href
                                */
                               json = descendInto(json, "inboundMMSMessage");
                               String subject = getStringValue(json, "subject");
                               String priority = getStringValue(json, "priority");
                               if (json.has("attachment")) {
                                   Log.logDebug("attachment(s) exists");
                                   json = descendInto(json, "attachment");
                                JSONArray list = json.getJSONArray("attachment");
                                Attachment[] a = new Attachment[list.length()];
                                  for (int i = 0, j = 0; i < list.length(); i++) {
                                    a[i] = null;
                                    json = list.getJSONObject(i);
                                    String contentType = getStringValue(json, "contentType");
                                    String href = getStringValue(json, "href");
                                    a[j++] = new Attachment(contentType, href);
                                }

                                  return new InboundMessage(destinationAddress,
                                                          senderAddress,
                                                          dateTime,
                                                          resourceURL,
                                                          messageId,
                                                          new InboundMMSMessage(subject,
                                                                                priority,
                                                                                a));
                               }
                           }
                           else if (json.has("inboundSMSBase64Message")) {
                               Log.logDebug("inboundSMSBase64Message exists");
                               //Log.logInfo("inboundSMSBase64Message not supported");
                               /*
                                * -datacoding
                                * -sourcePort
                                * -destinationPort
                                */
                               json = descendInto(json, "inboundSMSBase64Message");
                               int dataCoding = getIntValue(json, "dataCoding");
                               int sourcePort = getIntValue(json, "sourcePort");
                               int destinationPort = getIntValue(json, "destinationPort");
                               String message = getStringValue(json, "message");

                               return new InboundMessage(destinationAddress,
                                                      senderAddress,
                                                      dateTime,
                                                      resourceURL,
                                                      messageId,
                                                      new InboundSMSBase64Message(message,
                                                               dataCoding,
                                                               sourcePort,
                                                               destinationPort));

                           }
                       }
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
    public static InboundMessage processInboundMessage(String resp) {
        if (resp != null) {
            try {
                   JSONObject json = new JSONObject(resp);  // Whole json
                   Log.logDebug("JSON:"+json);
                   // try to detect which known JSON object this is
                   if (json.has("inboundMessage")) {
                       Log.logDebug("inboundMessage exists");
                       json = descendInto(json, "inboundMessage");
                       String destinationAddress = getStringValue(json, "destinationAddress");
                       String senderAddress = getStringValue(json, "senderAddress");
                       String dateTime = getStringValue(json, "dateTime");
                       String resourceURL = getStringValue(json, "resourceURL");
                       String messageId = getStringValue(json, "messageId");
                       if (json.has("inboundSMSTextMessage")) {
                           Log.logDebug("inboundSMSTextMessage exists");
                           json = descendInto(json, "inboundSMSTextMessage");
                           String message = getStringValue(json, "message");

                           return new InboundMessage(destinationAddress,
                                                  senderAddress,
                                                  dateTime,
                                                  resourceURL,
                                                  messageId,
                                                  new InboundSMSTextMessage(message));
                       }
                       else if (json.has("inboundMMSMessage")) {
                           Log.logDebug("inboundMMSMessage exists");
                           Log.logInfo("inboundMMSMessage not supported");
                           /*
                            * -subject
                            * -priority
                            * -attachment
                            *   -contentType
                            *   -link
                            *     -rel
                            *     -href
                            */
                       }
                       else if (json.has("inboundSMSBase64Message")) {
                           Log.logDebug("inboundSMSBase64Message exists");
                           //Log.logInfo("inboundSMSBase64Message not supported");
                           /*
                            * -datacoding
                            * -sourcePort
                            * -destinationPort
                            */
                           json = descendInto(json, "inboundSMSBase64Message");
                           int dataCoding = getIntValue(json, "dataCoding");
                           int sourcePort = getIntValue(json, "sourcePort");
                           int destinationPort = getIntValue(json, "destinationPort");
                           String message = getStringValue(json, "message");

                           return new InboundMessage(destinationAddress,
                                                  senderAddress,
                                                  dateTime,
                                                  resourceURL,
                                                  messageId,
                                                  new InboundSMSBase64Message(message,
                                                           dataCoding,
                                                           sourcePort,
                                                           destinationPort));

                       }
                   }
            } catch (JSONException e) {
                // not a valid JSON response
                e.printStackTrace();
            }
        }
        return null;
    }

    /*
     * parses JSON data enough to detect which message type it is
     */
    public static int detectMessageType(String msg) {
        int msgType = Message.SMS_UNKNOWN_MESSAGE;
        if (msg != null) {
            try {
                   JSONObject json = new JSONObject(msg);  // Whole json
                   Log.logDebug("JSON:"+json);
                   // try to detect which known JSON object this is
                   if (json.has("inboundMessageNotification"))
                       msgType = Message.SMS_NOTIFICATION | Message.SMS_INBOUND_MESSAGE;
                   if (json.has("inboundMessageList"))
                       msgType = Message.SMS_LIST | Message.SMS_INBOUND_MESSAGE;
            } catch (JSONException e) {
                // not a valid JSON response
                e.printStackTrace();
            }
        }
        return msgType;
    }


    /*
     * convert message type to string representation
     */
    public static String messageTypeToString(int msgType) {
        String s = "";
        if ((msgType & Message.SMS_INBOUND_MESSAGE) != 0) {
            s = "InboundMessage";
        }
        if ((msgType & Message.SMS_NOTIFICATION) != 0) {
            s += "Notification";
        }
        if ((msgType & Message.SMS_LIST) != 0) {
            s += "List";
        }
        return s;
    }


    /*
     * processes the body returned in HttpResponse from an API Call
     * and parses the JSON Data.
     * if the JSON Data contains a requestError then it parses and
     * returns the policyException messageId (or null).
     */
    public static String processRequestError(String resp) {
        if (resp != null) {
            try {
                   JSONObject json = new JSONObject(resp);  // Whole json
                   Log.logDebug("JSON:"+json);
                   // try to detect which known JSON object this is
                if (json.has("requestError")) {
                       json = JSONHandler.descendInto(json, "requestError");
                    if (json.has("policyException")) {
                           json = JSONHandler.descendInto(json, "policyException");
                           return JSONHandler.getStringValue(json, "messageId");
                    }
                }
            } catch (JSONException e) {
                // not a valid JSON response
                return null;
            }
        }
        return null;
    }


    /* utilities for navigating json object */

    /*
     * returns the integer value of an object or 0 if it does not exist
     */
    public static int getIntValue(JSONObject json, String elementName) {
        int val = 0;
        if (json.has(elementName)) {
            Log.logDebug(elementName+" exists");
            String value = json.getString(elementName);
            try {
                val = Integer.parseInt(value);
            }
            catch (Exception e) {}
        }
        return val;
    }

    /*
     * returns the string value of an object or null if it does not exist
     */
    public static String getStringValue(JSONObject json, String elementName) {
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
    public static JSONObject descendInto(JSONObject json, String elementName) {
        if (json.has(elementName)) {
            Log.logDebug(elementName+" exists");
            json = json.getJSONObject(elementName);
        }
        return json;
    }

    // = end of public part ===================================================

}
