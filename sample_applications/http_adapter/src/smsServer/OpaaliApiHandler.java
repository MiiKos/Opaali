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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import CgwCompatibility.CgwHttpRequest;
import CgwCompatibility.CgwMessage;
import CgwCompatibility.KeywordMapper;
import OpaaliAPI.Config;
import OpaaliAPI.HttpResponse;
import OpaaliAPI.InboundMessage;
import OpaaliAPI.Log;
import OpaaliAPI.Message;


/*
 * handler for Opaali API notification HTTP requests
 */
public class OpaaliApiHandler implements HttpHandler {

    private final int OPAALI_RC_OK = 204;
    private final int OPAALI_RC_FAIL = 403;
    private final int OPAALI_RC_PANIC = 500;

    OpaaliApiHandler(ServiceConfig sc) {

        sc.setValidity(true);   // default
        this.sc = sc;
        this.serviceName = sc.getServiceName();
        // mappingFile contains mappings from keywords to target URLs
        mappingFile = sc.getConfigEntry(ServerConfig.CONFIG_MAPPINGFILE);
        if (mappingFile != null) {
            mapping = new Mapping(mappingFile);
            if (!mapping.readMappings()) {
                // failed to initialize mapping, so plan B: use defaultUrl only
                mapping = null;
            }
        }
        defaultUrl = sc.getConfigEntry(ServerConfig.CONFIG_DEFAULTURL);
        if (mapping != null && defaultUrl != null) {
            keyMapper = new KeywordMapper(mapping, defaultUrl);
        }
        else if (defaultUrl != null) {
            keyMapper = new KeywordMapper(defaultUrl);
        }
        else {
            // invalid config - must have somewhere to send requests to
            isValid = false;
        }
        opaaliCharset = sc.getConfigEntry(ServerConfig.CONFIG_OPAALICHARSET);
        if (opaaliCharset == null) {
            opaaliCharset = "UTF-8";
        }
        StrMask[] logMasks = StrMask.parseMaskConfig(sc.getConfigEntry(ServerConfig.CONFIG_LOG_MASK));
        if (logMasks.length == 0) {
            logMasks = null;
        }
        logRequest = new RequestLogger(logMasks);

        nowait = (sc.getConfigEntryInt(ServerConfig.CONFIG_NOWAIT) != 0);

        defaultReplyUrl = sc.getConfigEntry(ServerConfig.CONFIG_DEFAULTREPLYURL);
        defaultReplyCharset = sc.getConfigEntry(ServerConfig.CONFIG_DEFAULTREPLYCHARSET);
        if (defaultReplyCharset == null) {
            defaultReplyCharset = "ISO-8859-15";
        }

        targetCharset = sc.getConfigEntry(ServerConfig.CONFIG_TARGETCHARSET);
        if (targetCharset == null) {
            targetCharset = "ISO-8859-15";
        }

        //qSvc = QueueServiceHandler.create();

        if (isValid) {
            Config.setServiceConfig(serviceName, sc);
        }
        else {
            sc.setValidity(false);
            failureMode = true;
        }

    }

    String serviceName = null;
    ServiceConfig sc = null;
    String defaultUrl = null;
    String mappingFile = null;
    String opaaliCharset = null;
    RequestLogger logRequest = null;
    boolean failureMode = false;         // true after an unrecoverable error
    KeywordMapper keyMapper = null;
    Mapping mapping = null;
    private boolean isValid = true;
    private boolean nowait = false;
    private QueueService qSvc = null;
    private String defaultReplyUrl = null;
    private String defaultReplyCharset = null;
    private String targetCharset = null;

    @Override
    public void handle(HttpExchange x) throws IOException {

        long startTime = RequestLogger.getTimeNow();

        // this triggers log rotation after midnight, if enabled
        SmsServer.checkLogRotation();

        Log.logDebug("http request: " + x.getRequestURI());

        // ignore possible query string (at least for now...)
        //String queryString = x.getRequestURI().getRawQuery();
        //
        //QueryString params = ((queryString != null && queryString.length() > 0) ? new QueryString(queryString) : null);

        //Log.logDebug(params.toString());

        /*
        // TODO: check that we have a POST request
        if (x.getRequestMethod().equalsIgnoreCase("POST")) {

        }
        */

        Headers headers = x.getRequestHeaders();

        // read request body into string
        InputStream is = x.getRequestBody();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder reqBody = new StringBuilder();
        String inputLine = null;
        while ((inputLine = in.readLine()) != null) {
            reqBody.append(inputLine);
        }
        in.close();
        String body = reqBody.toString();

        HttpResponse resp = translateOpaali2CGWRequest(x.getRequestURI().getPath(), headers, body);

        Log.logDebug("OpaaliApiHandler response: HTTP " + resp.rc + ": " + resp.responseBody);

        Headers responseHeaders = x.getResponseHeaders();
        for (String key: resp.headers.keySet()) {
            responseHeaders.set(key, resp.headers.get(key));
        }

        try {
        x.sendResponseHeaders(resp.rc, (resp.responseBody.length() > 0 ? resp.responseBody.length() : -1));
            if (resp.rc != OPAALI_RC_OK) {
                OutputStream os = x.getResponseBody();
                os.write(resp.responseBody.getBytes());
                os.close();
            }
        } catch (IOException e) {
            Log.logWarning("failed to send response headers to client");
        }

        long endTime = RequestLogger.getTimeNow();
        logRequest.log(x.getRequestURI().toString(), resp.rc, x.getLocalAddress().getPort(), endTime-startTime);
    }



    // = end of public part ===============================================

    /*
     * translate an Opaali style notification HTTP request into a CGW style notification http request
     */
    private HttpResponse translateOpaali2CGWRequest(String path, Headers headers, String body) {

        if (failureMode) {
            // TODO: fail fast
            return new HttpResponse(OPAALI_RC_PANIC, null, "Server Error");
        }

        int rc = OPAALI_RC_OK;    // default success response
        HttpResponse resp = null;
        boolean delayedProcessing = false;

        // create Opaali style message from JSON data
        InboundMessage msg = OpaaliAPI.JSONHandler.processInboundMessageNotification(body);

        if (msg == null) {
            // fail
            rc = OPAALI_RC_PANIC;
            resp = null;
        }
        else {
            // do message type specific processing
            if ((msg.getMessageType() & Message.SMS_TEXT_MESSAGE) != 0) {
                // text message
            }
            else if ((msg.getMessageType() & Message.SMS_BINARY_MESSAGE) != 0) {
                // binary message
                // TODO: to be implemented
                rc = OPAALI_RC_FAIL;
            }
            else {
                // probably MMS message
                //rc = OPAALI_RC_FAIL;
            }

            if (rc == OPAALI_RC_OK) {
                // do the common processing    up to the point of making the actual request

                // convert Opaali style message into CGW style message
                CgwMessage message = new CgwMessage(msg);

                // get the URL template based on keyword
                String templateUrl = keyMapper.mapKeyword(message.getTo(), message.getKeyword());

                // fill templateUrl from message
                String targetUrl = KeywordMapper.tmplExpand(templateUrl, message, targetCharset);

                String replyUrl = null;
                if (ServerConfig.SERVICE_TYPE_QR.equalsIgnoreCase(sc.getServiceType())) {
                    // fill templateUrl from message
                    replyUrl = KeywordMapper.tmplExpand(defaultReplyUrl, message);
                }

                // don't wait for backend http request to complete if the service has
                // nowait enabled in config and the url ends in "nowait"
                delayedProcessing = nowait && path.endsWith(ServerConfig.CONFIG_NOWAIT);

                if (delayedProcessing) {
                    // submit the http request into a queue to be processed later
                    // and immediately return success to the caller
                    // (tell a little white lie...we don't know if the data is actually delivered)
                    // in case submit to queue failed, just turn delayedProcessing off and fall through
                    if (delayedProcessing = QueueServiceHandler.get().submit(null, msg.getMessageId(), targetUrl, replyUrl)) {
                        logRequest.log("Queued processing for request "+targetUrl+"with id "+msg.getMessageId(), -1, -1, 0);
                    }
                    rc = OPAALI_RC_OK;
                }
                if (!delayedProcessing) {
                    // make the actual CGW style http request now

                    long startTime = RequestLogger.getTimeNow();

                    resp = CgwHttpRequest.get(targetUrl, targetCharset);

                    long endTime = RequestLogger.getTimeNow();

                    if (resp == null) {
                        logRequest.log("Failed to make http request to"+targetUrl, -1, -1, endTime-startTime);
                    }
                    else {
                        logRequest.log(targetUrl, resp.rc, -1, endTime-startTime);

                        if (resp.responseBody != null && resp.responseBody.length() > 0) {
                            /*
                             * if successful response contained a body, return that content
                             * by generating a separate MT send request after returning from this request
                             * (to mimic CGW QR functionality)
                             */
                            if (ServerConfig.SERVICE_TYPE_QR.equalsIgnoreCase(sc.getServiceType())) {
                                /*
                                 * expand replyUrl a second time to fill in the message (if not already done),
                                 * this relies on the replyUrl containing the macro $(MSG)
                                 * which relies the original replyUrl having contained $$(MSG)
                                 * ( $$ is interpreted as escaped $ )
                                 */

                                replyUrl = KeywordMapper.tmplExpand(replyUrl, new CgwMessage(null,null,null,null,resp.responseBody), defaultReplyCharset);

                                if (QueueServiceHandler.get().submit(null, msg.getMessageId(), replyUrl, null)) {
                                    // submitted to queue
                                    logRequest.log("Queued processing for response to "+targetUrl, -1, -1, 0);}
                                else {
                                    // failed to submit, data will be lost!
                                    logRequest.log("Failed to queue response for request "+targetUrl+", data will be lost!", -1, -1, 0);
                                }
                            }
                            rc = OPAALI_RC_OK;    // at least the request was successful, if reply was not...
                        }
                    }
                }

            }
        }

        //process response
        if (resp != null) {
            rc = resp.rc;
            switch (resp.rc) {
                case 200: // OK
                    rc = OPAALI_RC_OK;
                    break;
                case 400: // BAD REQUEST
                    break;
                case 401: // UNAUTHORIZED
                    // authentication is not supported by CGW Provider Server
                    break;
                case 403: // FORBIDDEN
                    // not supported
                    break;
                default:
                    break;
                }
        } else if (!delayedProcessing) {
            rc = OPAALI_RC_PANIC;
        }

        /*
         * request return code, response headers and body
         */
        /*
         * supported responses:
         * - 204 No Content = success
         * - 403 Forbidden = failure
         * - 500 Server error = permanent failure
         *
         */

        HashMap<String, String> responseHeaders = new HashMap<String, String>();
        String responseBody = "";

        // ready to return response
        resp = new HttpResponse(rc, responseHeaders, responseBody);

        return resp;
    }


}
