package com.traviswheeler.ninja;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import com.traviswheeler.libs.ArrayHeapExtMem;
import com.traviswheeler.libs.LogWriter;
import com.traviswheeler.ninja.DistanceCalculator.CorrectionType;
import com.traviswheeler.ninja.SequenceFileReader.AlphabetType;

public class ArgumentHandler {

	
	public static enum InputType {alignment, distance};
	public static enum OutputType {distance, tree};

	
	String method = "default"; // default should be "bin" for small, "cand" for large.
	File njTmpDir = null;
	String inFile = null;
	InputType inType = InputType.alignment;
	OutputType outType = OutputType.tree;
	AlphabetType alphType = null;
	CorrectionType corrType = CorrectionType.not_assigned;

	public ArgumentHandler (String argString) {
		this(argString.split("\\s"));
	}
	
	public ArgumentHandler (String[] argv) {
		int x = 0;
		LongOpt[] longopts = new LongOpt[21];
		
		longopts[x++] = new LongOpt("in", LongOpt.REQUIRED_ARGUMENT, null, 'i');
		longopts[x++] = new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 'o');
		longopts[x++] = new LongOpt("method", LongOpt.REQUIRED_ARGUMENT, null, 'm');
		longopts[x++] = new LongOpt("clust_size", LongOpt.REQUIRED_ARGUMENT, null, 's');
		longopts[x++] = new LongOpt("rebuild_step_ratio", LongOpt.REQUIRED_ARGUMENT, null, 'r');
		longopts[x++] = new LongOpt("verbose", LongOpt.REQUIRED_ARGUMENT, null, 'v');
		longopts[x++] = new LongOpt("quiet", LongOpt.NO_ARGUMENT, null, 'q');
		longopts[x++] = new LongOpt("disk_pages", LongOpt.REQUIRED_ARGUMENT, null, 'p');
		longopts[x++] = new LongOpt("tmp_dir", LongOpt.REQUIRED_ARGUMENT, null, 't');
		//longopts[8] = new LongOpt("cand_iters", LongOpt.REQUIRED_ARGUMENT, null, 'c');
		longopts[x++] = new LongOpt("complex_cand_ratio", LongOpt.NO_ARGUMENT, null, 'c');
		longopts[x++] = new LongOpt("chop", LongOpt.REQUIRED_ARGUMENT, null, 'c');
		longopts[x++] = new LongOpt("cand_heap_decay", LongOpt.REQUIRED_ARGUMENT, null, 'y');
		longopts[x++] = new LongOpt("variable_rebuild_steps", LongOpt.NO_ARGUMENT, null, 'd');
		longopts[x++] = new LongOpt("cand_heap_threshold", LongOpt.REQUIRED_ARGUMENT, null, 'c');
		longopts[x++] = new LongOpt("dist_in_mem", LongOpt.NO_ARGUMENT, null, 'd');
		longopts[x++] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
		longopts[x++] = new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v');
		longopts[x++] = new LongOpt("in_type", LongOpt.REQUIRED_ARGUMENT, null, 'x');
		longopts[x++] = new LongOpt("out_type", LongOpt.REQUIRED_ARGUMENT, null, 'x');
		longopts[x++] = new LongOpt("alph_type", LongOpt.REQUIRED_ARGUMENT, null, 'a');
		longopts[x++] = new LongOpt("corr_type", LongOpt.REQUIRED_ARGUMENT, null, 'a');
		
		int c;
		String arg;		
		
		
		String optName;
		Getopt g = new Getopt("ninja", argv, "a:c:di:h:m:o:p:r:s:qt:v:x:y:", longopts);

		while ((c = g.getopt()) != -1) {
            arg = g.getOptarg();
			
            if (g.getLongind() == -1)  optName = "";	
			else  optName = longopts[g.getLongind()].getName(); 
		                 
            switch (c)  {
				case 'a':
				if (optName.equals("alph_type")) {
					if (arg.toString().equals("a"))
						alphType = AlphabetType.amino;
					else if (arg.toString().equals("d"))
						alphType = AlphabetType.dna;
					else {
            			LogWriter.stdErrLogln("invalid alph_type:  try  'a' or 'd'.");
            			System.exit(1);
					}
				} else if (optName.equals("corr_type")) {
					if (arg.toString().equals("n"))
						corrType = CorrectionType.none;
					else if (arg.toString().equals("j"))
						corrType = CorrectionType.JukesCantor;
					else if (arg.toString().equals("k"))
						corrType = CorrectionType.Kimura2;
					else if (arg.toString().equals("s"))
						corrType = CorrectionType.FastTree;
					else {
            			LogWriter.stdErrLogln("invalid ut_type:  try  'n', 'j', 'k', or 's'.");
            			System.exit(1);
					}
				} 
	            break;
				case 'c':
		            if (optName.equals("complex_cand_ratio")) {
						TreeBuilderExtMem.complexCandidateRatio = Integer.parseInt(arg.toString());
		            } else if (optName.equals("cand_heap_threshold")) {    
		            	TreeBuilderExtMem.candHeapThresh = Integer.parseInt(arg.toString());
		            } else if (optName.equals("chop")) {    
						if (arg.toString().equals("top"))
							ArrayHeapExtMem.chopBottom = false;
						else if (arg.toString().equals("bottom"))
							ArrayHeapExtMem.chopBottom = true;
		            }
					//TreeBuilder.candidateIters = Integer.parseInt(arg.toString());
					break;
				case 'd':
		            if (optName.equals("variable_rebuild_steps")) {
						TreeBuilder.rebuildStepsConstant = true;
		            } else if (optName.equals("dist_in_mem")) {
		            	TreeBuilder.distInMem = true;
		            }					
					break;
				case 'h':
					NinjaLogWriter.printUsage();
					System.exit(0);
					break;
				case 'i':
					inFile  = arg.toString();
	        		break;
				case 'm':
		            method = arg.toString();
		            if (method.equals("default")) {
        				// this method will first try "inmem", then if out of memory, "extmem"
        				TreeBuilderExtMem.useCandHeaps = true; // will only apply if 2nd phase is attempted
        			} else if (method.startsWith("extmem")) {
        				TreeBuilderExtMem.useCandHeaps = true;
            		} else if (method.startsWith("inmem") ) {
            			//ok
            		} else {
            			LogWriter.stdErrLogln("invalid method: available methods are  'default', 'inmem', and 'extmem'.");
            			System.exit(1);
            		}
		            /*
        			if (method.startsWith("extret")) {
        				TreeBuilderExtMem.returnCandsToHeaps = true;
        			} else if (method.startsWith("ext")) {
        				TreeBuilderExtMem.useCandHeaps = false;        				
        			} else if (method.equals("default")) {
        				// this method will first try "bin", then if out of memory, "cand"
        				TreeBuilderExtMem.useCandHeaps = true; // will only apply if 2nd phase is attempted
        			} else if (method.startsWith("cand")) {
        				TreeBuilderExtMem.useCandHeaps = true;
        				if (method.startsWith("cand_only")) {
            				TreeBuilderExtMem.useBinPairHeaps = false;
                			LogWriter.stdErrLogln("invalid method:  not yet implemented ...");
                			System.exit(1);
        				}
            		} else if (method.startsWith("bin") || method.equals("standard")) {
            			//ok
            		} else {
            			LogWriter.stdErrLogln("invalid method:  try  'default', 'binheap', 'candheap'. Testing methods include 'extmemheap' and 'standard'.");
            			System.exit(1);
            		}
            		*/
        			break;
				case 'o': //out        			
	            	try {
	            		PrintStream out = new PrintStream(new FileOutputStream(arg.toString()));
		                System.setOut(out);
	            	} catch (Exception E) {
	            		LogWriter.stdErrLogln("Error creating file for output: " + arg.toString());
                		System.exit(1);            		
	            	}
		            break;
				case 'q':
					TreeBuilder.verbose = 0;
		            break;
				case 'p':
					DistanceReader.numPages = Integer.parseInt(arg.toString());
					break;
				case 'r':
					TreeBuilder.rebuildStepRatio  = Float.parseFloat(arg.toString());
	        		break;
				case 's':
					TreeBuilder.clustCnt = Integer.parseInt(arg.toString());
					break;
				case 't':
					if (optName.equals("tmp_dir")) {
						try {
							njTmpDir = new File( arg.toString() );
						}catch (Exception e) {
							LogWriter.stdErrLogln("invalid temporary directory : " + arg.toString());
							System.exit(1);
						}
					}
					break;
				case 'v':
					if (optName.equals("verbose")) {
						TreeBuilder.verbose = Integer.parseInt(arg.toString());
					} else if (optName.equals("version")) {
						NinjaLogWriter.printVersion();
						System.exit(0);
					}
		            break;
				case 'x':
					if (optName.equals("in_type")) {
						if (arg.toString().equals("a"))
							inType = InputType.alignment;
						else if (arg.toString().equals("d"))
							inType = InputType.distance;
						else {
	            			LogWriter.stdErrLogln("invalid in_type:  try  'a' or 'd'.");
	            			System.exit(1);
						}
					} else if (optName.equals("out_type")) {
						if (arg.toString().equals("d"))
							outType = OutputType.distance;
						else if (arg.toString().equals("t"))
							outType = OutputType.tree;
						else {
	            			LogWriter.stdErrLogln("invalid ut_type:  try  'd' or 't'.");
	            			System.exit(1);
						}
					} 
		            break;
				case 'y':
					TreeBuilderExtMem.candHeapDecay = Float.parseFloat(arg.toString());
		            break;

			}
		

		}
		if (null == inFile) {
			if (argv.length>0  && g.getOptind()<argv.length && argv[g.getOptind()] != null) {
				inFile = argv[g.getOptind()].toString();
			}
		}	

	}

	public String getMethod() {
		return method;
	}
	
	public String getInFile() {
		return inFile;
	}	


	public File getNJTmpDir() {
		return njTmpDir;
	}

	
	public InputType getInputType () {
		return inType;
	}

	public OutputType getOutputType () {
		return outType;
	}

	public AlphabetType getAlphabetType () {
		return alphType;
	}

	public CorrectionType getCorrectionType () {
		return corrType;
	}
	
}
