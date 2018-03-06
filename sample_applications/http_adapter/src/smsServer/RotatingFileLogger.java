package smsServer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

import OpaaliAPI.Log;


/*
 * a LogWriter that writes log to a file that has current date added to the filename (or to stderr if opening file fails)
 */
public class RotatingFileLogger extends FileLogger {

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


