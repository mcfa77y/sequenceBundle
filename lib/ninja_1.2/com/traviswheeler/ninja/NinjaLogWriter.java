package com.traviswheeler.ninja;

import com.traviswheeler.libs.LogWriter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.traviswheeler.ninja.GenericNinjaException;


public class NinjaLogWriter extends LogWriter {

	private static String version = "";
	
	
	public NinjaLogWriter () {
		Package p = this.getClass().getPackage();	
		version = p.getImplementationVersion();
	}

	public static void printVersion ( ) {
		NinjaLogWriter l = new NinjaLogWriter(); // to assign value to version variable
		logger.logln( "\nNinja v" + version + " by Travis Wheeler ");
		logger.logln( "\nhttp://nimbletwist.com/software/ninja/\n" );
		
		if (TreeBuilder.verbose>1) {
			LogWriter.stdErrLogln("Total Memory "+Runtime.getRuntime().totalMemory());    
			LogWriter.stdErrLogln("Free Memory "+Runtime.getRuntime().freeMemory());
		}
		
		 
		logger.logln( "Please cite:\n" +
		"Wheeler, T.J. 2009. Large-scale neighbor-joining with NINJA.\n" +  
		"In S.L. Salzberg and T. Warnow (Eds.), Proceedings of \n" +
		"the 9th Workshop on Algorithms in Bioinformatics. \n" +
		"WABI 2009, pp. 375-389. Springer, Berlin.\n");
	}
	
	public static void printUsage () {
		printVersion();
		
		try {
			InputStream is = Ninja.class.getResourceAsStream("usage.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
		    while (null != (line = br.readLine())) {
		    	logger.logln(line);
		     }
		} catch (FileNotFoundException e) {		
			logger.logln("The usage file cannot be found.  Qutting");
			throw new GenericNinjaException("The usage file cannot be found.  Qutting");
		} catch (IOException e) {		
			logger.logln("Problem reading usage file cannot be found.  Qutting");
			throw new GenericNinjaException("Problem reading usage file cannot be found.  Qutting");
		}
		
		logger.logln("");
	}

	
}
