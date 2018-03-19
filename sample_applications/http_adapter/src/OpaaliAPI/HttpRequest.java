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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

// TODO: clean up

public class HttpRequest {

    
    public HttpRequest() {}

    /*
     * makes a http request using the provided template
     * which contains the lines of the complete request
     * as a string array formatted as
     * Request-Line
     * Request-Header-Lines
     * empty line
     * body (optional)
     */
    public static HttpResponse makeRequest(String[] tmpl) {
        int rc = HttpURLConnection.HTTP_INTERNAL_ERROR;
        HashMap<String, String> headers = null;
        String responseBody = null;

                
        // split request line into its components
        // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
        String[] s = Pattern.compile(" +").split(tmpl[0]);
        if (s.length != 3) {
            Log.logError("invalid request line:"+tmpl[0]);
            return null;
        }

        String requestMethod = s[0];
        String requestURL = s[1];
        String protocolVersion = s[2];
        String charSet = "UTF-8"; //"ISO-8859-15";
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(requestURL).openConnection();

            Log.logDebug(">"+tmpl[0]);

            // process http headers up to a blank line
            int line=1;
            Pattern p = Pattern.compile(": *");
            while (line < tmpl.length && tmpl[line++].length() > 0) {
                String[] hdr = p.split(tmpl[line-1]);
                String key = hdr[0];
                String value = hdr[1];
                conn.setRequestProperty(key, value);
                Log.logDebug(">"+key+": "+value);
                // snoop character set from headers
                if ("Content-Type".equalsIgnoreCase(key)) {
                    charSet = extractCharSet(value, charSet);
                }
            }
            Log.logDebug(">"+tmpl[line-1]);
  
            if ("POST".equals(requestMethod) || "PUT".equals(requestMethod)) {
                conn.setRequestMethod(requestMethod);
                conn.setDoOutput(true);

                // the rest is an optional request body
                StringBuilder body = new StringBuilder(); 
                while (line < tmpl.length) {
                    body.append(tmpl[line++].trim());
                }
                String bodyStr = body.toString().substring(0, body.toString().length());
                Log.logDebug(">"+bodyStr);

                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), charSet);
                out.write(bodyStr);
                Log.logDebug("body in bytes:"+dumpbytes(bodyStr.getBytes(charSet)));
                out.close();

            }
            else {
                conn.setRequestMethod(requestMethod);
                conn.setDoOutput(false);
            }

            
            // get response status
            try {
                rc = conn.getResponseCode();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.logDebug("...ignoring");
            }
            
            // get optional response headers
            headers = new HashMap<String, String>();
            for (int n=0; true; n++) {
                String key = conn.getHeaderFieldKey(n);
                String value = conn.getHeaderField(n);
                if (key != null || value != null) {
                    Log.logDebug("<"+(key != null ? key+": " : "")+value);
                    headers.put(key, value);
                }
                else {
                    break;
                }
            }
            
            // get response body (if one exists)
            if (conn.getContentLengthLong() != 0) {
                String inputLine = null;
                StringBuilder respBody = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader((rc < 400 ? conn.getInputStream() : conn.getErrorStream()),charSet));
                while ((inputLine = in.readLine()) != null) {
                    respBody.append(inputLine);
                }
                in.close();
                responseBody = respBody.toString();
                Log.logDebug("<"+responseBody);
            }
        
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ConnectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new HttpResponse(rc, headers, responseBody);
    }

    
    /*
     * extract character set from headers
     */
    private static String extractCharSet(String line, String defCharSet) {
        String charset = defCharSet; // default: Latin-9 aka ISO-8859-15
        int a = line.indexOf(';');
        if (a >= 0) {
            int b = line.substring(a).toLowerCase().indexOf("charset=");
            if (b > 0) {
                charset = line.substring(a + b + 8).trim();
            }
        }
        return charset;
    }
    

    // dump bytes just for debug purposes
    private static String dumpbytes(byte[] b) {
        String s = "";
        for (byte bb : b) {
            s += Byte.toUnsignedInt(bb) + "("+(char) bb +"), ";
        }
        return s;
    }
    

}
