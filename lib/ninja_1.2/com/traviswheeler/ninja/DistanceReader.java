package com.traviswheeler.ninja;

import java.io.*;
import com.traviswheeler.libs.LogWriter;


public class DistanceReader {

	public static int numPages = 10;
	int K = 0;
	BufferedReader r;
	DistanceCalculator distCalc = null;
	
	
	public DistanceReader (	String fileName )  throws Exception {
		
		
		if (null == fileName) {   // maybe it was streamed in?
			InputStream is = System.in;
			int x = 0;
			try {
				x= is.available();
			} catch ( IOException e) {
				LogWriter.stdErrLogln("Error reading from STDIN");
				LogWriter.stdErrLogln(e.getMessage());
				//System.exit(1);		
				throw e;
			}
			if (x==0) {  //STILL??  what am I supposed to read?
				//LogWriter.stdErrLogln("No distance file provided. Quitting.");
				throw new FileNotFoundException ("No distance file provided. Quitting.");			
			}
			r = new BufferedReader(new InputStreamReader(is));
		} else {
			try {
				r = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException e) {
				//LogWriter.stdErrLogln("The file '" + fileName + "' is invalid.  Qutting");
				throw new FileNotFoundException ("The file '" + fileName + "' is invalid.  Qutting");
			}
		}

		
	    String line = r.readLine();
	    line = line.trim();
	    K = Integer.parseInt(line);
	    
	    if ( ! (line.equals(""+K) )) {
	    	//LogWriter.stdErrLogln("Invalid format for input file. Please use Phylip distance matrix format. Qutting");
			throw new Exception("Invalid format for input file. Please use Phylip distance matrix format. Qutting");
	    }
	    
	}
	
	
	public DistanceReader ( DistanceCalculator distCalc, int K ) throws Exception { // used when the distance matrix is handed to the "reader" 
		
		if (distCalc == null){
	    	LogWriter.stdErrLogln("Null distance calculator handed to external matrix reader. Qutting.");
	    	throw new GenericNinjaException("Null distance calculator.");
		}
		
		this.distCalc = distCalc;
		this.K = K;
	}	
	
	public void read (String[] names, int[][] distances) throws Exception {

	    int cnt = 0;
	    
	    int buffSize = numPages*4096; 
	    
	    char[] buff = new char[buffSize];
	    char token[] = new char[50];

	    int i;
	    int buffPtr=0;
	    int tokenPtr=0;

	    boolean go;
	    if (distCalc != null) {//using distCalc on input alignment
	    	for (i=0; i<K; i++) 
	    		for (int j=i+1; j<K; j++) 
	    			distances[i][j-i-1] = 100 * (int)(((100000000*distCalc.calc(i,j))+50)/100) ; // this gets the same rounding I have in the distance writer code
	    		
	    } else { 

	    	r.read(buff);
		    	    
		    while (cnt != K && (buffPtr<buffSize || r.ready())) {	
		    	
		    	// spin past whitespace
				go = true;
		    	while (go) {
		    		if (buffPtr<buffSize) {
		    			if (buff[buffPtr] == ' ' || buff[buffPtr] == '\t' || buff[buffPtr] == '\n' || buff[buffPtr] == '\r') {
		    	    		buffPtr++;
		    			} else {
		    				go = false;
		    			}
		    		} else {
		    	    	r.read(buff);
		    	    	buffPtr = 0;
		    		}
		    	}
		    	
		    	//fetch first token, which is the name	    	
		    	go = true;
		    	while (go) {
		    		if (buffPtr<buffSize) {
		    			if (buff[buffPtr] == ' ' || buff[buffPtr] == '\t' || buff[buffPtr] == '\n' || buff[buffPtr] == '\r') {
		    				go = false;
		    			} else {
		    				token[tokenPtr++] = buff[buffPtr++];
		    			}
		    		} else {
		    	    	r.read(buff);
		    	    	buffPtr = 0;
		    		}
		    	}
		    	names[cnt] = new String(token,0,tokenPtr);
				if (names[cnt] == null) {
					LogWriter.stdErrLogln("empty name encountered while reading file");
					throw new Exception("empty name encountered while reading file");
				}
				tokenPtr = 0;
				
		    	// spin past whitespace
				go = true;
		    	while (go) {
		    		if (buffPtr<buffSize) {
		    			if (buff[buffPtr] == ' ' || buff[buffPtr] == '\t' || buff[buffPtr] == '\n' || buff[buffPtr] == '\r') {
		    	    		buffPtr++;
		    			} else {
		    				go = false;
		    			}
		    		} else {
		    	    	r.read(buff);
		    	    	buffPtr = 0;
		    		}
		    	}
				
		    	
				for (i=0; i<cnt; i++) {
					
			    	//fetch token, which is the next number	    	
			    	go = true;
			    	while (go) {
			    		if (buffPtr<buffSize) {
			    			if (buff[buffPtr] == ' ' || buff[buffPtr] == '\t' || buff[buffPtr] == '\n' || buff[buffPtr] == '\r') {
			    				go = false;
			    			} else if (buff[buffPtr] == '.') { // do this to make it a little quicker to read a number as an int
			    				buffPtr++;
			    			} else {
			    				token[tokenPtr++] = buff[buffPtr++];
			    			}
			    		} else {
			    	    	r.read(buff);
			    	    	buffPtr = 0;
			    		}
			    	};
			    	token[tokenPtr++] = '0';  //just appending this to create a little more "precision" for this int serving as a fixed-point number
			    	token[tokenPtr] = '0';
			    	
					//distances[i][cnt-i-1] = Integer.parseInt(new String(token,0,tokenPtr));
					//replace with a faster atoi call (which I have to roll myself ... why is such a function not available ???)
			    	distances[i][cnt-i-1] = atoi(token, tokenPtr);
					
					
					tokenPtr = 0;
					
			    	// spin past whitespace
					go = true;
			    	while (go) {
			    		if (buffPtr<buffSize) {
			    			if (buff[buffPtr] == ' ' || buff[buffPtr] == '\t' || buff[buffPtr] == '\n' || buff[buffPtr] == '\r') {
			    	    		buffPtr++;
			    			} else {
			    				go = false;
			    			}
			    		} else {
			    	    	r.read(buff);
			    	    	buffPtr = 0;
			    		}
			    	}
			    	
					//	LogWriter.stdErrLogln("too few numbers for row " + cnt + " in distance file");
					//	throw new Exception();
				}
	
		    	// spin until the next line
				go = true;
		    	while (go) {
		    		if (buffPtr<buffSize) {
		    			if ( buff[buffPtr] != '\n' && buff[buffPtr] != '\r') {
		    	    		buffPtr++;
		    			} else {
		    				go = false;
		    			}
		    		} else {
		    	    	r.read(buff);
		    	    	buffPtr = 0;
		    		}
		    	}
	
				
		        cnt++;
		    }
		    
			if (cnt < K) {			
				LogWriter.stdErrLogln("too few lines in distance matrix file" );
				throw new Exception();
			}

	    }
	    
	    
		
	}
	
	private int atoi (char[] in, int end) throws Exception {
		int val = 0;
		int pos = end;
		int multiplier = 1;
		boolean isNeg = false;
		while (pos>=0) {
			if (in[pos] == '-' ) {
				isNeg = true;
		 	} else if (in[pos]<'0' || in[pos]>'9') {	
				throw new Exception("Unable to convert integer from invalid char array. Position + " + pos + ", char = [" + in[pos] + "]");
			}
			
			val += multiplier * (in[pos]-'0');
			multiplier *= 10;
			pos--;
		}		
		
		if (isNeg)
			return 0 - val;
		else 
			return val;
	}
	
}
