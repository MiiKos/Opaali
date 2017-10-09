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

/*
 * This is a convenience class for starting up one or more instances of the sms server
 * 
 * if no arguments are given tries to start one instance with configuration from file default configuration file (config.txt)
 * 
 * If names of one or more configuration files are given as arguments starts one instance for each config file
 * 
 */

public class SmsServer {

	public static void main(String[] args) {
		if (args.length == 0) {
			smsServer.SmsServer.main(args);
		}
		else if ("-v".equals(args[0])) {
			System.err.println(VersionInfo.versionString);
		}
		else {
			/*
			 * starting multiple servers is not supported
			 * because the current Log implementation is not compatible with that
			for (String s : args ) {
				String[] a = {s};
				smsServer.SmsServer.main(a);
			}
			*/
			if (args.length == 1) {
				smsServer.SmsServer.main(args);
			}
			else {
				System.err.println("usage: java -jar SmsServer.jar [configurationfile]");
				System.err.println("       default configurationfile is ./config.txt");
			}
		}

	}

}
