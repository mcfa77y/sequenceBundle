package com.traviswheeler.ninja;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.Random;

import com.traviswheeler.libs.Arrays;
import com.traviswheeler.libs.DefaultLogger;
import com.traviswheeler.libs.LogWriter;
import com.traviswheeler.libs.Logger;
import com.traviswheeler.ninja.ArgumentHandler.InputType;
import com.traviswheeler.ninja.ArgumentHandler.OutputType;
import com.traviswheeler.ninja.DistanceCalculator.CorrectionType;
import com.traviswheeler.ninja.SequenceFileReader.AlphabetType;

public class TreeBuilderManager {

	String method;
	File njTmpDir;
	String inFile;
	String[] names = null;
	float[][] inD = null;
	InputType inType;
	AlphabetType alphType;
	CorrectionType corrType;

	//standard constructor, used by the main class. Logger has already been set
	public TreeBuilderManager(String method, File njTmpDir, String inFile, InputType inType, AlphabetType alphType, CorrectionType corrType) {
		this.method = method;
		this.njTmpDir = njTmpDir;
		this.inFile = inFile;
		this.inType = inType;
		this.alphType = alphType;
		this.corrType = corrType;
	}
	
	//special constructor for Mesquite, accepts the logger to set it
	public TreeBuilderManager(String method, File njTmpDir, float[][] inD, String[] inNames, Logger logger) {
		this.method = method;
		this.njTmpDir = njTmpDir;
		this.inD = inD;
		this.names = inNames;
		LogWriter.setLogger(logger);
	}

	
	public String doJob() throws FileNotFoundException {

		int[][] distances = null;
		float[][] memD = null;
		float[] R  = null;
		// all for external memory version
		int floatSize = 4;
		int pageBlockSize = 1024; //that many ints = 4096 bytes;		
		RandomAccessFile diskD=null; 

		int rowLength = 0;
		int firstMemCol = -1;

		
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		boolean ok = true;
		TreeNode[] nodes = null;
		String treeString = null;
		
		NinjaLogWriter.printVersion();
				

		
		try {
			int K = 0;
			
			if (method.startsWith("extmem")) {

				if (maxMemory < 1900000000) {
					LogWriter.stdErrLogln("\nWarning: using an external-memory variant of NINJA with less than 2GB allocated RAM.");
					LogWriter.stdErrLogln("The data structures of NINJA may not work well if given less than 2GB. Please use the -Xmx flag,");
					LogWriter.stdErrLogln("e.g. 'java -Xmx2G -jar Ninja.jar', or 'java -d64 -Xmx8G -jar Ninja.jar' on a 64 bit system.");
				}
				
				if (njTmpDir == null) njTmpDir = new File( System.getProperty("user.dir") );
				
				File tmpDirParent = njTmpDir; 
				
				//create the temporary directory where work will be done
				while (njTmpDir.exists()) {
					njTmpDir = new File(tmpDirParent.getAbsolutePath(), "ninja_temp_" + getUniqueID());
				}
				njTmpDir.mkdir();
				
				LogWriter.stdErrLogln("created temporary directory for this run of NINJA : " + njTmpDir.getAbsolutePath());

				DistanceReaderExtMem reader = null;
				if (inD == null) {
					if (inType == InputType.alignment) {					
						SequenceFileReader seqReader = new SequenceFileReader(inFile, alphType);
						char[][] seqs = seqReader.getSeqs();
						names = seqReader.getNames();
						alphType = seqReader.getAlphType();
						DistanceCalculator distCalc = new DistanceCalculator(seqs, alphType, corrType);
						K = names.length;
						reader = new DistanceReaderExtMem(distCalc, K);
					}	else {
						reader = new DistanceReaderExtMem(inFile);
						K = reader.K;
						names = new String[K];
					}
				} else {
					reader = new DistanceReaderExtMem(inD);
					K = names.length;					
				}

				R = new float[K];
				rowLength = (K + K-2); //that's a K*K table for the initial values, plus another K*(K-2) columns for new nodes
		
				long maxSize; // max amount of D stored in memory
				if (TreeBuilderExtMem.useBinPairHeaps) {
					//maxSize = 200 * (int)Math.pow(2, 20); //200MB
					maxSize = maxMemory / 10;
				} else {
//					maxSize = 750 * (int)Math.pow(2, 20); //750MB
					maxSize = maxMemory / 3;
				}
				
				int numCols = (int)(maxSize / (4 * K));
				// needs to be a multiple of pageBlockSize
				int numBlocks = numCols/pageBlockSize; // chops off fractional part
				if (numBlocks == 0) numBlocks  = 1;  //for huge inputs, this could result in memD larger than 400MB
				numCols = numBlocks * pageBlockSize;
					

				if (numCols >= 2*K-2) {
					numCols = 2*K-2;
				} else {
					File tmpFile = File.createTempFile("ninja", "diskD", njTmpDir);
					diskD = new RandomAccessFile(tmpFile,"rw");
					
					long fileSize = (long)K * rowLength * floatSize;
					diskD.setLength(fileSize) ;  // that's a K*K table for the initial values, plus another K*(K-3) columns for new nodes, at 4 bytes per float
					LogWriter.stdErrLogln("created file of size " + fileSize + " for the distance matrix");
				}

				memD = new float[K][numCols];
				firstMemCol = reader.read( names, R, diskD, memD, rowLength, pageBlockSize);
				
			/*
for (int i=0; i<K; i++) {
	System.out.print(names[i] + ": ");
	for (int j=0; j<K; j++) {
		float d = -2;
		if (j>=firstMemCol) {
			d = memD[i][j-firstMemCol];
		} else {
			System.out.println("not good!!!");
		}
		System.out.print( 10* (int)(d*10000000) + " ");
	}
	System.out.print("\n");
}			
System.exit(1);*/

			} else {
				
//					
				DistanceReader reader = null;
				if (inD == null) { 
					if (inType == InputType.alignment) {
						SequenceFileReader seqReader = new SequenceFileReader(inFile, alphType);
						char[][] seqs = seqReader.getSeqs();
						names = seqReader.getNames();
						alphType = seqReader.getAlphType();
						DistanceCalculator distCalc = new DistanceCalculator(seqs, alphType, corrType);
						K = names.length;
						reader = new DistanceReader(distCalc, K);
					} else {
						reader = new DistanceReader(inFile);
						K = reader.K;
						names = new String[K];
					}
				} else {
					K = names.length;			
				}

				distances = new int[K][];
				for (int i=0; i<K; i++) {
					distances[i] = new int[K - i - 1];
				}
				
				
				if (inD == null) {
					reader.read( names, distances);
				} else {
					for (int i=0; i<inD.length-1; i++) 
						for (int j=0; j<inD.length-1-i; j++)
							distances[i][j] = (int)(inD[i][j+i+1] * 10000000); 
				}
/*
for (int i=0; i<K; i++) {
	System.out.print(names[i] + ": ");
	for (int j=0; j<distances[i].length; j++)
		System.out.print( distances[i][j] + " ");
	System.out.print("\n");
}
System.exit(0);
			*/
			}

			
			if (TreeBuilder.verbose>0)
				if (inType == InputType.alignment)
					LogWriter.stdErrLogln("Distances computed");
				else if (inD == null) 
					LogWriter.stdErrLogln("Distance file read");
				else
					LogWriter.stdErrLogln("Distances imported by NINJA");
			
			

			if (method.startsWith("inmem") || method.equals("default")) { 
				TreeBuilder tb = new TreeBuilderBinHeap(names, distances);
				nodes = tb.build();
			} else if (method.startsWith("extmem") ) {
				TreeBuilderExtMem tb_extmem = new TreeBuilderExtMem(names,  R, njTmpDir, diskD, memD, firstMemCol, rowLength, maxMemory);
				nodes = tb_extmem.build();
			}
			
		} catch (FileNotFoundException e){
			//pass error along quietly
			throw e;
		} catch (Exception e){
			LogWriter.stdErrLogln("error building tree");
			LogWriter.stdErrLogln(e.getMessage());
			e.printStackTrace();
			ok = false;
		} catch (OutOfMemoryError e){
			ok = false;
			boolean report_oom = true;
			if ( method.equals("default") ) {
				// we just tried "inmem", and that didn't work, so go for "extmem"
				LogWriter.stdErrLogln("\nThe in-memory (faster) algorithm  exceeded available memory. ");
				LogWriter.stdErrLogln("Trying again with the external-memory (slower) algorithm, which should not run out of memory. ");

				
				distances = null;
				memD = null;
				report_oom = false;
				try {
					method = "extmem_default";
					treeString = doJob();
//					ok = true;
				} catch (OutOfMemoryError ee){
					report_oom = true;					
				}
			} else if ( method.equals("extmem_default") ) {
				// throw the error back to the 1st level of this recursion
				throw e;
			}
			if (report_oom) {
				LogWriter.stdErrLogln("\n\nOut of memory error  ");
				LogWriter.stdErrLogln(e.getMessage());
			}
		}
		
				
		if (  method.startsWith("extmem") && njTmpDir.exists()) {
			LogWriter.stdErrLogln("delete " + njTmpDir.getAbsolutePath());
			deleteDir(njTmpDir);
		}

		if (ok && treeString == null) {
			if (nodes != null) {
				StringBuffer sb = new StringBuffer(); 
				nodes[nodes.length-1].buildTreeString(sb);
				treeString = sb.toString() + ";\n";
			}
		}
		

		return (treeString);
		
	}
	
	final static int NUM_RAND_DIR_CHARS = 6;
	final static String chars = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	static Random r = new Random();

	static String getUniqueID() {
		char[] buf = new char[NUM_RAND_DIR_CHARS];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = chars.charAt(r.nextInt(chars.length()));
		}
		return new String(buf);
	}

	static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String name: children) {
                boolean success = deleteDir(new File(dir, name));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

	
}
