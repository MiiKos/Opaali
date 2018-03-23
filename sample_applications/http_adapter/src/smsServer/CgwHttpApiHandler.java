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

//import CgwCompatibility.CgwMessage;
//import CgwCompatibility.UDH;
import OpaaliAPI.AccessToken;
import OpaaliAPI.ApiCall;
import OpaaliAPI.AuthAPI;
import OpaaliAPI.Config;
import OpaaliAPI.HttpResponse;
import OpaaliAPI.Log;
import OpaaliAPI.Message;
import OpaaliAPI.MessagingAPI;
import OpaaliAPI.OutboundSMSBinaryMessage;
import OpaaliAPI.OutboundSMSFlashMessage;
import OpaaliAPI.OutboundSMSTextMessage;
import OpaaliAPI.ApiCall.DeliveryInfo;

/*
 * handler for CGW style HTTP requests
 */
public class CgwHttpApiHandler implements HttpHandler {

    private final int CGW_RC_OK = 200;
    private final int CGW_RC_FAIL = 401;

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
        StrMask[] logMasks = StrMask.parseMaskConfig(sc.getConfigEntry(ServerConfig.CONFIG_LOG_MASK));
        if (logMasks.length == 0) {
            logMasks = null;
        }
        logRequest = new RequestLogger(logMasks);
        Config.setServiceConfig(serviceName, sc);

    }

    String serviceName = null;
    ServiceConfig sc = null;
    AccessToken access_token = null;
    String username = null;
    String password = null;
    String cgwCharset = null;
    RequestLogger logRequest = null;
    boolean failureMode = false;         // true after an unrecoverable error

    @Override
    public void handle(HttpExchange x) throws IOException {

        long startTime = RequestLogger.getTimeNow();

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

        long endTime = RequestLogger.getTimeNow();
        logRequest.log(x.getRequestURI().toString(), resp.rc, x.getLocalAddress().getPort(), endTime-startTime);
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
        int rc = CGW_RC_FAIL;    // default error response
        HashMap<String, String> responseHeaders = new HashMap<String, String>();
        String responseBody = "";
        String responseTemplate = "<html><head><title>Send SMS result</title></head><body><h2>Delivery result</h2>%1$s<br></body></html>";

        responseHeaders.put("content-type", "text/html");

        if (params == null) {
            // no parameters!
            Log.logDebug("NO PARAMS!");
            rc = CGW_RC_FAIL;
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
                rc = CGW_RC_FAIL;
                responseBody = String.format(responseTemplate, "Missing sender address in HTTP request.<br><br>");
            } else if (to == null || to.length == 0) {
                rc = CGW_RC_FAIL;
                responseBody = String.format(responseTemplate, "Missing recipient address in HTTP request line.<br><br>");
            } else if (msg == null || msg.length() == 0) {
                rc = CGW_RC_FAIL;
                responseBody = String.format(responseTemplate, "Missing message content in HTTP request.<br><br>");
            } else {
                rc = CGW_RC_OK;    // optimistically we will now expect everything to succeed...
                // get possible optional parameters
                boolean bin = false;      // true if a binary message is to be sent
                boolean flash = false;    // true if a flash message is to be sent
                int port = -1;            // >=0 if a smart message (port addressing) is requested
                int vp = -1;              // >=0 if specific validity period is requested
                String udh = "";          // udh hex string, if provided

                if (params.has("mcl")) {
                    // if message class is 0 send as flash message
                    String mcl = params.getParam("mcl");
                    if ("0".equals(mcl)) {
                        flash = true;
                    }
                    else if ("1".equals(mcl)) {
                        flash = false;
                    }
                    else {
                        // unsupported value
                        rc = CGW_RC_FAIL;
                        responseBody = String.format(responseTemplate, "Unsupported value "+mcl+" for message class in HTTP request line.<br><br>");
                    }
                }
                if (params.has("bin")) {
                    bin = true;
                }
                /*
                 * TODO: implementing smart parameter in a way compatible with CGW is a bit tricky
                 *       so we'll ignore it for now
                if (params.has("smart")) {
                }
                */
                if (params.has("udh")) {
                    // User Data Header requires a binary message
                    bin = true;
                    udh = params.getParam("udh");
                }
                /*
                 * TODO: how on earth can we pass this to the API request?
                 *       Opaali API would want it as an extra http header
                if (params.has("vp")) {
                    // Validity Period
                    String vpStr = params.getParam("vp");
                    try {
                        vp = Integer.parseInt(vpStr);
                    }
                    catch (Exception e) {
                        Log.logWarning("invalid value for parameter vp="+vpStr+" ignored!");
                    }
                }
                 */
                if (rc == CGW_RC_OK) {
                    // continue only if all the parameters were fine

                    if (params.has("validateonly")) {
                        // validate request syntax without actually making the request to OpaaliAPI
                        rc = CGW_RC_OK;
                        responseBody = "<html><head><title>Send SMS parameters validated</title></head><body><h2>Send SMS parameters validated</h2>No actual message sent.<br></body></html>";
                        Log.logInfo("validating request parameters without making an actual API request");
                    }
                    else {

                        // make conversions to the message content
                        try {
                            msg = URLDecoder.decode(msg, cgwCharset);
                        } catch (UnsupportedEncodingException e) {
                            Log.logWarning("CGW:failed to URL decode using "+cgwCharset);
                            msg = URLDecoder.decode(msg);
                        }
                        msg = ApiCall.escapeJSON(msg);
                        msg = ApiCall.escapeSweFin(msg);

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
                            return new HttpResponse(CGW_RC_FAIL, responseHeaders, responseBody);
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

                        // build the correct type of message
                        Message message = null;
                        if (bin) {
                            //message = new OutboundSMSBinaryMessage(udh+msg);
                            //message = new OutboundSMSBinaryMessage(null, msg);
                            message = new OutboundSMSBinaryMessage(udh, msg);
                            //TODO: make sure that this works if msg is a text string and not a hex string
                        }
                        else if (flash) {
                            message = new OutboundSMSFlashMessage(msg);
                        }
                        else {
                            message = new OutboundSMSTextMessage(msg);
                        }

                        // make the actual request to Opaali-API
                        String resourceURL = MessagingAPI.outboundMessageRequest(access_token,
                                                                                 to,
                                                                                 from,
                                                                                 senderName,
                                                                                 null, //no receiptRequest
                                                                                 null, //no clientCorrelator
                                                                                 null, //no chargingInfo,
                                                                                 message);    // charset?

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
            }
        }

        // ready to return response
        HttpResponse resp = new HttpResponse(rc, responseHeaders, responseBody);

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

}
