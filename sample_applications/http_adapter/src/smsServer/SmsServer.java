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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import OpaaliAPI.Log;

/*
 * smsServer: smsServer
 * starts a http server that passes requests between an application and OpaaliAPI
 */

public class SmsServer {


    /*
     * to be called by each HttpHandler when a http request is received 
     * for triggering log rotation if day has changed
     *
     */
    public static void checkLogRotation() {
        if (fl != null && fl instanceof RotatingFileLogger) {
            ((RotatingFileLogger) fl).checkRotation();
        }
    }

    private static FileLogger fl = null;


    public SmsServer(String configFile) {
        this(configFile, "version unknown");
    }


    public SmsServer(String configFile, String versionString) {
        /*
         * read service configuration from config file and assign handlers
         * for configured service types
         */

        boolean startServer = false;
        ExecutorService executor = null;
        
        // read server configuration
        ServerConfig sc = new ServerConfig(configFile);
        if (sc.isValid()) {
            // set log_file and log_level
            int logLevel = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_LEVEL);                    

            if (logLevel >= 0) {
                Log.setLogLevel(logLevel);
            }

            int logStderr = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_STDERR);                    

            if (logStderr < 0) {
                logStderr = 1;
            }

            int logAppend = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_APPEND);                    
            int logRotate = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_LOG_ROTATE);

            if (logAppend < 0 || logRotate > 0) {
                // default is append, logRotate implies logAppend
                logAppend = 1;
            }
            
            String logFile = sc.getServiceConfig(null).getConfigEntry(ServerConfig.CONFIG_LOG_FILE);
            
            if (logFile != null) {
                if (logRotate > 0) {
                    Log.setLogger(fl = new RotatingFileLogger(logFile, (logStderr == 0 ? false : true), (logAppend == 0 ? false : true)));
                }
                else {
                    Log.setLogger(fl = new FileLogger(logFile, (logStderr == 0 ? false : true), (logAppend == 0 ? false : true)));
                }
            }
            
            // log startup message as soon as we have logger set
            versionString = VersionInfo.versionString;
            Log.logInfo("starting up ("+versionString+")");

            // get (optional) threadPoolSize
            int threadPoolSize = sc.getServiceConfig(null).getConfigEntryInt(ServerConfig.CONFIG_THREADPOOLSIZE);
            if (threadPoolSize > 0) {
                executor = Executors.newFixedThreadPool(threadPoolSize);
                Log.logInfo("created a threadpool of "+threadPoolSize+ " threads");
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
                                Log.logInfo("CGW HTTP API started at port "+port+", path /"+serviceName);
                            }
                            else {
                                Log.logWarning("unknown service ["+serviceName+":"+serviceType+"] ignored");
                            }
                        }
                    }
                    if (startServer) {
                        server.setExecutor(executor);
                        server.start();
                    }
                    
                } catch (IOException e) {
                    Log.logError("HTTP SERVER at port "+port+" failed to start");
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
        
        SmsServer server = new SmsServer(args.length > 0 ? args[0] : "config.txt", args.length > 1 ? args[1] : "unknown version");


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


