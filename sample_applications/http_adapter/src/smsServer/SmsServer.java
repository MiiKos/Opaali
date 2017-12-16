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
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        volatile PrintWriter fw = null;
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
    
    /*
     * a LogWriter that writes log to a file that has current date added to the filename (or to stderr if opening file fails)
     */
    private static class RotatingFileLogger extends FileLogger {

        private final String savedFilename;
        private LocalDate current = LocalDate.now();

        RotatingFileLogger(String filename, boolean logStderr, boolean logAppend) {
            super(getDatedFilename(filename), logStderr, logAppend);
            savedFilename = filename;
            current = LocalDate.now();
        }

        /*
         * check if date in log file name should be changed
         */
        synchronized public void checkRotation() {
            if (LocalDate.now().isAfter(current)) {
                // day has changed, change log file name
                String newFilename = getDatedFilename(savedFilename);
                current = LocalDate.now();
                this.close();
                try {
                    fw = new PrintWriter(new BufferedWriter(new FileWriter(newFilename, logAppend)));
                } catch (IOException e) {
                    fw = null;
                    Log.logError("cannot set log_file to "+newFilename);
                }
            }

        }

        /*
         * insert today's date to given filename
         */
        private static String getDatedFilename(String filename) {
            String newFilename = "";
            LocalDate today = LocalDate.now();
            int i = filename.lastIndexOf('.');
            if (i > 0) {
                newFilename = filename.substring(0, i) + today + filename.substring(i);
            }
            else {
                newFilename = filename + today;
            }
            return newFilename;
        }

    }

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
                                Log.logInfo("CGW HTTP API started");
                            }
                        }
                    }
                    if (startServer) {
                        server.setExecutor(executor);
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


