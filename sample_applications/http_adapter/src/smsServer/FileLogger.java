package smsServer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import OpaaliAPI.Log;
import OpaaliAPI.LogWriter;

public class FileLogger implements LogWriter {

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

