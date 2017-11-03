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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import com.sun.net.httpserver.HttpServer;

import OpaaliAPI.Log;
import OpaaliAPI.LogWriter;

/*
 * smsServer: smsServer
 * starts a http server that passes requests between an application and OpaaliAPI
 */

public class SmsServer {

    /*
     * a LogWriter that writes log to a file (or to stderr if opening file fails)
     */
    private static class FileLogger implements LogWriter {

        PrintWriter fw = null;
        boolean logStderr = true;    // always log to stderr, too
        boolean logAppend = true;    // append to existing log file
        
        FileLogger(String filename, boolean logStderr, boolean logAppend) {
            this.logStderr = logStderr;
            this.logAppend = logAppend;
            try {
                fw = new PrintWriter(new BufferedWriter(new FileWriter(filename, logAppend)));
            } catch (IOException e) {
                fw = null;
                Log.logError("cannot set log_file to "+filename);
            }
        }
        
        @Override
        synchronized public void logWrite(String s) {
            if (logStderr) {
                System.err.println(s);
            }
            if (fw != null) {
                fw.println(s);
                fw.flush();
            }
        }
        
        public void close() {
            if (fw != null) {
                fw.flush();
                fw.close();
                fw = null;
            }
        }
        
    }
    
    static FileLogger fl = null;


    public SmsServer(String configFile) {
        /*
         * read service configuration from config file and assign handlers
         * for configured service types
         */

    	boolean startServer = false;
        
        // read server configuration
        ServerConfig sc = new ServerConfig(configFile);
        if (sc.isValid()) {
            // set log_file and log_level
            int logLevel = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_LEVEL);                    

            if (logLevel >= 0) {
                Log.setLogLevel(logLevel);
            }

            int logStderr = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_STDERR);                    

            if (logStderr <0) {
                logStderr = 1;
            }

            int logAppend = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_APPEND);                    

            if (logAppend <0) {
                logAppend = 1;
            }
            
            String logFile = sc.getServiceConfig(null).getConfigEntry(ServerConfig.CONFIG_LOG_FILE);
            
            if (logFile != null) {
                Log.setLogger(fl = new FileLogger(logFile, (logStderr == 0 ? false : true), (logAppend == 0 ? false : true)));
            }
            

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
                                startServer = true;
                                Log.logInfo("CGW HTTP API started");
                            }
                        }
                    }
                    if (startServer) {
                        server.setExecutor(null);
                        server.start();
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
        
        Log.setLogLevel(Log.ALL);
        Log.logInfo("Default CharSet:"+Charset.defaultCharset());
        
        SmsServer server = new SmsServer(args.length > 0 ? args[0] : "config.txt");
        
        // add shutdown handler to write a log entry about shutting down 
        // (this only works if the JVM exits when this application exits) 
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Log.logInfo("shutting down...");
                Log.logInfo("----------------");
                if (fl != null) {
                    fl.close();
                    fl = null;
                }
            }
        });
    }

}


