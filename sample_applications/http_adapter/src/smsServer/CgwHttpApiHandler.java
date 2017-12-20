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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import OpaaliAPI.AccessToken;
import OpaaliAPI.ApiCall;
import OpaaliAPI.AuthAPI;
import OpaaliAPI.Config;
import OpaaliAPI.HttpResponse;
import OpaaliAPI.Log;
import OpaaliAPI.MessagingAPI;
import OpaaliAPI.OutboundSMSTextMessage;
import OpaaliAPI.ApiCall.DeliveryInfo;

/*
 * handler for CGW style HTTP requests
 */
public class CgwHttpApiHandler implements HttpHandler {
    
    private final int CGW_RC_OK = 200;

    CgwHttpApiHandler(ServiceConfig sc) {
        
        this.sc = sc;
        this.serviceName = sc.getServiceName();
        username = sc.getConfigEntry(ServerConfig.CONFIG_USERNAME);
        sc.remove(ServerConfig.CONFIG_USERNAME);  // just to be sure, hide from others
        password = sc.getConfigEntry(ServerConfig.CONFIG_PASSWORD);
        sc.remove(ServerConfig.CONFIG_PASSWORD);  // just to be sure, hide from others
        cgwCharset = sc.getConfigEntry(ServerConfig.CONFIG_CGWCHARSET);
        if (cgwCharset == null) {
            cgwCharset = "ISO-8859-15";
        }
        logMasks = StrMask.parseMaskConfig(sc.getConfigEntry(ServerConfig.CONFIG_LOG_MASK));
        if (logMasks.length == 0)
            logMasks = null;
        Config.setServiceConfig(serviceName, sc);
        
    }

    String serviceName = null;
    ServiceConfig sc = null;
    AccessToken access_token = null;
    String username = null;
    String password = null;
    String cgwCharset = null;
    StrMask[] logMasks = null;
    boolean failureMode = false;         // true after an unrecoverable error
    
    @Override
    public void handle(HttpExchange x) throws IOException {

        long startTime = java.util.Calendar.getInstance().getTimeInMillis();

        // this triggers log rotation after midnight, if enabled
        SmsServer.checkLogRotation();

        Log.logDebug("http request: " + x.getRequestURI());
        
        String queryString = x.getRequestURI().getRawQuery();
        
        QueryString params = ((queryString != null && queryString.length() > 0) ? new QueryString(queryString) : null);

        //Log.logDebug(params.toString());

        HttpResponse resp = translateCGW2OpaaliRequest(params);
        
        Log.logDebug("CGWHandler response: HTTP " + resp.rc + ": " + resp.responseBody);
        
        Headers responseHeaders = x.getResponseHeaders();
        for (String key: resp.headers.keySet()) {
            responseHeaders.set(key, resp.headers.get(key));
        }

        x.sendResponseHeaders(resp.rc, resp.responseBody.length());
        OutputStream os = x.getResponseBody();
        os.write(resp.responseBody.getBytes());
        os.close();

        long endTime = java.util.Calendar.getInstance().getTimeInMillis();
        logRequest(x.getRequestURI().toString(), resp.rc, x.getLocalAddress().getPort(), endTime-startTime);
    }



    // = end of public part ===============================================

    //private HashMap<String, List<String>> parameters = null;

    
    /*
     * translate a CGW style HTTP request into an OpaaliAPI request
     */
    private HttpResponse translateCGW2OpaaliRequest(QueryString params) {
        
       
        /*
         * All known CGW style HTTP request parameters,
         * not all will be implemented.
         * 
         * Parameters in brackets are optional.
         * 
         * From
         * To
         * Msg
         * [nrqurl]
         * [ddt]
         * [vp]
         * [bin]
         * [udh]
         * [mcl]
         * [smart]
         * [charge]
         * [info]
         *  
         */
        
        /*
         * request return code, response headers and body
         */
        int rc = 401;    // default error response
        HashMap<String, String> headers = new HashMap<String, String>();
        String responseBody = "";
        String responseTemplate = "<html><head><title>Send SMS result</title></head><body><h2>Delivery result</h2>%1$s<br></body></html>";

        headers.put("content-type", "text/html");

        if (params == null) {
            // no parameters!
            Log.logDebug("NO PARAMS!");
            rc = 401;
            responseBody = String.format(responseTemplate, "Invalid HTTP request line.<br><br>");
        }
        else {
            // get mandatory parameters
            String senderName = null;
            String from = params.getParam("from");
            String[] to = params.getParamList("to");
            String msg = params.getParam("msg");

            Log.logDebug("checking mandatory params");

            // verify that mandatory parameters are present
            if (from == null || from.length() == 0) {
                rc = 401;
                responseBody = String.format(responseTemplate, "Missing sender address in HTTP request.<br><br>");
            } else if (to == null || to.length == 0) {
                rc = 401;
                responseBody = String.format(responseTemplate, "Missing recipient address in HTTP request line.<br><br>");
            } else if (msg == null || msg.length() == 0) {
                rc = 401;
                responseBody = String.format(responseTemplate, "Missing message content in HTTP request.<br><br>");
            } else {

                // get possible optional parameters
                //TODO: implement more functionality

                // make conversions to the message content
                msg = ApiCall.escapeJSON(msg);
                msg = ApiCall.escapeSweFin(msg);
                try {
                    msg = URLDecoder.decode(msg, cgwCharset);
                } catch (UnsupportedEncodingException e) {
                    Log.logWarning("CGW:failed to URL decode using "+cgwCharset);
                    msg = URLDecoder.decode(msg);
                }
                
                // make sure we have a valid session
                if (!failureMode && access_token == null) {
                    access_token = AuthAPI.requestAccessToken(username, password);
                    //TODO: error handling
                    /*
                     * if (access_token == null) shut down this handler (unrecoverable error)
                     * if (access_token != null && access_token.length() == 0) retry (one or more?) after 1-2 seconds
                     * if (access_token != null && access_token.length() > 0) go make the actual API call
                     *
                     * potential problem: if retry takes a long time the caller may time out even when the eventual API call succeeds
                     *
                     */
                    if (access_token == null) {
                        // unrecoverable error
                        //TODO: yes this is possible, due to e.g. an unrecoverable config error
                        //log this error and return 403 or 503 or 509 immediately
                        //TODO: should we try again or should we keep failing until this
                        //service is restarted?
                        Log.logError("problem with access token...");
                        failureMode = true;
                    }
                    else {
                        // success
                        //api = new MessagingAPI(serviceName, access_token);   //TODO: not really needed?
                    }

                }

                if (failureMode) {
                    // after encountering an unrecoverable error keep returning error to caller
                    Log.logError("unrecoverable error, returning 401 Unauthorized to request");
                    if (responseBody.length() == 0) {
                        responseBody = "<b>Access is denied due to probably invalid credentials in http_adapter configuration</b>";
                    }
                    return new HttpResponse(401, headers, responseBody);
                }

                // add "tel:" prefix to international numbers (and URLdecode too...)
                from = addTelPrefix(from);
                for (int i = 0; i < to.length; i++) {
                    to[i] = addTelPrefix(to[i]);
                }

                if (from.charAt(0) == '$') {
                    // alphanumeric sender
                    senderName = from.substring(1);
                    from = null;
                }



                // make the actual request to Opaali-API
                String resourceURL = MessagingAPI.outboundMessageRequest(access_token,
                                                                         to,
                                                                         from,
                                                                         senderName,
                                                                         null, //no receiptRequest
                                                                         null, //no clientCorrelator
                                                                         null, //no chargingInfo, 
                                                                         new OutboundSMSTextMessage(msg));    // charset?

                Log.logDebug("resourceURL="+resourceURL);
                
                if (resourceURL != null) {

                    /*
                     * at this point, if there are multiple recipients, we should check the status for each using the resourceURL
                     *
                     */

                    StringBuilder str = new StringBuilder();
                    DeliveryInfo[] dInfos = MessagingAPI.getDeliveryInfos(access_token, resourceURL);

                    for (DeliveryInfo dI : dInfos) {
                        if (dI != null && dI.address != null) {
                            if (dI.deliveryStatus == dI.DeliveredToNetwork ||
                                dI.deliveryStatus == dI.DeliveredToTerminal) {
                                str.append("Success: ").append(dI.address).append(": OK<br>");
                                rc = CGW_RC_OK;
                            }
                            else {
                                str.append("Failure: ").append(dI.address).append(": ").append("Message sending failed.").append(dI.description != null ? "("+dI.description+")" : "").append("<br>");
                                Log.logWarning(dI.toString());
                            }
                        }
                    }

                    responseBody = String.format(responseTemplate, str);
                }
                else {
                    // return failure for all recipients
                    StringBuilder str = new StringBuilder();

                    for (String s : to) {
                        str.append("Failure: ").append(s).append(": ").append("Message sending failed.").append("<br>");
                    }

                    responseBody = String.format(responseTemplate, str);
                }

            }
        }

        // ready to return response        
        HttpResponse resp = new HttpResponse(rc, headers, responseBody);

        return resp;
    }


    /*
     * convert international numbers to required format
     */
    private String addTelPrefix(String msisdn) {
        
        try {
            msisdn = URLDecoder.decode(msisdn, cgwCharset);
        } catch (UnsupportedEncodingException e) {
            Log.logWarning("CGW:failed to URL decode using "+cgwCharset);
            msisdn = URLDecoder.decode(msisdn);
        }     
        
        return (msisdn.startsWith("+") ? "tel:"+msisdn : msisdn);
    }


    private void logRequest(String request, int respCode, int port, long ms) {
        StringBuilder sb = new StringBuilder();
        sb.append("http request(port ").append(port).append("): ");
        sb.append(logMasks != null ? StrMask.applyMasks(request, logMasks) : request);
        sb.append(" (response in ");
        sb.append(ms);
        sb.append("ms:");
        sb.append(respCode);
        if (respCode == 200)
            sb.append(" OK");
        else if (respCode == 401)
            sb.append(" Unauthorized");
        sb.append(')');
        Log.logInfo(sb.toString());
    }
}
