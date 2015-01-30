package com.traviswheeler.ninja;

import java.io.*;
import java.util.Vector;

import com.traviswheeler.libs.LogWriter;

public class SequenceFileReader {
	
	public static enum AlphabetType {dna, amino};
	
	char[][] seqs;	
	String[] names;
	AlphabetType alphType;

	
	public SequenceFileReader (String filename, AlphabetType alphTypeIn) {
		
		InputStream is = null;
		char[] dna_chars = {'A','G','C','T','U'}; 
		
		
		try {
			if (null == filename) {
				
				is = System.in;
				int x = 0;
				try {
					x= is.available();					
				} catch ( IOException e) {
					LogWriter.stdErrLogln("Error reading from STDIN");
					throw new GenericNinjaException(e.getMessage());			
				}

				if (x==0) {  //STILL??  what am I supposed to align?
					LogWriter.stdErrLogln("No sequences to align. Quitting");
					//logger.printUsage();
					throw new GenericNinjaException("No sequences to align. Quitting");
				}

			} else {
				is = new FileInputStream(filename);
			}
		} catch (FileNotFoundException e) {
			LogWriter.stdErrLogln("The file '" + filename + "' cannot be found.  Qutting");
			throw new GenericNinjaException("The file '" + filename + "' cannot be found.  Qutting");
		}
	
		byte b[];
		try {
			int x= is.available();
			b = new byte[x];
			is.read(b);			
		} catch ( IOException e) {
			LogWriter.stdErrLogln("Error reading file '" + filename + "'");
			throw new GenericNinjaException(e.getMessage());			
		}
		
		Vector<String> namesV = new Vector<String>();
		Vector<char[]> charsV = new Vector<char[]>();
		StringBuffer namebuffer = new StringBuffer();
		boolean inname = false;
		char[] tmpChar = new char[10000]; //don't expect to see sequences this long
		int tmpCharLen = 0;
		boolean started = false;
		for (int i=0; i<b.length; i++) {
			if (b[i] == '>') {
				if (started) { //push the previous char[] onto the charsV vector
					char[] ch = new char[tmpCharLen];
					for (int j=0; j<tmpCharLen; j++)
						ch[j] = tmpChar[j];
					charsV.add(ch);		
					tmpCharLen = 0;
				} else {
					started = true;
				}
				i++;
				namebuffer.append((char)b[i]);
				inname = true;
			} else if (inname) {
				if (b[i] == ' ' || b[i] == '\t' || b[i] == '\n') {
					namesV.add(namebuffer.toString());
					namebuffer.delete(0, 10000);
					while (b[i] != '\n') i++; // spin to end of name line
					inname = false;
				} else {
					namebuffer.append((char)(b[i]));
				}
			} else if (b[i] != ' ' && b[i] != '\r' && b[i] != '\n' ) {
				if (b[i] == '.') 
					tmpChar[tmpCharLen++] = '-';
				else 
					tmpChar[tmpCharLen++] = (char)(b[i]);
			}
		}
		//last char[]
		char[] ch = new char[tmpCharLen];
		for (int j=0; j<tmpCharLen; j++)
			ch[j] = tmpChar[j];
		charsV.add(ch);		

		
		names = new String[namesV.size()];
		char[][] tmpChars = new char[namesV.size()][];
		for (int i=0; i<namesV.size(); i++) {
			names[i] = namesV.elementAt(i);
			tmpChars[i] = charsV.elementAt(i);
		}
		namesV = null;
		charsV = null;
		
		//clean out columns with only dashes; check alphabet_type; possibly convert U->T
		int k = 0;
		if (alphTypeIn != null)
			alphType = alphTypeIn;
		else
			alphType = AlphabetType.dna;
						
		for (int i=0; i<tmpChars[0].length; i++){ //columns
			boolean good = false;
			for (int j=0; j<tmpChars.length; j++) {
				if (tmpChars[j][i] != '-') {
					good = true;
					break;
				}
			}
			if (good) { 
				for (int j=0; j<tmpChars.length; j++) {
					tmpChars[j][k] = tmpChars[j][i]; 
					if ( alphTypeIn == null && tmpChars[j][k] != '-' && alphType == AlphabetType.dna) {
						boolean match = false;
						for (int c=0; c<dna_chars.length; c++) {
							if (tmpChars[j][k] == dna_chars[c]) {
								match = true;
								break;
							}
						}
						if (!match) 
							alphType = AlphabetType.amino;
					}							
				}
				k++;
			}			
		}
	
		seqs = new char[tmpChars.length][k];
		for (int i=0; i<k; i++) { //columns
			for (int j=0; j<tmpChars.length; j++) {
				seqs[j][i] = Character.toUpperCase( tmpChars[j][i] );
				if (alphType == AlphabetType.dna && seqs[j][i] == 'U')
					seqs[j][i] = 'T';
			}
		}
	}
	
	public char[][] getSeqs () {
		return seqs;
	}

	public String[] getNames () {
		return names;
	}
	public AlphabetType getAlphType () {
		return alphType;
	}
}
