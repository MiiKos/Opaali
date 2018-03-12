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

import java.util.HashMap;

public class ApiCall {

    /*
     * making an API Call through a http request
     */
    public static String makeRequest(AccessToken access_token, Template tmpl, HashMap <String, String> vars) {

        /*
         * possible http request results:
         *  200 - success
         *  400 - Bad Request (terminate)
         *  401 - Authentication failure (terminate)
         *  403 - Policy Exception:
         *        - empty body: (terminate)
         *        - policy_error 3003: log the error and wait to prevent further exceeding TPS
         *        - policy_error 3004: (terminate)
         */

        HttpResponse resp = null;
        //HttpResponse lastResp = null;
        int retries = 0;
        do {
            //lastResp = resp;
            resp = HttpRequest.makeRequest(tmpl.expand(vars).toStrings());
            switch (resp.rc) {
                case 200: // OK
                    break;
                case 400: // BAD REQUEST
                    break;
                case 401: // UNAUTHORIZED
                    // re-authenticate once in case access_token has expired
                    String tokenString = access_token.authenticate();
                    /*
                     * tokenString is one of these types:
                     * - null -> unrecoverable error -> terminate somehow
                     * - ""   -> recoverable error, probably TPS exceeded
                     * -"..." -> valid token, go on and retry
                     *
                     * authenticate() should catch if 401 is recurring
                     *
                     */
                    vars.put("ACCESS_TOKEN", tokenString);
                    break;
                case 403: // FORBIDDEN
                    // this is handled in doRetry()
                    break;
                case 408: // REQUEST TIMEOUT
                    // make a retry, but only once - this is handled in doRetry()
                    break;
                default:
                    break;
            }
        } while (doRetry(retries++, resp/*, lastResp*/));

        return processResponse(resp);
    }


    /*
     * escape characters that cannot be used inside a JSON string
     */
    public static String escapeJSON(String s) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            switch (c) {
                case '\"':
                    str.append("\\\"");
                    break;
                case '\\':
                    str.append("\\\\");
                    break;
                case '/':
                    str.append("\\/");
                    break;
                case '\b':
                    str.append("\\b");
                    break;
                case '\f':
                    str.append("\\f");
                    break;
                case '\n':
                    str.append("\\n");
                    break;
                case '\r':
                    str.append("\\r");
                    break;
                case '\t':
                    str.append("\\t");
                    break;
                default:
                    if (Character.compare(c, '\u001f') <= 0) {
                        // escape control characters \u0000..\u001f
                        str.append("\\u"+String.format("%04x", (int) c));
                    }
                    else {
                        str.append(c);
                    }
                    break;
            }
        }
        return str.toString();
    }

    /*
     * for convenience escape accented characters used in Finland
     */
    public static String escapeSweFin(String s) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            switch (c) {
                case 'å':
                    str.append("\\u00E5");
                    break;
                case 'ä':
                    str.append("\\u00E4");
                    break;
                case 'ö':
                    str.append("\\u00F6");
                    break;
                case 'Å':
                    str.append("\\u00C5");
                    break;
                case 'Ä':
                    str.append("\\u00C4");
                    break;
                case 'Ö':
                    str.append("\\u00D6");
                    break;
                default:
                    str.append(c);
                    break;
            }
        }
        return str.toString();
    }



    public static class ReceiptRequest {
        String notifyURL = null;
        String notificationFormat = "JSON";
        String callbackData = null;

        public ReceiptRequest(String notifyURL, String callbackData) {
            this.notifyURL = notifyURL;
            this.callbackData = (callbackData != null ? escapeJSON(callbackData) : null);
        }

        public ReceiptRequest(String notifyURL) {
            this.notifyURL = notifyURL;
            this.callbackData = null;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return ", \"receiptRequest\": { \"notifyURL\":\"+notifyURL+"
                    + "\", \"notificationFormat\":\"JSON\""
                    + (callbackData != null ? ", \"callbackData\":\""+callbackData+"\"}" : "}");

        }
    }


    public class ChargingInfo {
        String description = null;
        String currency = "EUR";
        String amount = null;
        String code = null;

        public ChargingInfo(String description, float amount) {
            this.description = escapeJSON(description);
            this.amount = String.valueOf(amount);
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "\"charging\": { \"description\":[\""+description+
                         "\", \"currency\":\"EUR\",\"amount\""+amount+"\"}";
        }
    }


    public static class DeliveryInfo {
        public final String address;
        public final int deliveryStatus;
        public final String deliveryStatusStr;
        public final String description;
        public final int DeliveryNotificationNotSupported = 0;
        public final int DeliveredToNetwork = 1;
        public final int DeliveredToTerminal = 2;
        public final int DeliveryUncertain = 3;
        public final int DeliveryImpossible = 4;
        public final int MessageWaiting = 5;

        DeliveryInfo(String address, String deliveryStatusStr, String description) {
            this.address = address;
            this.description = description;
            this.deliveryStatusStr = deliveryStatusStr;
            if ("DeliveredToNetwork".equals(deliveryStatusStr)) {
                deliveryStatus = DeliveredToNetwork;
            }
            else if ("DeliveredToTerminal".equals(deliveryStatusStr)) {
                deliveryStatus = DeliveredToTerminal;
            }
            else if ("DeliveryUncertain".equals(deliveryStatusStr)) {
                deliveryStatus = DeliveryUncertain;
            }
            else if ("DeliveryImpossible".equals(deliveryStatusStr)) {
                deliveryStatus = DeliveryImpossible;
            }
            else {
                deliveryStatus = DeliveryNotificationNotSupported;
            }
        }

        @Override
        public String toString() {
            return "{deliveryInfo: address: "+address+", deliveryStatus: "+deliveryStatusStr+(description != null ? ", description: "+description : "")+"}";
        }

    }


    public static class Attachment {
        public final String contentType;
        public final Link link;

        public static class Link {
            public final String rel = "attachment";
            public final String href;
            //public final int size;

            Link(String href){
                this.href = href;
            }
        }

        Attachment(String contentType, String href) {
            this.contentType = contentType;
            this.link = new Link(href);
        }


        @Override
        public String toString() {
            return "{attachment: {contentType: "+contentType+"link: {rel: "+link.rel+", href: "+link.href+"}";
        }

    }


    /*
     * called when authentication fails due to a temporary condition
     */
    public AccessToken retryAuth(AccessToken access_token) {
        int retries = 1;
        while (retries-- >= 0) {
            String tokenString = access_token.authenticate();
            // check authenticate() return value
            if (tokenString == null) {
                // unrecoverable error
                return null;
            } else if (tokenString.length() == 0) {
                // a recoverable error, such as TPS exceeded - wait a second and retry
                slowDown(1);
            }
            else {
                // success
                break;
            }
        }
        return access_token;
    }


    private static boolean doRetry(int counter, HttpResponse resp /*, HttpResponse lastResp*/) {
        final int maxRetries = 4;
        switch (resp.rc) {
            case 200: return false;  // success - no need to retry
                 //break;
            case 400: return false;  // terminal failure - certainly no need to retry!
                 //break;
            case 401:
                // possible cause: token expired
                // repeated cases are caught elsewhere
                break;
            case 403:
                // possible cause: TPS exceeded
                // check if POL3003 error, then retry if counter < 10

                if (resp.responseBody != null)
                {
                    String policyError = JSONHandler.processRequestError(resp.responseBody);
                       if (policyError != null && "POL3003".equals(policyError)) {
                           // max transactions per interval exceeded - this is a recoverable error
                           slowDown(counter+1);
                       }
                       else {
                           // unrecoverable error
                           return false;
                       }
                }
                else {
                    // unrecoverable error
                    return false;
                }


                break;
            case 408:
                // possible cause: unknown
                if (counter < 2) {
                    Log.logError("HTTP "+resp.rc+" response from host, retrying once");
                    return true;
                }
                else {
                    return false;
                }
                //break;
            default:
                // unexpected -> terminate
                return false;
                //break;
        }
        if (counter < maxRetries) {
            return true;
        }
        return false;
    }




    /*
     * called to recover from temporary exceeding TPS limit
     */
    public static void slowDown(int secs) {
        // in case of a recoverable error, such as TPS exceeded - wait a second and retry
        try {
            Thread.sleep(secs*1000);
        } catch (InterruptedException e) {
            // just ignore this
            ;
         }
    }


    // = end of public part ===================================================

    /*
     * check request status,
     * return response body if successful
     * or null if fails
     */
    private static String processResponse(HttpResponse resp) {

        if (resp != null && resp.responseBody != null && resp.rc != 400) {
            return JSONHandler.processResponseData(resp);
        }
        else {
            Log.logError("HTTP "+resp.rc+" "+resp.responseBody);
            return null;
        }

    }



}
