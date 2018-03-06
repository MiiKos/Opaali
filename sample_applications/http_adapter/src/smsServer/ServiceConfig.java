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

import java.util.HashMap;

public class ServiceConfig extends HashMap<String, String> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    ServiceConfig(String serviceName, HashMap <String, String> config) {
        if (config != null) {
            this.putAll(config);
        }
        this.serviceName = serviceName;
    }
    
    /*
     * returns true if there is a valid config
     */
    public boolean isValid() {
        return isValid;
    }

    
    /*
     * set the validity of the config
     * only the service by itself knows if its config is valid or not,
     * the service can use this to signal that it cannot work due
     * to misconfiguration
     */
    public void setValidity(boolean validity) {
        this.isValid = validity;
    }


    public String getServiceName() {
        return serviceName;
    }
    

    public String getServiceType() {
        if (serviceName != null) {
            if (serviceType == null) {
                serviceType = this.get("serviceType");
            }
            return serviceType;
        }
        return null;
    }

     public String getConfigEntry(String key) {
        return this.get(key);
    }
    
    public int getConfigEntryInt(String key) {
        String s = this.get(key);
        if (s != null) {
            return Integer.parseInt(s);
        }
        return -1;
    }
    
    //= end of public part ====================================================

    private ServiceConfig() {}
    
    private String serviceName = null;
    private String serviceType = null;
    private boolean isValid = false;
    
}
