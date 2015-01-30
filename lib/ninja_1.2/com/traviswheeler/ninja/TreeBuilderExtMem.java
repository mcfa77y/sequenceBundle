package com.traviswheeler.ninja;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import com.traviswheeler.libs.ArrayHeapExtMem;
import com.traviswheeler.libs.Arrays;
import com.traviswheeler.libs.BinaryHeap;
import com.traviswheeler.libs.BinaryHeap_TwoInts;
import com.traviswheeler.libs.LogWriter;



public class TreeBuilderExtMem {
 
	int K;
	String[] names;
	TreeNode[] nodes;
	int[] redirect;
	
	public static boolean returnCandsToHeaps = false;
	public static boolean useCandHeaps = true;
	public static boolean useBinPairHeaps = true;
	public static int complexCandidateCount = 2000;
	public static int complexCandidateRatio = 40;
	public static float candHeapDecay = (float)0.6;
	public static int clustCnt = 30;
	public static int candHeapThresh = 50000;
	int[] nextActiveNode;
	int[] prevActiveNode;
	int firstActiveNode;

	ArrayHeapExtMem[][] arrayHeaps;

	int[] clustAssignments;
	float[] clustMaxes;
	float[] clustMins;
	int[] clustersBySize;
	int[] clustSizes;
	
	int newK;
	
	RandomAccessFile diskD=null;
	RandomAccessFile candFile=null;
	long candFilePos = 0;
	
	int numCandTriplesToDisk = 16384; // 16 * 3 pages (or so)
	//int numCandTriplesToDisk = 1024; 
	
	float[] R;
	float[][] memD;
	int firstColInMem;
	int curColInMem;
	//int pageFloatCnt;
	int memDSize;

	float[] fBuff;
	byte[] bBuff;
	public int[] candidateCountPerLoop;
	public int[] candidateViewsPerLoop;
	public int[] candidateRowsCountPerLoop;
	
	File njTmpDir;

	int nextInternalNode;
	
	float[] candidatesD;
	int[] candidatesI;
	int[] candidatesJ;
	boolean[] candidatesActive;
	Stack<Integer> freeCandidates;
//	int[] freeCandidates;
//	int freeCandidatesPos = -1;
	int lastCandidateIndex;
	
	CandidateHeap starterCandHeap;
	
	ArrayList<CandidateHeap> candHeapList;
	boolean usingSimpleCandidates = true;
	
	final int floatSize = 4;
	int rowLength;
	
//	int checkpoint = -1;
	
	long maxMemory;

	public TreeBuilderExtMem (String[] names, float R[], File njTmpDir, RandomAccessFile diskD, 
			float[][] memD, int firstMemCol, int rowLength, long maxMemory) throws Exception{
		
		if (!useBinPairHeaps && !useCandHeaps) {
			throw new Exception ("external memory method must use one of the heaps");
		}
		
		clustCnt = TreeBuilder.clustCnt;
		this.rowLength = rowLength;
		this.njTmpDir = njTmpDir;
		this.names = names;
		this.diskD = diskD; 
		this.memD = memD;
		this.maxMemory = maxMemory;
		//pageFloatCnt = pageBlockSize / floatSize;
		//pageFloatCnt = memD[0].length;
		memDSize = memD[0].length;
		fBuff = new float[memDSize];
		bBuff = new byte[memDSize * 4];
		
		
		this.R = R;		
		
		nextInternalNode = K = names.length;
		
//		this.memD = new float[K][pageFloats];
		curColInMem = firstColInMem = firstMemCol; //K;
		
		//if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = (int)(K * TreeBuilder.rebuildStepRatio);

		
		candidateCountPerLoop = new int[K-1];
		candidateViewsPerLoop = new int[K-1];
		candidateRowsCountPerLoop = new int[K-1];
		redirect = new int[2*K-1];
		nodes = new TreeNode[2*K-1];	
		
		int i;

		for (i=0; i<K; i++) {  
			redirect[i] = i;
			nodes[i] = new TreeNode(names[i]);
		}
		
		for (i=K; i<2*K-1; i++) {
			redirect[i] = -1;
			nodes[i] = new TreeNode();
		}

		firstActiveNode = 0;
		nextActiveNode = new int[2*K-1];
		prevActiveNode = new int[2*K-1];
		for ( i=0; i<2*K-1; i++) {
			nextActiveNode[i] = i+1;
			prevActiveNode[i] = i-1;
		}
		
		newK = K;
		
		clusterAndHeap(K);

	}
	
	
	private void clusterAndHeap (int maxIndex ) throws Exception {
		
		if (candidatesD == null) {
			candidatesD = new float[10000];
			candidatesI = new int[10000];
			candidatesJ = new int[10000];
			//freeCandidates = new int[10000];
			freeCandidates = new Stack<Integer>();
			candidatesActive = new boolean[10000];
		} else { //else - must already be created.  Just keep it the same size
			if (useCandHeaps) freeCandidates.clear();
		}
		lastCandidateIndex = -1;

		
		if (useCandHeaps) {
			if (candHeapList == null) {
				candHeapList = new ArrayList<CandidateHeap>();
			} else { 
				if (TreeBuilder.verbose >= 2 && candHeapList.size() > 0)
					LogWriter.stdErrLogln("Cleared candidate heap list when K = " + (2 * K - nextInternalNode));
			
				for (CandidateHeap h : candHeapList) 
					h.closeAndDeleteFile();
				candHeapList.clear();
			}
		} 

		int i,j, ri, rj;
		
		if (!useBinPairHeaps) { // just using candidate heaps
			
			//starterCandHeap = new CandidateHeap(njTmpDir, null, newK, this, 29 /* 1/2 GB */);
			starterCandHeap = new CandidateHeap(njTmpDir, null, newK, this, maxMemory/4);
				
		} else { // useBinPairHeapsHeaps
			
			// pick clusters
			clustAssignments = new int[K];
			
			float maxT = 0;
			float minT = Float.MAX_VALUE;
			
	
			i = firstActiveNode;
			while (i<maxIndex) {
				ri = redirect[i];
				if (R[ri] > maxT) maxT = R[ri];
				if (R[ri] < minT) minT = R[ri];
				i=nextActiveNode[i];
			}
			clustMins = new float[clustCnt];
			clustMaxes = new float[clustCnt];
			float seedTRange = maxT - minT;
			for (i=0; i<clustCnt-1; i++) {
				clustMaxes[i] = minT + (i+1)*seedTRange/clustCnt;
			}
			clustMaxes[clustCnt-1] = maxT;
			clustSizes = new int[clustCnt];
			i=firstActiveNode;
			while (i<maxIndex) {
				ri = redirect[i];
				for (j=0; j<clustCnt; j++) {
					if (R[ri]<=clustMaxes[j]) {
						clustAssignments[ri] = j;
						clustSizes[j]++; 
						break;
					}
				}
				i=nextActiveNode[i];
			}
							
	
			//sort using a heap		
			BinaryHeap heap = new BinaryHeap();
			for (i=0; i<clustCnt; i++) 
				heap.insert(i, clustSizes[i]);
			clustersBySize = new int[clustCnt];
			int minPos;
			for (i=0; i<clustCnt; i++)  {
	        	minPos = heap.heapArray[1];
	        	clustersBySize[i] = heap.val1s[minPos];
	        	heap.deleteMin();
			}
	
			
			// tell me about cluster sizes
			if (TreeBuilder.verbose >= 3) {
				LogWriter.stdErrLogln("cluster sizes");
				for (i=0; i<clustCnt; i++) 
					LogWriter.stdErrLogln(i + " : " + clustSizes[i] + " ( " + clustMaxes[i] + " )");
			}
					
			
			
			if (arrayHeaps == null)
				arrayHeaps = new ArrayHeapExtMem[clustCnt][clustCnt];
			
			
			for (i=0; i<clustCnt; i++) {
				for (j=i; j<clustCnt; j++) {
					if (arrayHeaps[i][j]!= null) {
						arrayHeaps[i][j].prepare();
					} else {
//						arrayHeaps[i][j] = new ArrayHeapExtMem(njTmpDir, redirect, 3MB);
						arrayHeaps[i][j] = new ArrayHeapExtMem(njTmpDir, redirect, maxMemory/666 /*that's about 3MB if mem is 2GB*/);
						arrayHeaps[i][j].A = i;
						arrayHeaps[i][j].B = j;
					}
				}
			}
		}
			
		int cra=-1, crb=-1 ;
		float d, q;
    	long diskPos;
    	int buffStart;
    	
		i=firstActiveNode;
		int numLeft;
		
		//long cntr = 0;
		
		while (i<maxIndex) {
			ri = redirect[i];
			buffStart = -memDSize; // get a new buffer

			j = nextActiveNode[i];
			while (j<maxIndex) {
				rj = redirect[j];
				
				if (useBinPairHeaps) {
					if (clustAssignments[ri] < clustAssignments[rj]) {
						cra = clustAssignments[ri]; 
						crb = clustAssignments[rj];
					} else {
						cra = clustAssignments[rj]; 
						crb = clustAssignments[ri];
					} 
				}
				

				
				//d = D[ra][rb-ra-1];  was this
				if (j>=firstColInMem) {
					d = memD[ri][j-firstColInMem];
				} else {
					if (j >= buffStart + memDSize) {
						//read in next page;
						while ( (buffStart += memDSize) + memDSize <= j); // probably just move to the next page

				    	diskPos = floatSize * ((long)rowLength * ri + buffStart )  ;
				    	diskD.seek(diskPos);
						
						numLeft = maxIndex - buffStart+1;
						if (numLeft >= fBuff.length) {
							diskD.read(bBuff);
							Arrays.byteToFloat(bBuff, fBuff);
						} else {
							diskD.read(bBuff,0, 4*numLeft);
							Arrays.byteToFloat(bBuff, fBuff, numLeft);								
						}

						
					}
					d = fBuff[j - buffStart];					

				}

				
				if (useBinPairHeaps) {
					arrayHeaps[cra][crb].insert(i, j, d);
				} else {
					q = (newK-2) * d - R[ri] - R[rj];
					starterCandHeap.insert(i, j, q);
				}

				
				j = nextActiveNode[j];
			}
			i = nextActiveNode[i];
		}
		
		if (!useBinPairHeaps) {
			starterCandHeap.buildNodeList();
		}
	}
	
	
	public TreeNode[] build ()  throws Exception{
		
		
		nextInternalNode = K;
		
		int cand_cnt = 0;
		int defunct_cnt = 0;
		
		int i, j, x, ri, rj, rx, cluster;
		float Dxi, Dxj, Dij, tmp;
		int a,b;
		int prev;
		int next;
		int min_i, min_j;

		
		int clA, clB; 
		
		float minQ, q, qLimit, minD;
		
		int[] maxT1 = new int[clustCnt]; 
		int[] maxT2 = new int[clustCnt];
		float[] maxT1val = new float[clustCnt];
		float[] maxT2val = new float[clustCnt];
		
		int stepsUntilRebuild = TreeBuilder.rebuildSteps;
				
		if ( stepsUntilRebuild == -1 ) stepsUntilRebuild = (int)(K * TreeBuilder.rebuildStepRatio);
		if ( stepsUntilRebuild < 500 ) 
			stepsUntilRebuild = K; //don't bother rebuilding for tiny trees
		
		CandidateHeap cHeap;
		
		
    	float[] fBuff_i = new float[memDSize];
    	float[] fBuff_j = new float[memDSize];
    	byte[] bBuff = new byte[4*memDSize];

    	
    	try {
			while (nextInternalNode<2*K-1) {// until there are 3 left ... at which point the merging is obvious
				
				usingSimpleCandidates = true;
										
				//get two biggest T values for each cluster maxT1[] and maxT2[]
				if (useBinPairHeaps) {
					for (i=0; i<clustCnt; i++) {
						maxT1[i] = maxT2[i] = -1;
						maxT1val[i] = maxT2val[i] = Float.MIN_VALUE;
					}
					x=firstActiveNode;
					while (x < nextInternalNode) {
						rx = redirect[x];
						cluster = clustAssignments[rx];
						if (R[rx] > maxT2val[cluster]){
							if (R[rx] > maxT1val[cluster]){
								maxT2val[cluster] = maxT1val[cluster];
								maxT1val[cluster] = R[rx];
								maxT2[cluster] = maxT1[cluster];
								maxT1[cluster] = rx;
							} else {
								maxT2val[cluster] = R[rx];
								maxT2[cluster] = rx;						
							}
						}
						x = nextActiveNode[x];
					}
				}
				
				minQ = Float.MAX_VALUE;
				minD = Float.MIN_VALUE;
				
				//Go through current list of candidates, and find best so far.
				min_i = min_j = -1;

				float maxTSum;
				int inactiveCnt = 0;
				if (!returnCandsToHeaps) {
					HashSet<Integer> hash = new HashSet<Integer>();
					for (x=lastCandidateIndex; x>=0; x--) {
						if (!candidatesActive[x]) {
							inactiveCnt++;
							continue;
						}
									
						ri = redirect[candidatesI[x]];
						rj = redirect[candidatesJ[x]];
		
						hash.add(ri);
						hash.add(rj);
						
						if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
							candidatesActive[x] = false; // dead node ... can safely remove, 'cause we're going backwards through the list
							defunct_cnt++;
							if (x == lastCandidateIndex) {
								//scan left to find it
								int y = x;
								while (y>0 && !candidatesActive[y]) {y--;}
								lastCandidateIndex = y;
							}
		
						} else {
							q = candidatesD[x] * (newK-2) - R[ri] - R[rj]; 

							//System.err.println("A: " + candidatesI[x] + ", " + candidatesJ[x] + ": " + q);

							if (q <= minQ) {
								min_i = candidatesI[x];
								min_j = candidatesJ[x];
								minQ = q;
								minD = candidatesD[x];
							} 
						}
					}
					
					candidateViewsPerLoop[K-newK] = candidateCountPerLoop[K-newK] = lastCandidateIndex-inactiveCnt+1;
					candidateRowsCountPerLoop[K-newK] = hash.size();
					 
					if (useCandHeaps) {
						for (CandidateHeap h :candHeapList) {
							candidateCountPerLoop[K-newK] += h.size(); 
						}
					}
				
					if (useBinPairHeaps) {
						/*	frequently (every 50 or 100 iters?), scan through the candidates, and return
						 * entries to the array heap that shouldn't be candidates any more
						 */		
						int clI, clJ;
						if ( (K-newK)%TreeBuilder.candidateIters == 0  && stepsUntilRebuild > TreeBuilder.candidateIters/2  ) {
							for (x=lastCandidateIndex; x>=0; x--) {
								if (!candidatesActive[x]) continue;
							
								ri = redirect[candidatesI[x]];
								rj = redirect[candidatesJ[x]];
								clI = clustAssignments[ri];
								clJ = clustAssignments[rj];
				
								maxTSum = maxT1val[clI] +  ( clI == clJ ? maxT2val[clI] :  maxT1val[clJ]) ;
								qLimit = candidatesD[x] * (newK-2) - maxTSum;
										
								if (qLimit > minQ ) { 
									// it won't be picked as a candidate in next iter, so stick it back in the cluster heaps
									removeCandidate (x);
									
									
									if (clI<=clJ) {
										clA = clI;
										clB = clJ;
									} else {
										clA = clJ;
										clB = clI;
									}
		
									arrayHeaps[clA][clB].insert(candidatesI[x], candidatesJ[x], candidatesD[x]);
		
								}
							}
						}
					}
					
					
					//compact the condidate list
					if (lastCandidateIndex> 0 && inactiveCnt > (float)lastCandidateIndex/5) {
						int left = 0;
						int right = lastCandidateIndex;
						while (left < right) {
							while (left < right && candidatesActive[left]) left++;
							while (right > left && !candidatesActive[right]) right--;
							if (left < right) {
								candidatesD[left] = candidatesD[right];
								candidatesI[left] = candidatesI[right];
								candidatesJ[left] = candidatesJ[right];
								candidatesActive[right] = false;
								candidatesActive[left] = true;
	
								left++;
								right--;
							}
						}
						lastCandidateIndex = right;
						inactiveCnt = 0;
						//freeCandidatesPos = -1;
						freeCandidates.clear();
					}
								
								
					if (useCandHeaps) {
						float qPrime, d;
						BinaryHeap_TwoInts H;
						boolean expiredHeapExists = false;
						// now go through the candidate heaps
						for (int c=candHeapList.size()-1; c>=0; c--) { // backwards through the list since more recent ones are likely to have bounded values closer to real values, so I want to use them to reduce the minQ first
							cHeap = candHeapList.get(c);
							
							cHeap.calcDeltaValues(newK);
							
							//pluck entries off the heap as long as the delta-based condition allows -> stick them in the cand. list
							while (!cHeap.isEmpty()) {
								H = cHeap.getBinaryHeapWithMin();
								qPrime = H.keys[H.heapArray[1]];
					    		
								candidateViewsPerLoop[K-newK]++;
								
								if ( cHeap.k_over_kprime * qPrime + cHeap.minDeltaSum  < minQ) { // "key" hold the q_prime value (q at time of insertion to candidate list)
									i = H.val1s[H.heapArray[1]];
									j = H.val2s[H.heapArray[1]];
									ri = redirect[i];
									rj = redirect[j];
									cHeap.removeMin(); // remove the one we're looking at
									if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {	
										//don't bother to keep it for return to the minheap
										defunct_cnt++;
									} else {
										//convert q' back to d;
										d = (qPrime + cHeap.rPrimes[ri] + cHeap.rPrimes[rj]) / (cHeap.kPrime - 2);
										q = d * (newK-2) - R[ri] - R[rj];								
		//								q = cHeap.k_over_kprime * (qPrime + cHeap.rPrimes[ri] + cHeap.rPrimes[rj]) - R[ri] - R[rj];
										appendCandidate( d, i, j);
										
										//System.err.println("B: " + i + ", " + j + ": " + q);

										
										if (q <= minQ) {
											min_i = i;
											min_j = j;
											
											minQ = q;
											minD = d;
										}
									}
								} else {
									break;
								}
						
							}
							
							//if the remaining size of this heap is too small, mark this heap as needing to be cleared.
							//if ( cHeap.size() / cHeap.representedRowCount < complexCandidateRatio || cHeap.size() < cHeap.origSize * .5) {
							//if ( cHeap.size() / cHeap.representedRowCount < (complexCandidateRatio/2) || cHeap.size() < cHeap.origSize * .5) {
							if (  cHeap.size() < cHeap.origSize * candHeapDecay) { // by now, lots of these are likely defunct anyway.  Reset.
								cHeap.expired = true;
								expiredHeapExists = true;
							}
							
						}
					
						
						//empty the heaps needing to be cleared - the entries will be merged into a new heap if appropriate limits are reached
						if (expiredHeapExists) {
							for (int c=candHeapList.size()-1; c>=0; c--) {
								cHeap = candHeapList.get(c);
								if (cHeap.expired) {
									while (!cHeap.isEmpty()) {								
										H = cHeap.getBinaryHeapWithMin();
										i = H.val1s[H.heapArray[1]];
										j = H.val2s[H.heapArray[1]];
										ri = redirect[i];
										rj = redirect[j];
										if ( ri != -1 && rj != -1) {
											qPrime = H.keys[H.heapArray[1]];
											d =  (qPrime + cHeap.rPrimes[ri] + cHeap.rPrimes[rj]) / (cHeap.kPrime - 2);
											appendCandidate(d, i, j);
										}
										cHeap.removeMin();
									}
									candHeapList.remove(c);
									if (TreeBuilder.verbose >= 2)
										LogWriter.stdErrLogln("Removed a candidate heap (with K = " + cHeap.kPrime + ") when newK =" + newK );
		
								}
							}
								
						}						
					}
				}
				
				if (returnCandsToHeaps) {
					//log the number of candidates grabbed at each iteration.  
					//This tells us how the inner loop scales with the # seqs.
					candidateViewsPerLoop[K-newK] = candidateCountPerLoop[K-newK] = 0;
				}
				
				
				if (!useBinPairHeaps && !starterCandHeap.isEmpty()) {
					//pull off entries from the primary candidate heap that have some chance of having minQ
					float qPrime, d;
					BinaryHeap_TwoInts H;

					
					starterCandHeap.calcDeltaValues(newK);

					//pluck entries off the heap as long as the delta-based condition allows -> stick them in the cand. list
					while (!starterCandHeap.isEmpty()) {
						H = starterCandHeap.getBinaryHeapWithMin();
						qPrime = H.keys[H.heapArray[1]];
			    		
						candidateViewsPerLoop[K-newK]++;
						
						if ( starterCandHeap.k_over_kprime * qPrime + starterCandHeap.minDeltaSum  <= minQ) { 
							i = H.val1s[H.heapArray[1]];
							j = H.val2s[H.heapArray[1]];
							ri = redirect[i];
							rj = redirect[j];
							starterCandHeap.removeMin(); // remove the one we're looking at
							if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {	
								//don't bother to keep it for return to the minheap
								defunct_cnt++;
							} else {
								//convert q' back to d;
								d = (qPrime + starterCandHeap.rPrimes[ri] + starterCandHeap.rPrimes[rj]) / (starterCandHeap.kPrime - 2);
								q = d * (newK-2) - R[ri] - R[rj];								
//								q = cHeap.k_over_kprime * (qPrime + cHeap.rPrimes[ri] + cHeap.rPrimes[rj]) - R[ri] - R[rj];
								appendCandidate( d, i, j);
								
//								System.err.println("C: " + i + ", " + j + ": " + q);

								
								if (q <= minQ) {
									min_i = i;
									min_j = j;
									minQ = q;
									minD = d;
								}
							}
						} else {
							break;
						}
				
					}
					
				} else if (useBinPairHeaps) {
					//pull off entries for the bin-pair heaps that have some chance of having minQ
					int h_i, h_j, minPos;
					float h_d;
					BinaryHeap_TwoInts bh;
					ArrayHeapExtMem h;
					for (a=0; a<clustCnt; a++) {
						for (b=a; b<clustCnt; b++) {
														
							clA = clustersBySize[a]<clustersBySize[b] ? clustersBySize[a] : clustersBySize[b];	
							clB = clustersBySize[a]<clustersBySize[b] ? clustersBySize[b] : clustersBySize[a];
							
							maxTSum = maxT1val[clA] +  ( clA == clB ? maxT2val[clA] :  maxT1val[clB]) ;
							
							h = arrayHeaps[clA][clB];
							
							while (!h.isEmpty()) {
								bh = h.getBinaryHeapWithMin();
								
								if (bh == null) {
					        		LogWriter.stdErrLogln("Surprising: null binary heap, for heap " + clA + ", " + clB);    		
					        		h.describeHeap();
					        		throw new Exception("null binary heap");    			
								}
								
								minPos = bh.heapArray[1];
								h_i = bh.val1s[minPos];
								h_j = bh.val2s[minPos];
								h_d = bh.keys[minPos];
								
								ri = redirect[h_i];
								rj = redirect[h_j];
									
								if (rj==-1 || ri==-1 /*that's an old pointer*/) {							
									h.removeMin();//pull it off
									
									defunct_cnt++;
									continue;
								}						
								q = h_d * (newK-2); 
								qLimit = q - maxTSum;
								
//								System.err.println("D: " + h_i + ", " + h_j + ": " + qLimit + " (" + q + ", " + maxTSum + ")");

								if (qLimit <= minQ) {
									// it's possible that this or one of the following nodes on the 
									// heap has Q-value less than the best I've seen so far	
						    		
									arrayHeaps[clA][clB].removeMin();//pull it off
									appendCandidate (h_d, h_i, h_j); 
									cand_cnt++;
									q -=  R[ri] + R[rj];
									
									if (q <= minQ) { // this is now the best I've seen								
										min_i = h_i;
										min_j = h_j;
										minQ = q;
										minD = h_d;
									}
								} else {
									break; // no use pulling more off the heap ... they can't beat the best so far.
								}
							}
	
							if (returnCandsToHeaps) {
	
								//log the number of candidates grabbed at each iteration.  
								//This tells us how the inner loop scales with the # seqs.
								candidateViewsPerLoop[K-newK] += lastCandidateIndex+1;
								candidateCountPerLoop[K-newK] += lastCandidateIndex+1;
								returnCandidates();
							}
							
						}
					}	
				}
				
				if (useCandHeaps && !usingSimpleCandidates && !returnCandsToHeaps ) {
					candHeapList.get(candHeapList.size()-1).buildNodeList();	
				}
				
				
				//Now I know the position on the candidates array that has the best Q node.
				//Remove it from the candidates, merge the nodes, update D/T values
				// and possibly reset the candidates (every few iterations? when exceed some size?)
				
				
				ri = redirect[min_i];
				rj = redirect[min_j];
								
				nodes[nextInternalNode].leftChild = nodes[min_i];
				nodes[nextInternalNode].rightChild = nodes[min_j];
				
				//assign branch lengths
				if (minD == Float.MIN_VALUE) {
					throw new Exception("minD was not assigned correctly");
				}
				if (newK==2) {
					nodes[min_i].length = nodes[min_j].length = (float)minD / 2;
				} else {
					nodes[min_i].length = (minD + (R[ri]-R[rj])/(newK-2)) / 2 ;
					nodes[min_j].length = (minD + (R[rj]-R[ri])/(newK-2)) / 2 ;
				}				
				//if a length is negative, move root of that subtree around to compensate.
				if (nodes[min_i].length < 0) {
					nodes[min_j].length += nodes[min_i].length;
					nodes[min_i].length = 0;
				} else if (nodes[min_j].length < 0) {
					nodes[min_i].length += nodes[min_j].length;
					nodes[min_j].length = 0;
				}
				
				
				// remove i,j from active list
				redirect[min_i] = redirect[min_j] = -1;
				
				prev = prevActiveNode[min_i];
				next = nextActiveNode[min_i];
				prevActiveNode[next] = prev;
				if (prev == -1) 
					firstActiveNode = next;
				else 
					nextActiveNode[prev] = next;
				
				prev = prevActiveNode[min_j];
				next = nextActiveNode[min_j];
				prevActiveNode[next] = prev;
				if (prev == -1) 
					firstActiveNode = next;
				else 
					nextActiveNode[prev] = next;
	

				//calculate new D and T values
		    	//    	for all these D reads, should check if the i/j/x value is greater than the most recently written-to-disk index
		    	//   	 ... if not, then read from the memD, not diskD
				R[ri] = 0;
		    	long diskPos;
		    	
	
		    	// I need to get this before going into the "foreach x" loop, 'cause I need Dij for all new vals.
				if (min_i>=firstColInMem) {
					Dij = memD[rj][min_i-firstColInMem];
				} else if (min_j>=firstColInMem) {
					Dij = memD[ri][min_j-firstColInMem];
				} else {	    	 
			    	diskPos = floatSize * ((long)rowLength * ri + min_j )  ;

//			    	diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile_i))  + h_j );
			    	diskD.seek(diskPos);
			    	Dij = diskD.readFloat();
				}
				

		    	int buffStart_i = -memDSize;
		    	int buffStart_j = -memDSize;
		    	
		    	x=firstActiveNode;
		    	
		    	int numLeft;
				while (x<nextInternalNode) {
					
					rx = redirect[x];
	
					if (min_i>=firstColInMem) {
						Dxi = memD[rx][min_i-firstColInMem];
					} else if (x>=firstColInMem) {
						Dxi = memD[ri][x-firstColInMem];
					} else {					
						if (x >= buffStart_i + memDSize) {	
							//read in next page;
							while ( (buffStart_i += memDSize) + memDSize <= x); // probably just move to the next page
	
					    	diskPos = floatSize * ((long)rowLength * ri + buffStart_i )  ;
							diskD.seek(diskPos);
							
							numLeft = nextInternalNode - buffStart_i + 1;
							if (numLeft >= fBuff_i.length) {
								diskD.read(bBuff);
								Arrays.byteToFloat(bBuff, fBuff_i);
							} else {
								diskD.read(bBuff,0, 4*numLeft);
								Arrays.byteToFloat(bBuff, fBuff_i, numLeft);								
							}
						}
						Dxi = fBuff_i[x - buffStart_i];					
					}
	
					
					if (min_j>=firstColInMem) {
						Dxj = memD[rx][min_j-firstColInMem];
					} else if (x>=firstColInMem) {
						Dxj = memD[rj][x-firstColInMem];
					} else {
						if (x >= buffStart_j + memDSize) {	
							//read in next page;
							while ( (buffStart_j += memDSize) + memDSize <= x); // probably just move to the next page
							
							diskPos = floatSize * ((long)rowLength * rj + buffStart_j ) ;
							diskD.seek(diskPos);
							
							numLeft = nextInternalNode - buffStart_j + 1;
							if (numLeft >= fBuff_j.length) {
								diskD.read(bBuff);
								Arrays.byteToFloat(bBuff, fBuff_j);
							} else {
								diskD.read(bBuff,0, 4*numLeft);
								Arrays.byteToFloat(bBuff, fBuff_j, numLeft);								
							}
							
						}
						Dxj = fBuff_j[x - buffStart_j];					
						
					}
					
					//tmp =  Math.round(1000000 * (Dxi + Dxj - Dij) / 2)/(float)1000000;	 // this is the level of precision of input distances. No use allowing greater (noisy) precision to dominate decisions
					tmp =   (Dxi + Dxj - Dij) / 2;	 // this is the level of precision of input distances. No use allowing greater (noisy) precision to dominate decisions
					
					R[ri] += tmp;								
					R[rx] += tmp - (Dxi + Dxj);
					
	//				instead of writing to the file, write to an in-memory D buffer.  if that buffer is full, then write it to disk
					//D[ra][rb-ra-1] = tmp;
//System.err.println(ri + ", " + rx + ": " + tmp + " (R[" + ri +"] = " + R[ri] + "), R[" + rx + "] = " + R[rx]);
					memD[rx][nextInternalNode - firstColInMem] = tmp;
					if (x>=firstColInMem) 
						memD[ri][x - firstColInMem] = tmp;
					
					
					x = nextActiveNode[x];
				}
				

				if (TreeBuilder.verbose >= 3) {
					LogWriter.stdErrLogln("(extmem) next node: " + nextInternalNode + ": " + min_i + "(" + ri +") , " + min_j + "(" + rj + ") Q = " + minQ + " (" + cand_cnt + " cands; " + defunct_cnt +  " defunct);  R[" + ri + "] = " + R[ri] );
					LogWriter.stdErrLogln("     lengths: " + nodes[min_i].length + ", " + nodes[min_j].length );  
				}

				redirect[nextInternalNode] = ri;
	
								
				curColInMem++;			
				if (curColInMem == firstColInMem + memDSize) {
					
			    	//write memD to diskD  ... there's just one buffer per row.
	
			    	// first, we append each row in memD to existing rows in the file (one disk block per row)
			    	x=firstActiveNode;
					while (x<nextInternalNode) {
						rx = redirect[x];
				    
						Arrays.floatToByte(memD[rx], bBuff);
						diskPos = floatSize * ((long)rowLength * rx + firstColInMem )  ;
//						diskPos = 4 * (numDCols * (rx-(numDRowsPerFile*diskFile)) + firstColInMem )   ;
				    	diskD.seek(diskPos);			    	
				    	diskD.write(bBuff);
									    	
				    	x = nextActiveNode[x];
					}
	
					//then write each column as a full new row in the file (which will cover multiple blocks)
					float[] fBuff_horiz = new float[nextInternalNode];
					byte[] bBuff_horiz = new byte[4 * nextInternalNode];
					int ry;
			    	for (i=firstColInMem; i<curColInMem; i++ ) { // for each new column
			    		//write distances from the memD into a buffer, 
			    		//which is then written to the row for this new node (ri)
			    		
			    		ry = redirect[i];
			    		if (ry==-1) continue; // this node was already merged with another, even before being dumped to file
	
			    		x=firstActiveNode;
			    		while (x<nextInternalNode) {
			    			//many entries in the buffer are left in default.
			    			// minor speedup could be had in the conversion routine (but I won't bother)
			    			rx = redirect[x];
			    		
			    			fBuff_horiz[x] = memD[rx][i-firstColInMem];	    	
			    			x = nextActiveNode[x];
			    		}
						Arrays.floatToByte(fBuff_horiz, bBuff_horiz);
						diskPos = floatSize * ((long)rowLength * ry  )  ;
						//diskPos = 4 * (numDCols * (ry-(numDRowsPerFile*diskFile))  )   ;
						diskD.seek(diskPos);
				    	diskD.write(bBuff_horiz);
			    		
			    	}
					
				}
				
				newK--;
				
				if ( stepsUntilRebuild == 0 ) {
					if (TreeBuilder.verbose >= 3) {
						LogWriter.stdErrLogln ("Resetting the clusters and corresponding PQs after " + (K-newK-1) + " iterations");
					}

					redirect[nextInternalNode++] = ri;
					clusterAndHeap(nextInternalNode);

					if (newK < 200) {
						stepsUntilRebuild = newK; // almost done, quit shrinking the rebuild size 
					} else {
						if ( TreeBuilder.rebuildSteps == -1 ) {
							if (TreeBuilder.rebuildStepsConstant) 
								stepsUntilRebuild = (int)(K * TreeBuilder.rebuildStepRatio);
							else
								stepsUntilRebuild = (int)(newK * TreeBuilder.rebuildStepRatio);
						} else {
							stepsUntilRebuild = TreeBuilder.rebuildSteps;
						}
					}

					
				} else {
					stepsUntilRebuild--;
					
					if (useBinPairHeaps) {
						// re-set the max levels for clusters (based on one-iteration-old data)
						for (j=0; j<clustCnt; j++) {
							//LogWriter.stdErrLogln("updating cluster " + j + " from " + clustPercentiles[j] + " to "  + maxT1val[j]);	
							clustMaxes[j] = maxT1val[j];	
							clustMins[j] = maxT1val[j];
						}
						//then pick new cluster for ri, based on it's T value;
						for (j=0; j<clustCnt; j++) {
							if (R[ri]<=clustMaxes[j]) {
								clustAssignments[ri] = j;
								break;
							}
						}
					}
					
					// ... then add all new distances to the appropriate place (bin-pair heap or candidates)
					x = firstActiveNode;
					float d;
					while (x<nextInternalNode) {
						
						rx = redirect[x];
						d = memD[rx][nextInternalNode - firstColInMem];
						
						if (useBinPairHeaps) {
							if (clustAssignments[ri]<=clustAssignments[rx]) {
								clA = clustAssignments[ri];
								clB = clustAssignments[rx];
							} else {
								clA = clustAssignments[rx];
								clB = clustAssignments[ri];
							}
	
							arrayHeaps[clA][clB].insert(nextInternalNode, x, d);
						} else {
							appendCandidate(d, nextInternalNode, x);
						}
						
						x = nextActiveNode[x];


					}

					
					nextInternalNode++;
					
				}

				if (curColInMem == firstColInMem + memDSize) 
			//	if (curColInMem == firstColInMem + memD[0].length) 
					firstColInMem = curColInMem;

			}
		} catch (Exception e){
			LogWriter.stdErrLogln("\n\nException caught while building tree ");  
//			LogWriter.stdErrLogln("building node: " + nextInternalNode + ", checkpoint: " + checkpoint );
			e.printStackTrace();
			
			throw e;
		} catch (OutOfMemoryError e){
			LogWriter.stdErrLogln("\n\nOut of memory error !!! ");   
//			LogWriter.stdErrLogln("building node: " + nextInternalNode + ", checkpoint: " + checkpoint );

			LogWriter.stdErrLogln("candidate array sizes : " + candidatesActive.length + ", " +
					"last cand index = " + lastCandidateIndex + ", freeCands stack size " +  freeCandidates.capacity() );


			throw e;
		}		
		
		if (TreeBuilder.verbose >= 1) {				
			LogWriter.stdErrLogln(cand_cnt + " candidates added");
			LogWriter.stdErrLogln(defunct_cnt + " defunct nodes removed");
		}
		
		if (TreeBuilder.verbose >= 2) {				
			long max_cnt = 0;
			long max_cnt_iter = 0;
			long views_at_max_count = 0;
			long max_views = 0;
			long max_views_iter = 0;
			long count_at_max_views = 0;
			float max_cnt_ratio = 0;
			float max_view_ratio = 0;
			long sum_possible = 0;
			long sum_cnt = 0;
			long sum_views = 0;
			float cntPerRowMax = 0;
			long row_sum_cnt = 0;
			long max_rowCnt = 0;
			for (i=1; i<candidateCountPerLoop.length; i++) {
				
				
				long rowCnt = candidateRowsCountPerLoop[i];
				long views = candidateViewsPerLoop[i];

				long cnt = candidateCountPerLoop[i];
				if (cnt > max_cnt) {
					max_cnt = cnt;
					max_rowCnt = rowCnt;
					max_cnt_iter = i;
					views_at_max_count = views;
					cntPerRowMax = (float)cnt / rowCnt;
				}
				sum_cnt += cnt;
				row_sum_cnt += rowCnt;

				
				if (views > max_views) {
					max_views = views;
					max_views_iter = i;
					count_at_max_views = cnt;
				}
				sum_views += views;

				
				
				long all_pairs_cnt = ( (K-i) * (K-i-1) / 2 ) ;
				sum_possible += all_pairs_cnt;
				
				float cnt_ratio = (float)cnt / all_pairs_cnt;
				if (cnt_ratio>max_cnt_ratio) max_cnt_ratio = cnt_ratio;
				
				float view_ratio = (float)cnt / all_pairs_cnt;
				if (view_ratio>max_view_ratio) max_view_ratio = view_ratio;				
				
			}
			
			LogWriter.stdErrLogln("max # candidates: " + max_cnt + " with " + max_rowCnt + " rows represtented, at iteration " + max_cnt_iter + " of " + candidateCountPerLoop.length + ", with " + views_at_max_count + " viewed");
			LogWriter.stdErrLogln("max # candidates: " + max_views + ", at iteration " + max_views_iter + ", with " + count_at_max_views + " total");
			LogWriter.stdErrLogln( "max ratio of candidates to possible pairs: " + String.format("%.8f", (float)max_cnt_ratio/100) + "%");
			LogWriter.stdErrLogln( "max ratio of viewed candidates to possible pairs: " + String.format("%.8f", (float)max_view_ratio/100) + "%");
			
			LogWriter.stdErrLogln("average # candidates: " + (sum_cnt/(K-4)) );
			LogWriter.stdErrLogln("average # views: " + (sum_views/(K-4)) );
			LogWriter.stdErrLogln("total # candidates: " + sum_cnt + "  ( of " + sum_possible + " possible), with " + sum_views + " viewed");

			LogWriter.stdErrLogln("avg ratio of candidates to possible pairs: " + String.format("%.8f", ((float)sum_cnt/sum_possible)/100 ) + "%");
			LogWriter.stdErrLogln("avg ratio of viewed candidates to possible pairs: " + String.format("%.8f", ((float)sum_views/sum_possible)/100 ) + "%");

			
			LogWriter.stdErrLogln("avg number of candidates per row represented in candidates list: " + String.format("%.1f", ((float)sum_cnt/row_sum_cnt) ) );
			LogWriter.stdErrLogln("number of candidates per row represented in candidates list, for max cand size: " + String.format("%.1f", cntPerRowMax ) );

		}
		
		
		return nodes;
		
	}
	
	private void returnCandidates () throws Exception {
		int cra, crb, x, ri, rj, i, j;
		float d;

		for (x=lastCandidateIndex; x>=0; x--) {
			if (!candidatesActive[x]) {
				continue;
			}
						
			ri = redirect[candidatesI[x]];
			rj = redirect[candidatesJ[x]];

			if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
				candidatesActive[x] = false; // dead node ... can safely remove, 'cause we're going backwards through the list
				if (x == lastCandidateIndex) {
					//scan left to find it
					while (x>0 && !candidatesActive[x]) {x--;}
					lastCandidateIndex = x;
				}
			} else {
			
				if (clustAssignments[ri] < clustAssignments[rj]) {
					cra = clustAssignments[ri]; 
					crb = clustAssignments[rj];
				} else {
					cra = clustAssignments[rj]; 
					crb = clustAssignments[ri];
				}	
				arrayHeaps[cra][crb].insert(candidatesI[x], candidatesJ[x], candidatesD[x]);

			}
		}
		
		lastCandidateIndex = -1;
		freeCandidates.clear();
		
		if (candFilePos>0) {
			int[] iBuff = new int[numCandTriplesToDisk*3];
			byte[] bbBuff = new byte[numCandTriplesToDisk*3*4];

			while (candFilePos>0) {
				//use bBuff, which is already in memory  
				candFile.seek( (candFilePos-numCandTriplesToDisk) * 3 * 4);
				candFile.read(bbBuff,0, numCandTriplesToDisk*3*4);
				Arrays.byteToInt(bbBuff, iBuff);
			
				for (x=0; x<numCandTriplesToDisk; x++) {
					i = iBuff[x*3];
					j = iBuff[x*3+1];
					d = Float.intBitsToFloat(iBuff[x*3+2]);

					ri = redirect[i];
					rj = redirect[j];
	
					if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
						//
					} else {									
						if (clustAssignments[ri] < clustAssignments[rj]) {
							cra = clustAssignments[ri]; 
							crb = clustAssignments[rj];
						} else {
							cra = clustAssignments[rj]; 
							crb = clustAssignments[ri];
						}	
						arrayHeaps[cra][crb].insert(i,j,d);
					}

				}
				candFilePos-=numCandTriplesToDisk;
			}						
		}
		

	}


	
	void appendCandidate (float d, int i, int j) throws Exception{

		if (useCandHeaps ) {
	
			if (usingSimpleCandidates) {
				
				int candCnt = lastCandidateIndex - freeCandidates.size() + 1 ;

				if ( candCnt >= 2000000 || (candCnt >= candHeapThresh && candCnt/newK > complexCandidateRatio ) ) { 
					usingSimpleCandidates = false;
					CandidateHeap heap = new CandidateHeap(njTmpDir, null, newK, this, maxMemory/1000 /* roughly 2MB for 2GB RAM */);
					float q=0;
					
					try {
						for (int x=0; x<=lastCandidateIndex; x++ ) {
							if (candidatesActive[x] && redirect[candidatesI[x]] != -1 && redirect[candidatesJ[x]] != -1) {
								q = candidatesD[x] * (newK-2) - R[redirect[candidatesI[x]]] - R[redirect[candidatesJ[x]]];  
								heap.insert(candidatesI[x], candidatesJ[x], q);
							}
						}
						q = d * (newK-2) - R[redirect[i]] - R[redirect[j]];  
						heap.insert(i, j, q);
					} catch (Exception e) {
						LogWriter.stdErrLogln("Death while appending candidate.  nextInternalNode = " + nextInternalNode);
						throw e;
					}
					
					
					//just cleared candidates ... clean up data structure
					lastCandidateIndex = -1;
					freeCandidates.clear();
	
					CandidateHeap expiringHeap;
					BinaryHeap_TwoInts H;
					int ri, rj;
					float qPrime;
					if (candHeapList.size() == 100 ) { // don't let the number of heaps exceed 100 ... merge the oldest 20 into this one
						for (int c=0; c<20;c++) {
							expiringHeap = candHeapList.get(0);
							while (!expiringHeap.isEmpty()) {								
								H = expiringHeap.getBinaryHeapWithMin();
								i = H.val1s[H.heapArray[1]];
								j = H.val2s[H.heapArray[1]];
								ri = redirect[i];
								rj = redirect[j];
								if ( ri != -1 && rj != -1) {
									qPrime = H.keys[H.heapArray[1]];
									d =  (qPrime + expiringHeap.rPrimes[ri] + expiringHeap.rPrimes[rj]) / (expiringHeap.kPrime - 2);
									q = d * (newK-2) - R[ri] - R[rj];
									heap.insert(i, j, q);
								}
								expiringHeap.removeMin();
							}
							candHeapList.remove(0);
							if (TreeBuilder.verbose >= 2)
								LogWriter.stdErrLogln("Removed a candidate heap (with K = " + expiringHeap.kPrime + ") when newK =" + newK );
	
						}
					}
						
					
					candHeapList.add(heap);
					if (TreeBuilder.verbose >= 2)
						LogWriter.stdErrLogln("Added a candidate heap when newK =" + newK + " : total heap count is " + candHeapList.size());
	
					
				} else {
					
					appendCandToSimpleList (d, i, j);
					
				}
					
			} else { // not using simple candidate list.
				
				d = d * (newK-2) - R[redirect[i]] - R[redirect[j]]; // really q' now  
	
				candHeapList.get(candHeapList.size()-1).insert(i, j, d);
			}
				
		}	else { // not using candHeaps
			appendCandToSimpleList (d, i, j); 
		}
	}

		
	private int appendCandToSimpleList (float d, int i, int j)  throws Exception {
		
		int freePos;
		if (freeCandidates.isEmpty()) {
			freePos = lastCandidateIndex + 1;
		} else {
			freePos = freeCandidates.pop();
		}
		
		
		
		//make sure we don't overflow
		if (freePos == candidatesD.length) {
			int newLength = (int)(candidatesD.length * (candidatesD.length < 1000000 ? 10 : 2 ));
		
			int x;
			
			if (newLength>8000000) {
			//if (newLength> 1040) {
			//	System.err.println("Need to change length test and numTriples");
			
				//need to write some of the current entries to a file, and add entries to freeCandidates 
				try {
					int[] iBuff = new int[numCandTriplesToDisk*3];
					byte[] bbBuff = new byte[numCandTriplesToDisk*3*4];
					for (x=0; x<numCandTriplesToDisk; x++) {
						iBuff[x*3] = candidatesI[lastCandidateIndex];
						iBuff[x*3+1] = candidatesJ[lastCandidateIndex];
						iBuff[x*3+2] = Float.floatToIntBits(candidatesD[lastCandidateIndex]);
						freeCandidates.add(lastCandidateIndex);
						candidatesActive[lastCandidateIndex] = false;
						lastCandidateIndex--;
					}
					freePos = lastCandidateIndex + 1;
					
					Arrays.intToByte(iBuff, bbBuff);
					if (candFile == null) {
						File tmpFile = File.createTempFile("ninja", "candDisk", njTmpDir);
						candFile = new RandomAccessFile(tmpFile,"rw");
					}
					candFile.seek(candFilePos * 3 * 4);
					candFile.write(bbBuff);
					candFilePos += numCandTriplesToDisk;
					
				} catch (Exception e) {
					throw e;
				}
			} else {
				
				float[] D = new float[newLength];
				for (x=0; x<candidatesD.length; x++ ) 
					D[x] = candidatesD[x];
				candidatesD = D;
				
				int[] I = new int[newLength];
				for (x=0; x<candidatesI.length; x++ ) 
					I[x] = candidatesI[x];
				candidatesI = I;
	
				int[] J = new int[newLength];
				for (x=0; x<candidatesJ.length; x++ ) 
					J[x] = candidatesJ[x];
				candidatesJ = J;
				
				boolean[] act = new boolean[newLength];
				for (x=0; x<candidatesActive.length; x++ ) 
					act[x] = candidatesActive[x];			
				candidatesActive = act;
			}
		}
		candidatesD[freePos] = d;
		candidatesI[freePos] = i;
		candidatesJ[freePos] = j;
		candidatesActive[freePos] = true;
		if (freePos > lastCandidateIndex) {
			lastCandidateIndex = freePos;
		}
		return freePos;
	}
	
	private void removeCandidate (int x) {
		
		
		//freeCandidates[++freeCandidatesPos] = x;
		freeCandidates.push(x);

		candidatesActive[x] = false;
		if (x == lastCandidateIndex) {
			//scan left to find it
			while (x>0 && !candidatesActive[x]) {x--;}
			lastCandidateIndex = x;
		}
	}
	
}
