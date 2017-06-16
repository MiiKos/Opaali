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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    public static final int NONE = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int INFO = 3;
    public static final int DEBUG = 4;
    public static final int ALL = 4;
    
    
    public static void setLogLevel(int level) {
        logLevel = level;
    }
    
    public static void setLogger(LogWriter logWriter) {
        logger = logWriter;
    }

    public static void log(int level, String s) {
        if (level <= logLevel) {
            String tag;
            switch (level) {
            case ERROR:
                tag = "ERROR";
                break;
            case WARNING:
                tag = "WARNING";
                break;
            case INFO:
                tag = "INFO";
                break;
            case DEBUG:
            default:
                tag = "DEBUG";
                break;
            }
            logger.logWrite(timestamp()+tag+" "+s);
        }
    }
    
    public static void logError(String s) {
        log(ERROR, s);
    }

    public static void logWarning(String s) {
        log(WARNING, s);
    }
    
    public static void logInfo(String s) {
        log(INFO, s);
    }
    
    public static void logDebug(String s) {
        log(DEBUG, s);
    }
    

    //= end of public part ============================================
    
    private static String timestamp() {
          LocalDateTime date = LocalDateTime.now();
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS ");
          return date.format(formatter);
    }
    
    private static class defaultLogger implements LogWriter {

        @Override
        public void logWrite(String s) {
            System.err.println(s);
        }
        
    }
    
    private static int logLevel = NONE;
    private static LogWriter logger = new defaultLogger();
    
}
