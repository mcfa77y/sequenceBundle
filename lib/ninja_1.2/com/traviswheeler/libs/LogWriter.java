package com.traviswheeler.libs;



public class LogWriter {

	public static Logger logger = null;
	
	
	public static void setLogger(Logger l) {
		logger = l;
	}

	
	protected static boolean allowStdOut = false;
	
	
	public static void stdOutLog(String s) {
		if (allowStdOut) {
			System.out.print(s);
		} else if (logger != null) { 
			logger.log(s);	
		}
	}

	public static void stdOutLogln(String s) {
		if (allowStdOut) {
			System.out.println(s);
		} else if (logger != null) { 
			logger.logln(s);
		}
	}

	public static void stdErrLog(String s) {
		if (logger != null) {
			logger.log(s);
		}
	}

	public static void stdErrLogln(String s) {
		if (logger != null) {
			logger.logln(s);
		}
	}		
	
}
