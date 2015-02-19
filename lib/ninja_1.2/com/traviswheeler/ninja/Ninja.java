package com.traviswheeler.ninja; 


/*
 * TO DO
 * 
 * 1) Remove identical sequences (stack them into a pectinate subtree?)
 * 
 * 2) reduce # files: 1 for each arrayheap.  Requires different position handling.  Need to specially handle lvl4 merges
 * 
 * 3) ratchet up number of clusters as # sequences grows  (max of 50?)
 * 
 * 4) add the list of candidate lists, each with associated R' values.  Used to avoid pulling them off list 
 * 
 * 
 */

import com.traviswheeler.libs.*;
import com.traviswheeler.ninja.ArgumentHandler.InputType;
import com.traviswheeler.ninja.ArgumentHandler.OutputType;
import com.traviswheeler.ninja.DistanceCalculator.CorrectionType;
import com.traviswheeler.ninja.SequenceFileReader.AlphabetType;

import java.io.File;

public class Ninja {

	/**
	 * @param args
	 */
	public static void main(String[] argv) {
		
		LogWriter.setLogger(new DefaultLogger());
		ArgumentHandler argHandler = new ArgumentHandler(argv);		
		
		String method = argHandler.getMethod();
		String inFile = argHandler.getInFile();
		File njTmpDir = argHandler.getNJTmpDir();
		InputType inType = argHandler.getInputType();
		OutputType outType = argHandler.getOutputType();
		AlphabetType alphType = argHandler.getAlphabetType();
		CorrectionType corrType = argHandler.getCorrectionType();
			
		
		try {
			
			if ( outType == OutputType.distance) {

				if (inType == InputType.distance) {
					System.err.println("Invalid option. Cannot choose both in_type and out_type of 'd'");
					System.exit(1);
				}
				
				
				SequenceFileReader reader = new SequenceFileReader(inFile, alphType);
				char[][] seqs = reader.getSeqs();
				String[] names = reader.getNames();
				alphType = reader.getAlphType();
				
				DistanceFileWriter wr = new DistanceFileWriter(seqs, names, alphType, corrType);
				wr.write();
				
				
			} else {
			
				TreeBuilderManager manager = new TreeBuilderManager(method, njTmpDir, inFile, inType, alphType, corrType);
							
				String treeString = manager.doJob();
				 
				if (treeString != null) {
					LogWriter.stdOutLogln(treeString);
				} else {
					LogWriter.stdErrLogln("\n\nTree string not generated for some unknown reason. Aborting.");
				}
			}
		} catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
			
}
