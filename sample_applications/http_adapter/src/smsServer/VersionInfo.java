package smsServer;

public class VersionInfo {
    public static String versionString = "(unknown)";
    
    public static void setVersionInfo(String s) {
    	versionString = (s != null ? s : versionString);
    }
}
