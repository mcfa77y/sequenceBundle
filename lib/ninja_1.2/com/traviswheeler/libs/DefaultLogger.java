package com.traviswheeler.libs;

public class DefaultLogger extends LogWriter implements Logger {

	public DefaultLogger () {
		allowStdOut = true;
	}
	
	public void log(String s) {
		System.err.print(s);
	}

	public void logln(String s) {
		System.err.println(s);
	}
	
}
