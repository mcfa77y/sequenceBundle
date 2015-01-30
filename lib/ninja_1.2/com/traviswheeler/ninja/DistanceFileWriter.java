package com.traviswheeler.ninja;

import java.io.*;
import com.traviswheeler.ninja.DistanceCalculator;
import com.traviswheeler.ninja.DistanceCalculator.*;
import com.traviswheeler.ninja.SequenceFileReader.AlphabetType;



public class DistanceFileWriter {

	BufferedWriter w;
	char[][] seqs;
	String[] names;
	DistanceCalculator dc;
	
	public DistanceFileWriter ( char[][] seqs, String[] names, AlphabetType alphType, CorrectionType corrType )  throws Exception {
		
		dc = new DistanceCalculator(seqs, alphType, corrType);
		
		//Nothing much to be done here - I've already re-set System.out to point to the correct file
		w = new BufferedWriter(new OutputStreamWriter(System.out));
		

		this.seqs = seqs;
		this.names = names;
		
		if (seqs.length != names.length)
			throw new GenericNinjaException("Incorrect count on input sequences");
		
	}
	
	public void write () throws Exception {
		/* Sample output - BB30015: same for FastTree and this code 
		   5
		   1baw_A 0.000000 0.705596 1.471400 0.864778 0.554073
		   1b3i_A 0.705596 0.000000 1.315287 0.932931 0.740447
		   1cuo_A 1.471400 1.315287 0.000000 1.588895 1.371498
		   1ag6_ 0.864778 0.932931 1.588895 0.000000 0.903443
		   1j5c_A 0.554073 0.740447 1.371498 0.903443 0.000000
		*/
		
		int K = seqs.length;
		w.write("\t" + K + "\n");
		
		//try to allocate an array to hold the lower triangle of 
		//distances. if it fails, I'll just compute the full matrix
		double[][] temp = new double[10000][]; // just something to leave a little space after allocating the distances arrays
		double[][] distances;
		try {
			distances = new double[K][];
			for (int i=0; i<K-1; i++)
				distances[i] = new double[K-i-1]; // upper diagonal
		} catch (OutOfMemoryError e) {
			distances = temp; //hoping the JVM won't optimize away the preceding allocation
			distances = null;
		}
		temp = null;
						
		for (int i=0; i<K; i++) {
			w.write(names[i] + " ");
 			for (int j=0; j<K; j++) {
				if (i==j) {
					w.write("0.000000 ");
				} else {
					double val;
					if (distances == null) { // wasn't enough room to calculate and store them all
						val = dc.calc(i, j);
					} else {
						if (i<j){
							val = distances[i][j-i-1] = dc.calc(i, j);
						} else {
							val = distances[j][i-j-1];
						}
					}
					
					
					
					// my own version of printf("%.6f") ... wildly faster than using Formatter
					int int_part = (int)(val+.0000005);
					w.write(int_part + ".");
					
					val -= int_part;
					if (val<0) { // it was essentially exactly that int value
						w.write("000000 ");
					} else {
						
						int shift = 10;
						while ( (val+.0000005) * shift < 1 && shift < 1000000) {
							w.write("0");
							shift *= 10;
						}
						w.write( (int)(0.5 + val * 1000000) + " ");
					}
					
				}
			}
			w.newLine();
		}
		
		w.flush();
		
	}
	
}
