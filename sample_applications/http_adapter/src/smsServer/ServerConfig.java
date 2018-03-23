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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import OpaaliAPI.Log;

public class ServerConfig {

    // supported smsserver service types
    public static final String SERVICE_TYPE_UNKNOWN = null;
    public static final String SERVICE_TYPE_SEND = "cgw";
    public static final String SERVICE_TYPE_DLR = "dlr";
    public static final String SERVICE_TYPE_RECEIVE = "receive";
    public static final String SERVICE_TYPE_QR = "qr";
    public static final String SERVICE_TYPE_INTERNALQ = "queue";
    public static final String SERVICE_DEFAULTCONFIG = null;

    // known config parameters
    public static final String CONFIG_PORT = "port";
    public static final String CONFIG_USERNAME = "applicationUserName";
    public static final String CONFIG_PASSWORD = "applicationPassword";
    public static final String CONFIG_CGWCHARSET = "cgwCharset";
    public static final String CONFIG_OPAALICHARSET = "opaaliCharset";
    public static final String CONFIG_DLR_URL = "DLR_URL";
    public static final String CONFIG_LOG_FILE = "log_file";
    public static final String CONFIG_LOG_LEVEL = "log_level";
    public static final String CONFIG_LOG_STDERR = "log_stderr";
    public static final String CONFIG_LOG_APPEND = "log_append";
    public static final String CONFIG_LOG_ROTATE = "log_rotate";
    public static final String CONFIG_LOG_MASK = "log_mask";
    public static final String CONFIG_THREADPOOLSIZE = "threadPoolSize";
    public static final String CONFIG_DEFAULTURL = "defaultUrl";
    public static final String CONFIG_DEFAULTREPLYURL = "defaultReplyUrl";
    public static final String CONFIG_MAPPINGFILE = "mappingFile";
    public static final String CONFIG_NOWAIT = "nowait";
    public static final String CONFIG_QSIZE = "queueSize";
    
    
    
    /*
     * create a default, minimal config
     */
    public ServerConfig() {
        defaultConfig = new ServiceConfig(null, null); 
        defaultConfig.put("API_HOST", "api.opaali.telia.fi");
        defaultConfig.put("log_level", "4");
        OpaaliAPI.Config.setConfig(defaultConfig);
    }
    
    
    /*
     * create config from lines of text 
     */
    public ServerConfig(String[] configLines) {
        parseConfigFile(configLines);
        OpaaliAPI.Config.setConfig(defaultConfig);
    }
    
    
    /*
     * create config from a file
     */
    public ServerConfig(String filename) {
        String[] configLines = readConfigFile(filename);
        if (configLines != null) {
            parseConfigFile(configLines);
            OpaaliAPI.Config.setConfig(defaultConfig);
        }
    }

    
    /*
     * returns true if there is a valid config
     */
    public boolean isValid() {
        return (defaultConfig != null);
    }
    

    /*
     * get service specific config or default config if there is none
     */
    public ServiceConfig getServiceConfig(String serviceName) {
        return (ServiceConfig) (serviceName == null ? defaultConfig : serviceConfig.get(serviceName));
    }
    

    /*
     * returns configured service names in an array of strings 
     */
    public String[] listServices() {
        ArrayList<String> a = new ArrayList<String>();
        if (serviceConfig != null) {
            for (Object s: serviceConfig.keySet()) {
                if (s != null) {
                    a.add(s.toString());
                }
            }
        }
        return a.toArray(new String[0]);
    }
    


    
    /*
     * read a (configuration) file into an array of strings
     */
    public static String[] readConfigFile(String filename) {
        ArrayList<String> lines = new ArrayList<String>();
        
        File file = new File(filename);
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
        } catch (IOException e) {
            Log.logError("failed to read configuration file \""+filename+"\"");
            return null;
        }
        Log.logInfo("processing configuration file \""+filename+"\"");
        return lines.toArray(new String[0]);
    }

    
    /*
     * extract config settings from a config "file" stored as an array of config file lines
     */
    public void parseConfigFile(String[] fileLines) {
        /*
         * default config are the name=value entries at the beginning 
         * of a config file before any section start
         * 
         * service specific config starts after a [section] line 
         */
        
        defaultConfig = new ServiceConfig(null, null);
        ServiceConfig current = defaultConfig;
        String sectionName = null;
        String serviceName = null;
        String serviceType = null;
        
        for (String s: fileLines) {
            s = s.trim();
            if (s.startsWith("#") || s.length() == 0) {
                // ignore comment lines and empty lines
                Log.logInfo("comment:"+s);
            }
            else if (s.startsWith("[")) {
                // new section starts
                // see if section already exists
                sectionName = s.substring(1, s.indexOf(']'));
                int i;
                if ((i = sectionName.indexOf(":")) >= 0) {
                    // a service entry in format [name:type]
                    serviceName = sectionName.substring(0, i).toLowerCase();
                    serviceType = sectionName.substring(i+1);
                    sectionName = serviceName;
                }
                if (serviceConfig == null || (current = serviceConfig.get(sectionName)) == null) {
                    current = new ServiceConfig(serviceName, null);
                    if (serviceConfig == null) {
                        serviceConfig = new HashMap<String, ServiceConfig>();
                    }
                    serviceConfig.put(sectionName, current);
                }
                if (serviceType != null) {
                    current.put("serviceType", serviceType);
                    serviceType = null;
                }
                Log.logInfo("section:"+s);
            }
            else if (s.contains("=")) {
                // store a config entry
                current.put(s.substring(0, s.indexOf('=')), s.substring(s.indexOf('=')+1));
                // avoid logging passwords unless in DEBUG mode
                if (s.substring(0, s.indexOf('=')).toLowerCase().contains("password") /*&& Log.le*/)
                    Log.logInfo("config :"+s.substring(0, s.indexOf('=')+1)+s.substring(s.indexOf('=')+1).replaceAll(".", "*"));
                else
                    Log.logInfo("config :"+s);
            }
            else {
                Log.logWarning("unrecognized config line:\""+s+"\"");
            }
        }
        Log.logInfo("end of configuration data reached");        
    }
    

    //= end of public part ====================================================
    
    private ServiceConfig defaultConfig = null;
    private HashMap<String, ServiceConfig> serviceConfig = null;
    
}
