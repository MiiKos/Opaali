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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import OpaaliAPI.ApiCall.DeliveryInfo;

/*
 * OpaaliAPI: Messaging
 * Implementation based on document: OMA Messaging REST API Guide, Revision 8.4
 */

public class MessagingAPI {
    
    public MessagingAPI(String serviceName,
                        AccessToken access_token) {
        this.serviceName = serviceName;
        this.access_token = access_token;
    }
    
    private String serviceName = null;
    private AccessToken access_token = null;
    
    private MessagingAPI() {}
    
    // use this as default sender number when there is no actual number 
    private final static String DEFAULT_SENDER = "tel:+358000000000";
    

    /*
     * there are three variants of each API request:
     * 1) one with built in session (access_token)
     * 2) a static one with no configuration data
     * 3) a static one with configuration data supplied
     *   
     * choose whichever suits your purpose
     */



    
    /*    
     * outboundMessageRequest
     *  - access_token
     *  - address/addressList
     *  - senderAddress
     *  - senderName
     *  - receiptRequest
     *  - clientCorrelator
     *  - chargingInformation
     *  - outboundSMSMessage
     */
    public String outboundMessageRequest(String[] addressList,
                                         String senderAddress, 
                                         String senderName,
                                         ApiCall.ReceiptRequest receiptRequest,
                                         String clientCorrelator,
                                         ApiCall.ChargingInfo chargingInfo,
                                         Message message) {
        
        return outboundMessageRequest(access_token,
                                      Config.getServiceConfig(serviceName),
                                      addressList,
                                      senderAddress, 
                                      senderName,
                                      receiptRequest,
                                      clientCorrelator,
                                      chargingInfo,
                                      message);
    }


    /*    
     * outboundMessageRequest
     *  - access_token
     *  - address/addressList
     *  - senderAddress
     *  - senderName
     *  - receiptRequest
     *  - clientCorrelator
     *  - chargingInformation
     *  - outboundSMSMessage
     */
    public static String outboundMessageRequest(AccessToken access_token, 
                                                String[] addressList,
                                                String senderAddress, 
                                                String senderName,
                                                ApiCall.ReceiptRequest receiptRequest,
                                                String clientCorrelator,
                                                ApiCall.ChargingInfo chargingInfo,
                                                Message outboundSMSMessage) {
        
        return     outboundMessageRequest(access_token,
                                       null,
                                       addressList,
                                       senderAddress, 
                                       senderName,
                                       receiptRequest,
                                       clientCorrelator,
                                       chargingInfo,
                                       outboundSMSMessage);
    }

    
    /*    
     * outboundMessageRequest
     *  - access_token
     *  - config - request specific config variables or null for default config
     *  - address/addressList
     *  - senderAddress
     *  - senderName
     *  - receiptRequest
     *  - clientCorrelator
     *  - chargingInformation
     *  - outboundSMSMessage
     */
    public static String outboundMessageRequest(AccessToken access_token,
                                                final HashMap <String, String> config,
                                                String[] addressList,
                                                String senderAddress, 
                                                String senderName,
                                                ApiCall.ReceiptRequest receiptRequest,
                                                String clientCorrelator,
                                                ApiCall.ChargingInfo chargingInfo,
                                                Message message) {

        String[] MTTemplate = {
            // API request for sending MT messages
            "POST https://${API_HOST}/production/messaging/v1/outbound/${SENDERADDRESS_ENCODED}/requests HTTP/1.1",
            "Host: ${API_HOST}",
            "Content-type: application/json",
            "Accept: application/json",
            "Authorization: Bearer ${ACCESS_TOKEN}",
            "",
            "{",
            "    \"outboundMessageRequest\":",
            "        {\"address\":[${RECIPIENTLIST}],",
            "         \"senderAddress\":\"${SENDERADDRESS}\",",
            "         ${MESSAGE}",
            "         ${SENDERNAMESTRING}${CHARGINGINFO}${DLRREQUESTSTRING}${CLIENTCORRELATOR}",
            "    }",
            "}"
        };
        
        senderAddress = (senderAddress != null ? senderAddress : DEFAULT_SENDER);
        
        Template tmpl = new Template(MTTemplate);
        
        
        HashMap <String, String> vars = (config != null ? (HashMap<String, String>)config.clone() : Config.getConfig());
        vars.put("ACCESS_TOKEN", access_token.renew());
        vars.put("SENDERADDRESS", senderAddress);
        vars.put("RECIPIENTLIST", makeList(addressList));
        vars.put("MESSAGE", message.toString());
        vars.put("SENDERNAMESTRING", (senderName != null ? ",\"senderName\":\""+senderName+"\"" : ""));
        vars.put("CHARGINGINFO", (chargingInfo != null ? chargingInfo.toString() : ""));
        vars.put("DLRREQUESTSTRING", (receiptRequest != null ? receiptRequest.toString() : ""));
        vars.put("CLIENTCORRELATOR", (clientCorrelator != null ? ", \"clientCorrelator\":\""+ApiCall.escapeJSON(clientCorrelator)+"\"" : ""));
        
        try {
            vars.put("SENDERADDRESS_ENCODED", URLEncoder.encode(senderAddress, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            vars.put("SENDERADDRESS_ENCODED", URLEncoder.encode(senderAddress));
        }
        
        
        return ApiCall.makeRequest(access_token, tmpl, vars);
        
    }
    

    /* 
     * getDeliveryInfos
     */
    public DeliveryInfo[] getDeliveryInfos(String resourceUrl) {
        return getDeliveryInfos(access_token,
                                Config.getServiceConfig(serviceName),
                                resourceUrl);
    }

    public static DeliveryInfo[] getDeliveryInfos(AccessToken access_token, String resourceUrl) {
        return getDeliveryInfos(access_token, null, resourceUrl);
    }
    
    public static DeliveryInfo[] getDeliveryInfos(AccessToken access_token,
                                                  final HashMap <String, String> config,
                                                  String resourceUrl) {

        String[] MTTemplate = {
                // API request for getting deliveryInfos
                "GET ${RESOURCEURL}/deliveryInfos HTTP/1.1",
                "Host: ${API_HOST}", 
                "Accept: application/json",
                "Authorization: Bearer ${ACCESS_TOKEN}", 
                ""
        };
        
        Template tmpl = new Template(MTTemplate);

        HashMap<String, String> vars = (config != null ? (HashMap<String, String>) config.clone() : Config.getConfig());
        vars.put("ACCESS_TOKEN", access_token.renew());
        vars.put("RESOURCEURL", resourceUrl);

        String response = ApiCall.makeRequest(access_token, tmpl, vars);
        DeliveryInfo[] dI = processDeliveryInfoList(response);
        return dI;
    }

    // = end of public part ===================================================
    
    /*
     * build a comma separated string from array of strings
     * -each individual string will be inside quotes 
     */
    private static String makeList(String[] list) {
        StringBuilder str = new StringBuilder();
        str.append("\"");
        for (String s : list) {
            str.append(s).append("\", \"");
        }
        if (str.length() > 1) {
            return str.substring(0, str.length()-3);    // crop out last ", \""
        }
        else {
            return "";
        }
    }
    
    
    private static DeliveryInfo[] processDeliveryInfoList(String resp) {
        return JSONHandler.processDeliveryInfoList(resp);
    }

    // dump data for debug purposes
    public static String dumpbytes(byte[] b) {
        String s = "";
        for (byte bb : b) {
            s += Byte.toUnsignedInt(bb) + ", ";
        }
        return s;
    }

    
}
