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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import OpaaliAPI.Log;

/*
 * Mapping
 *  keyword => target URL template
 * or
 *  (service_address, keyword) => target URL template
 *
 * Mapping is read from a mapping configuration file
 *
 */
public class Mapping {


    Mapping(String fileName) {
        this.fileName = fileName;
        mapping = new NumberMap();
    }


    /*
     * read mappings from mapping file
     * returns true if successful, false otherwise
     */
    public boolean readMappings() {
        return readMappingFile(fileName);
    }

    /*
     * get a URL template based on the serviceAddress and keyword
     *
     */
    public String mapToUrl(String serviceAddress, String keyword) {
        return mapping.mapToUrl(serviceAddress, keyword);
    }

    // = end of public part ===============================================


    /*
     * Mapping from service address (or "number") to keyword mappings
     */
    private class NumberMap {

        NumberMap() {
            map = new HashMap<String, KeywordMap>();
        }

        /*
         * returns a URL template for the specified (serviceAddress, keyword) pair
         */
        public String mapToUrl(String serviceAddress, String keyword) {
            KeywordMap keywords = map.get(serviceAddress);
            if (keywords != null) {
                return keywords.map.getOrDefault(keyword, keywords.defaultRef);
            }
            else {
                // try searching for a serviceAddress independent mapping
                return defaultMap.mapToUrl(keyword);
            }
        }

        HashMap<String, KeywordMap> map = null;      // map from service number to keyword map
        KeywordMap defaultMap = new KeywordMap();    // default keyword map
        KeywordMap currentKeywords = defaultMap;     // current keyword map (for parsing the mapping file)
    }

    /*
     * Mapping from keyword to target URL template
     */
    private class KeywordMap {

        KeywordMap() {
            map = new HashMap<String, String>();
        }

        /*
         * returns a URL template for the specified keyword
         */
        public String mapToUrl(String keyword) {
            if (map != null) {
                return map.getOrDefault(keyword, defaultRef);
            }
            else {
                return defaultRef;
            }
        }

        HashMap<String, String> map = null;    // map from keyword to URL template
        String defaultRef = null;    // URL template for default keyword (or null if not specified)
    }

    // parser modes
    private static final int MODE_COMMENTLINE = 0;
    private static final int MODE_SERVICENUMBER = 1;
    private static final int MODE_MAPPING = 2;
    private static final String DEFAULT_KEY = "@";

    private int mode = MODE_COMMENTLINE;
    private final NumberMap mapping;
    private String fileName = null;
    private int lineCounter = 0;
    private String numbers = "";

    /*
     * read a mapping file and build the mappings
     */
    private boolean readMappingFile(String filename) {

        Log.logInfo("processing mapping file \""+filename+"\"");

        File file = new File(filename);
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                lineCounter++;
                processLine(line);
            }
            br.close();
        } catch (IOException e) {
            Log.logError("failed to read configuration file \""+filename+"\"");
            return false;
        }
        Log.logInfo("processing mapping file \""+filename+"\" completed");
        return true;
    }


    /*
     * process a single line from a mapping file
     */
    private void processLine(String line) {
        String s = line;
        s = s.trim();
        if (s.startsWith("#") || s.length() == 0) {
            // ignore comment lines and empty lines
            Log.logInfo("comment:"+s);
        }
        else if (s.startsWith("[")) {
            // new section starts
            String key = s.substring(1, s.indexOf(']'));
            // should we verify this is a number? maybe convert it to an int?
            if (mode != MODE_SERVICENUMBER) {
                mode = MODE_SERVICENUMBER;
                // create a new keyword map
                mapping.currentKeywords = new KeywordMap();
                numbers = null;
            }
            // store key into mapping, referencing the current keyword map
            if (mapping.map.putIfAbsent(key, mapping.currentKeywords) != null) {
                Log.logError("duplicate service number "+key+" on line "+lineCounter);
            }
            else {
                numbers = (numbers != null ? numbers+", "+key : key);
            }
        }
        else if (s.contains("=")) {
            // store a config entry
            String key = s.substring(0, s.indexOf('=')).trim().toLowerCase();
            String value = s.substring(s.indexOf('=')+1).trim();
            if (mapping == null) {
                // actually: mapping can NOT be null
                Log.logError("no service number for "+key+" on line "+lineCounter);
            }
            else {
                mode = MODE_MAPPING;
                if (DEFAULT_KEY.equalsIgnoreCase(key)) {
                    if (mapping.currentKeywords.defaultRef != null) {
                        Log.logError("duplicate default mapping on line "+lineCounter);
                    }
                    else {
                        mapping.currentKeywords.defaultRef = value;
                        Log.logInfo("adding default keyword to service number(s) "+(numbers.length() > 0 ? numbers : "<default>"));
                    }
                }
                else if (mapping.currentKeywords.map.putIfAbsent(key, value) != null) {
                    Log.logError("duplicate keyword "+key+" on line "+lineCounter);
                }
                else {
                    Log.logInfo("adding keyword "+key+" to service number(s) "+(numbers.length() > 0 ? numbers : "<default>"));
                }
            }
        }
        else {
            Log.logWarning("unrecognized line:\""+s+"\"");
        }
    }

}
