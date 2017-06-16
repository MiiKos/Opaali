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
		 * 200 OK
		 * 400 BAD REQUEST
		 * 401 AUTHENTICATION FAILURE
		 */
		HttpResponse resp = null;
        int retries = 0;
        do {
			resp = HttpRequest.makeRequest(tmpl.expand(vars).toStrings());
			switch (resp.rc) {
				case 200: // OK
					break;
				case 400: // BAD REQUEST
					break;
				case 401: // UNAUTHERIZED
					// re-authenticate once in case access_token has expired
					retries = 1;
					vars.put("ACCESS_TOKEN", access_token.authenticate());
					break;
				default:
					break;
			}
        } while (retries-- > 0);
		
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
        	    default:
        	    	str.append(c);
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

	

	public class ReceiptRequest {
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
