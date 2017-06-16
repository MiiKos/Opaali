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

// configuration data variables
public class Config {
    
    public static HashMap <String, String> getConfig() {
        return (HashMap<String, String>) configSettings.clone();
    }
    
    public static void setConfig(HashMap <String, String> config) {
        configSettings.putAll(config);
    }
    
    public static HashMap <String, String> getServiceConfig(String serviceName) {
        HashMap<String, String> h = (HashMap<String, String>) configSettings.clone();
        h.putAll(serviceSettings.get(serviceName));
        return h;
    }
    
    public static void setServiceConfig(String serviceName, HashMap <String, String> config) {
        serviceSettings.put(serviceName, config);
    }
    
    // = end of public part ===================================================
    
    private static HashMap <String, String> configSettings; 
    private static HashMap <String, HashMap <String, String>> serviceSettings;
    
    static {
        // set default config
        configSettings = new HashMap <String, String>();
        configSettings.put("API_HOST", "api.sonera.fi");
        serviceSettings = new HashMap <String, HashMap<String, String>>();
    }
    
    
}
