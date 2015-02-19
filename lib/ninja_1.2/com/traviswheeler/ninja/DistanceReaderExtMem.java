package com.traviswheeler.ninja;

import java.io.*;

import com.traviswheeler.libs.Arrays;
import com.traviswheeler.libs.LogWriter;


public class DistanceReaderExtMem {

	public static int numPages = 10;
	int K = 0;
	BufferedReader r;
	final int floatSize = 4;
	float[][] inD = null;
	DistanceCalculator distCalc = null;
	
	public DistanceReaderExtMem (	String fileName )  throws Exception {
		  
		
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
				LogWriter.stdErrLogln("No distance file provided. Quitting.");
				throw new Exception();			
			}
			r = new BufferedReader(new InputStreamReader(is));
		} else {
			try {
				r = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException e) {
				LogWriter.stdErrLogln("The file '" + fileName + "' is invalid.  Qutting");
				throw e;
			}
		}

		
	    String line = r.readLine();
	    line = line.trim();
	    K = Integer.parseInt(line);
	    
	    if ( ! (line.equals(""+K) )) {
	    	LogWriter.stdErrLogln("Invalid format for input file. Please use Phylip distance matrix format. Qutting");
			throw new GenericNinjaException("Invalid input file format.");
	    }
	    
	}
	
	public DistanceReaderExtMem ( float[][] inD )  { // used when the distance matrix is handed to the "reader" 
		
		if (inD == null){
	    	LogWriter.stdErrLogln("Null distance matrix handed to external matrix reader. Qutting.");
	    	throw new GenericNinjaException("Null distance matrix.");
		}
		
		this.inD = inD;
		K = inD.length;
	}
	

	public DistanceReaderExtMem ( DistanceCalculator distCalc, int K )  { // used when the distance matrix is handed to the "reader" 
				
		if (distCalc == null){
	    	LogWriter.stdErrLogln("Null distance calculator handed to external matrix reader. Qutting.");
	    	throw new GenericNinjaException("Null distance calculator.");
		}
		
		this.distCalc = distCalc;
		this.K = K;
	}	
	
	
	public int read (String[] names, float[] R, RandomAccessFile diskD, float[][] memD, int rowLength, 
				int pageBlockSize) throws Exception {

	    int row = 0;
	    
	    
	    //I want to write a hunk of the data to memD instead of disk. 
	    //Find a point where memD is partly full, then scale back a bit, 
	    //so what would otherwise be an incomplete block on disk isn't written
	    // (note: numColsToDisk must be <= K)
	    int numColsToDisk = 0;
	    if (memD[0].length < 2*K-2) {
	    	numColsToDisk = 2*K - 2 - (memD[0].length/2)  ; 
		    int numBlocksToDisk = numColsToDisk/pageBlockSize;
		    numColsToDisk = numBlocksToDisk*pageBlockSize;
		    if (numColsToDisk > K) numColsToDisk = K;
	    }

	    
	    int currCol = 0;
	    
	    int buffSize = numPages*pageBlockSize; 
	    
	    char[] buff = new char[buffSize];
	    char token[] = new char[50];

	    int buffPtr=0;
	    int tokenPtr=0;

    	float d;
    	long diskPos;

    	float[] fBuff = new float[numColsToDisk];
    	byte[] bBuff = new byte[4*numColsToDisk];
    	int floatsInBuff = 0;
    	
	    boolean go;
    	
	    
	    if (inD == null && distCalc == null)  {
	    	r.read(buff);
	    
		    while (row != K && (buffPtr<buffSize || r.ready())) {	
		    	
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
		    	names[row] = new String(token,0,tokenPtr);
				if (names[row] == null) {
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
				
		    	
				for (int col=0; col<K; col++) {
					
			    	//fetch token, which is the next number	    	
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
			    	};
				
			    	
			    	//d = Float.parseFloat(new String(token,0,tokenPtr));
			    	// replace with my hand-rolled atof function
			    	d = atof(token,tokenPtr-1);
			    	
			    	
					R[row] += d;
					
//					LogWriter.stdErrLog("row " + row + ", d = " + d + ";  R = " + R[row] + "\n");
			    	
					if (currCol<numColsToDisk)  {
											
						fBuff[floatsInBuff++] = d;
				    	if (floatsInBuff == numColsToDisk ) {	
	
				    		Arrays.floatToByte(fBuff, bBuff);
						
					    	diskPos = (long)floatSize * rowLength * row ;
					    	diskD.seek(diskPos);			    	
					    	diskD.write(bBuff);
	
					    	floatsInBuff = 0;
					    	
				    	}		    	
					} else {
						memD[row][currCol-numColsToDisk] = d;
					}
					
					
					currCol++;
			    	
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
				
				row++;
		        currCol=0;
		    }
	    
		    if (row < K) {			
		    	LogWriter.stdErrLogln("too few lines in distance matrix file" );
		    	throw new Exception();
		    }
	    } else {

	    	while (row != K && buffPtr<buffSize) {
	    	
				for (int col=0; col<K; col++) {

					if (distCalc != null ) 
						d = ((float)Math.round((float)(10000000 * distCalc.calc(row, col))))/10000000;					
					else /// using inD 
						d = inD[row][col];
			    			    	
					R[row] += d;
					
					if (currCol<numColsToDisk)  {
											
						fBuff[floatsInBuff++] = d;
				    	if (floatsInBuff == numColsToDisk ) {	
	
				    		Arrays.floatToByte(fBuff, bBuff);
						
					    	diskPos = (long)floatSize * rowLength * row ;
					    	diskD.seek(diskPos);			    	
					    	diskD.write(bBuff);
	
					    	floatsInBuff = 0;
					    	
				    	}		    	
					} else {
						memD[row][currCol-numColsToDisk] = d;
					}
					
					
					currCol++;
			    	
					tokenPtr = 0;
					
				}
			
				row++;
		        currCol=0;
		    }
	    
	    }
		
		return numColsToDisk;
		
	}
	
	private float atof (char[] in, int end) throws Exception {
		int numerator = 0;
		int pos = end;
		int multiplier = 1;
		int decimalPos = -1;
		boolean isNeg = false;
		
		while (pos>=0) {
			if (in[pos]=='.') { // decimal
				if (decimalPos == -1) 
					decimalPos = pos;
				else
					throw new Exception("Unable to convert float, because multiple decimal points found in a single number (" + String.valueOf(in) + ")");
			} else if (in[pos] == '-' ) {
					if (isNeg)
						throw new Exception("Unable to convert float, because multiple '-' found in a single number (" + String.valueOf(in) + ")");
					isNeg = true;
			} else {
				if (in[pos]<'0' || in[pos]>'9')
					throw new Exception("Unable to convert float, invalid character found: '" + in[pos] + "' (" + String.valueOf(in) + ")" );
				
				numerator += multiplier * (in[pos]-'0');
				multiplier *= 10;
			}
			pos--;
		}
		
		float val = numerator;
		
		if (decimalPos != -1 && decimalPos != end) {
			int digits = end-decimalPos; 	
			
			int denom;
			switch (digits)  {
		     
				case 1:
					denom = 10;
					break;
				case 2:
					denom = 100;
					break;		
				case 3:
					denom = 1000;
					break;
				case 4:
					denom = 10000;
					break;
				case 5:
					denom = 100000;
					break;			
				case 6:
					denom = 1000000;
					break;
				case 7:
					denom = 10000000;
					break;
				default:
					denom = (int)Math.pow(10, end-decimalPos);
					break;
			}
			val /= denom; 

		}
		
		if (isNeg)
			return 0 - val;
		else 
			return val;
	}

}
