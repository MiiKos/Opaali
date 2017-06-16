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
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import com.sun.net.httpserver.HttpServer;

import OpaaliAPI.Log;

public class SmsServer {


    public SmsServer(String configFile) {
        /*
         * read service configuration from config file and assign handlers
         * for configured service types
         */

        
        // read server configuration
        ServerConfig sc = new ServerConfig(configFile);
        if (sc.isValid()) {
            // get port from default config
            int port = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_PORT);
            
            if (port >= 0) {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

                    // create handler for known service types that are configured
                
                    String[] services = sc.listServices();
                    for (String serviceName: services) {
                        ServiceConfig svc = sc.getServiceConfig(serviceName);
                        String serviceType = svc.get("serviceType");
                        if (serviceType != null) {
                            if (ServerConfig.SERVICE_TYPE_SEND.equalsIgnoreCase(serviceType)) {
                                server.createContext("/"+serviceName, new CgwHttpApiHandler(svc)); 
                                server.setExecutor(null);
                                Log.logDebug("CGW HTTP API started");
                                server.start();
                            }
                        }
                    }
                    
                } catch (IOException e) {
                    Log.logError("CGW HTTP API failed to start");
                    e.printStackTrace();
                }
            }
        }
        else {
            // config error regarding port
            Log.logError("service port is not configured");
        }
    
    }
    
        
        
   /*
    * 
    * command line syntax
    * java SmsServer configfile 
    * 
    */
    
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Log.setLogLevel(Log.ALL);
        Log.logInfo("Default CharSet:"+Charset.defaultCharset());
        
        SmsServer server = new SmsServer(args.length > 0 ? args[0] : "config.txt");
        
    }

}


