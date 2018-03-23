/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2018 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */

package smsServer;

import OpaaliAPI.Log;

/*
 * utility for writing http request strings to log with details masked out
 */
public class RequestLogger {

    private StrMask[] logMasks = null;

    public RequestLogger(StrMask[] logMasks) {
        this.logMasks = logMasks;
    }


    /*
     * returns current time in ms for use in timing
     */
    public static long getTimeNow() {
        return java.util.Calendar.getInstance().getTimeInMillis();
    }

    /*
     * making a log entry about a http request
     */
    public void log(String request, int respCode, int port, long ms) {
        StringBuilder sb = new StringBuilder();
        if (port >= 0) {
            sb.append("http request(port ").append(port).append("): ");
        }
        else {
            sb.append("http request(outbound): ");
        }
        sb.append(logMasks != null ? StrMask.applyMasks(request, logMasks) : request);
        sb.append(" (response ");
        if (ms >= 0) {
            sb.append("in ");
            sb.append(ms);
            sb.append("ms");
        }
        if (respCode >= 0) {
            sb.append(':');
            sb.append(respCode);
            if (respCode == 200)
                sb.append(" OK");
            else if (respCode == 204)
                sb.append(" No Content");
            else if (respCode == 401)
                sb.append(" Unauthorized");
        }
        sb.append(')');
        Log.logInfo(sb.toString());
    }

}
