package com.traviswheeler.ninja;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Stack;

import com.traviswheeler.libs.ArrayHeapExtMem;
import com.traviswheeler.libs.Arrays;
import com.traviswheeler.libs.BinaryHeap;
import com.traviswheeler.libs.BinaryHeap_TwoInts;
import com.traviswheeler.libs.LogWriter;



public class TreeBuilderExtMem_CandHeaps {

	int pageBlockSize;
	int K;
	String[] names;
	TreeNode[] nodes;
	int[] redirect;
	
//	public static int complexCandidateCount = 2000;
	public static int complexCandidateRatio = 30;
	public static float candHeapDecay = (float)0.6;
	
	public static int clustCnt = 30;
	int[] nextActiveNode;
	int[] prevActiveNode;
	int firstActiveNode;

	ArrayHeapExtMem[][] arrayHeaps;

	int[] clustAssignments;
	float[] clustMaxes;
	float[] clustMins;
	int[] clustersBySize;
	int[] clustSizes;
	
	RandomAccessFile diskD=null;
	
	float[] R;
	float[][] memD;
	int firstColInMem;
	int curColInMem;
	int pageFloats;

	float[] fBuff;
	byte[] bBuff;
	public int[] candidateCountPerLoop;
	public int[] candidateViewsCountPerLoop;
	
	File njTmpDir;

	int nextInternalNode;
	
	float[] candidatesD;
	int[] candidatesI;
	int[] candidatesJ;
	boolean[] candidatesActive;
	Stack<Integer> freeCandidates;
	int lastCandidateIndex;
	
	boolean usingSimpleCandidates = true;
	ArrayList<CandidateHeap> candHeapList;

	final int floatSize = 4;
	int rowLength;	

	int checkpoint = -1;

	public TreeBuilderExtMem_CandHeaps (String[] names, float R[], File njTmpDir, RandomAccessFile diskD, 
			float[][] memD, int firstMemCol, 
			int pageBlockSize, int rowLength) throws Exception{
		
		clustCnt = TreeBuilder.clustCnt;
		this.rowLength = rowLength;
		this.njTmpDir = njTmpDir;
		this.names = names;
		this.diskD = diskD; 
		this.pageBlockSize = pageBlockSize; 
		pageFloats = pageBlockSize / floatSize;
		fBuff = new float[pageFloats];
		bBuff = new byte[pageBlockSize];
		
		this.R = R;		
		
		nextInternalNode = K = names.length;
		
		memD = new float[K][pageFloats];
		//curColInMem = firstColInMem = K;
		curColInMem = firstColInMem = firstMemCol;
		
		//if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = (int)(K * TreeBuilder.rebuildStepRatio);

		
		candidateCountPerLoop = new int[K-3];
		candidateViewsCountPerLoop = new int[K-3];
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
		
		clusterAndHeap(K);

	}
	
	
	private void clusterAndHeap (int maxIndex ) throws Exception {
		
		checkpoint = 1;
		
		// pick clusters
		clustAssignments = new int[K];
		
		float maxT = 0;
		float minT = Float.MAX_VALUE;
		int i,j, ri, rj;

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
		
		
		//	make PQs for each cluster pair (make candidate list, too)  ... this will allow old ones to be collected
		if (TreeBuilder.verbose >= 2 && freeCandidates != null) 
			LogWriter.stdErrLogln("Re-clustering at K = " + (2 * K - nextInternalNode) + ". Currently " + (lastCandidateIndex - freeCandidates.size()) + " candidates");

		if (candidatesD == null) {
			candidatesD = new float[10000];
			candidatesI = new int[10000];
			candidatesJ = new int[10000];
			//freeCandidates = new int[10000];
			freeCandidates = new Stack<Integer>();
			candidatesActive = new boolean[10000];
		} else {
			//else - must already be created.  Just keep it the same size
			freeCandidates.clear();
		}
		lastCandidateIndex = -1;

		if (candHeapList == null) {
			candHeapList = new ArrayList<CandidateHeap>();
		} else { 

			if (TreeBuilder.verbose >= 2 && candHeapList.size() > 0)
				LogWriter.stdErrLogln("Cleared candidate heap list when K = " + (2 * K - nextInternalNode));
			
			for (CandidateHeap h : candHeapList) 
				h.closeAndDeleteFile();
			candHeapList.clear();
				

		}
		
		if (arrayHeaps == null)
			arrayHeaps = new ArrayHeapExtMem[clustCnt][clustCnt];
		
		
		for (i=0; i<clustCnt; i++) {
			for (j=i; j<clustCnt; j++) {
				if (arrayHeaps[i][j]!= null) {
					arrayHeaps[i][j].initialize();
				} else {
					arrayHeaps[i][j] = new ArrayHeapExtMem(njTmpDir, null);
					arrayHeaps[i][j].A = i;
					arrayHeaps[i][j].B = j;
				}
			}
		}
		
		int cra, crb ;
		float d;
    	long diskPos;
    	int buffStart;
    	
		i=firstActiveNode;
		while (i<maxIndex) {
			ri = redirect[i];
			buffStart = -pageFloats; // get a new buffer

			j = nextActiveNode[i];
			while (j<maxIndex) {
				rj = redirect[j];
				if (clustAssignments[ri] < clustAssignments[rj]) {
					cra = clustAssignments[ri]; 
					crb = clustAssignments[rj];
				} else {
					cra = clustAssignments[rj]; 
					crb = clustAssignments[ri];
				}
				
				//d = D[ra][rb-ra-1];  was this
				if (j>=firstColInMem) {
					d = memD[ri][j-firstColInMem];
				} else {
					if (j >= buffStart + pageFloats) {
						//read in next page;
						while ( (buffStart += pageFloats) + pageFloats <= j); // probably just move to the next page

				    	diskPos = floatSize * ((long)rowLength * ri + buffStart )  ;

//						diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile))  + buffStart );
						diskD.seek(diskPos);
						diskD.read(bBuff);
						Arrays.byteToFloat(bBuff, fBuff);
					}
					d = fBuff[j - buffStart];					

				}
				
				arrayHeaps[cra][crb].insert(i, j, d);
				j = nextActiveNode[j];
			}
			i = nextActiveNode[i];
		}

		
		usingSimpleCandidates = true;
		
	}
	
	
	public TreeNode[] build ()  throws Exception{
		
		checkpoint = 2;
		
		nextInternalNode = K;
		
		int cand_cnt = 0;
		int defunct_cnt = 0;
		
		int i, j, x, ri, rj, rx, cluster, newK;
		int min_i, min_j;
		float Dxi, Dxj, Dij, tmp;
		int a,b;
		int prev;
		int next;

		int clA, clB;
		
		float minQ, q, qLimit;
		
		int[] maxT1 = new int[clustCnt]; 
		int[] maxT2 = new int[clustCnt];
		float[] maxT1val = new float[clustCnt];
		float[] maxT2val = new float[clustCnt];
		
		int stepsUntilRebuild = TreeBuilder.rebuildSteps;
				
		if ( stepsUntilRebuild == -1 ) stepsUntilRebuild = (int)(K * TreeBuilder.rebuildStepRatio);
		
		CandidateHeap cHeap;
		
		newK = K;
		
		try {
			while (nextInternalNode<2*K-3) {// until there are 3 left ... at which point the merging is obvious
						
				usingSimpleCandidates = true;
				
				
/*				checkpoint = 3;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
*/

				//get two biggest T values for each cluster maxT1[] and maxT2[]
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
		
				minQ = Float.MAX_VALUE;
				
				//Go through current list of candidates, and find best so far.
				// If we're using the candidate heap, then push these to the next heap
				min_i = min_j = -1;
				
				float maxTSum;
				int inactiveCnt = 0;
				
				int ccc = 0;
				//for (x=lastCandidateIndex; x>=0; x--) {
				for (x=0; x<=lastCandidateIndex; x++) {
					if (!candidatesActive[x]) {
						inactiveCnt++;
						continue;
					}
								
					ri = redirect[candidatesI[x]];
					rj = redirect[candidatesJ[x]];
	
					if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
						
						defunct_cnt++;
						removeCandidate(x);// dead node ... can safely remove, 'cause we're going backwards through the list
						
					} else {
						ccc++;
							
						q = candidatesD[x] * (newK-2) - R[ri] - R[rj]; 
						if (q <= minQ) {
	
							min_i = candidatesI[x];
							min_j = candidatesJ[x];
							minQ = q;
						} 
					}
				}
				
				candidateViewsCountPerLoop[K-newK] = candidateCountPerLoop[K-newK] = lastCandidateIndex-inactiveCnt+1;
				for (CandidateHeap h :candHeapList) {
					candidateCountPerLoop[K-newK] += h.size(); 
				}
				
				//candidateRowsCountPerLoop[K-newK] = hash.size();
				
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
	
/*				checkpoint = 4;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
*/


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
			    		
						candidateViewsCountPerLoop[K-newK]++;
						
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
								appendCandidate( i, j, d, newK);
								if (q <= minQ) {
									min_i = i;
									min_j = j;
									minQ = q;
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
			
				/*
				checkpoint = 5;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
*/

				//if the total size of the heaps needing to be cleared, plus existing candidates, is above threshold,
				//then create a new heap, and move everything onto it (and change boolean to say "not using the simple list")
				//otherwise, move everything onto the candidate list
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
									appendCandidate(i, j, d, newK);
								}
								cHeap.removeMin();
							}
							candHeapList.remove(c);
							if (TreeBuilder.verbose >= 2)
								LogWriter.stdErrLogln("Removed a candidate heap (with K = " + cHeap.kPrime + ") when newK =" + newK );

						}
					}
						
				}				
				/*
				checkpoint = 6;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
*/
	
				//pull off entries for the heaps that have some chance of having minQ
				int minPos, h_i, h_j;
				BinaryHeap_TwoInts bh;
				float h_d;
				ArrayHeapExtMem h;
				for (a=0; a<clustCnt; a++) {
					for (b=a; b<clustCnt; b++) {
						
						checkpoint = 61;
						
						clA = clustersBySize[a]<clustersBySize[b] ? clustersBySize[a] : clustersBySize[b];	
						clB = clustersBySize[a]<clustersBySize[b] ? clustersBySize[b] : clustersBySize[a];
						
						maxTSum = maxT1val[clA] +  ( clA == clB ? maxT2val[clA] :  maxT1val[clB]) ;
						
						h = arrayHeaps[clA][clB];
						
						
						while (!h.isEmpty()) {

//							if (clA == 7 && clB == 9 && nextInternalNode == 59058) {								
//									LogWriter.stdErrLogln("H2 size = " + h.H2.size() + "\n--------------");
//									h.describeHeap();
//							}
							
							//checkpoint = 62;
							
							try {
								bh = h.getBinaryHeapWithMin();
							} catch (Exception e) {
				        		LogWriter.stdErrLogln("Error getting min, when clA = " + clA + ", clB = " + clB );    		
				        		h.describeHeap();
				        		throw new Exception("null binary heap");    			
							}
								
							if (bh == null) {
				        		LogWriter.stdErrLogln("Surprising: null binary heap, when clA = " + clA + ", clB = " + clB);    		
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
							q = h_d * (newK-2); //"fl" is d_ij  ... convert to a double for better precision
							qLimit = q - maxTSum;
							checkpoint = 63;
							if (qLimit <= minQ) {
								// it's possible that this or one of the following nodes on the 
								// heap has Q-value less than the best I've seen so far	
					    		
								checkpoint = 64;
								h.removeMin();//pull it off
								checkpoint = 65;
								
								appendCandidate ( h_i, h_j, h_d, newK); 
								cand_cnt++;
								
								
								checkpoint = 66;
								
								q -=  R[ri] + R[rj];
								
								if (q <= minQ) { // this is now the best I've seen								
									min_i = h_i;
									min_j = h_j;
									minQ = q;
								}
							} else {
								break; // no use pulling more off the heap ... they can't beat the best so far.
							}
						}
					}
				}	
				
				if (!usingSimpleCandidates ) {
					candHeapList.get(candHeapList.size()-1).buildNodeList();	
				}
				checkpoint = 7;
	
				//Now I know the position on the candidates array that has the best Q node.
				//Remove it from the candidates, merge the nodes, update D/T values
				// and possibly reset the candidates (every few iterations? when exceed some size?)
				
				ri = redirect[min_i];
				rj = redirect[min_j];
								
				nodes[nextInternalNode].leftChild = nodes[min_i];
				nodes[nextInternalNode].rightChild = nodes[min_j];
				
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
				
		    	float[] fBuff_i = new float[pageFloats];
		    	float[] fBuff_j = new float[pageFloats];
		    	byte[] bBuff = new byte[4*pageFloats];
		    	int buffStart_i = -pageFloats;
		    	int buffStart_j = -pageFloats;
		    	
		    	/*
		    	checkpoint = 8;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
				*/
				x=firstActiveNode;
				while (x<nextInternalNode) {
					
					rx = redirect[x];
	
					if (min_i>=firstColInMem) {
						Dxi = memD[rx][min_i-firstColInMem];
					} else if (x>=firstColInMem) {
						Dxi = memD[ri][x-firstColInMem];
					} else {					
						if (x >= buffStart_i + pageFloats) {	
							//read in next page;
							while ( (buffStart_i += pageFloats) + pageFloats <= x); // probably just move to the next page
	
					    	diskPos = floatSize * ((long)rowLength * ri + buffStart_i )  ;
//							diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile_i))  + buffStart_i );
							diskD.seek(diskPos);
							diskD.read(bBuff);
							Arrays.byteToFloat(bBuff, fBuff_i);
						}
						Dxi = fBuff_i[x - buffStart_i];					
					}
	
					
					if (min_j>=firstColInMem) {
						Dxj = memD[rx][min_j-firstColInMem];
					} else if (x>=firstColInMem) {
						Dxj = memD[rj][x-firstColInMem];
					} else {
						if (x >= buffStart_j + pageFloats) {	
							//read in next page;
							while ( (buffStart_j += pageFloats) + pageFloats <= x); // probably just move to the next page
							
							diskPos = floatSize * ((long)rowLength * rj + buffStart_j ) ;
//							diskPos = 4 * (numDCols * (rj-(numDRowsPerFile*diskFile_j))  + buffStart_j );
							diskD.seek(diskPos);
							diskD.read(bBuff);
							Arrays.byteToFloat(bBuff, fBuff_j);
						}
						Dxj = fBuff_j[x - buffStart_j];					
						
					}
					
					tmp = (Dxi + Dxj - Dij) / 2;					
					R[ri] += tmp;								
					R[rx] += tmp - (Dxi + Dxj);
					
	//				instead of writing to the file, write to an in-memory D buffer.  if that buffer is full, then write it to disk
					//D[ra][rb-ra-1] = tmp;
	
					memD[rx][nextInternalNode - firstColInMem] = tmp;
					if (x>=firstColInMem) 
						memD[ri][x - firstColInMem] = tmp;
					
					
					x = nextActiveNode[x];
				}
				/*
				checkpoint = 9;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}
*/
				if (TreeBuilder.verbose >= 3)
					LogWriter.stdErrLogln("(cand) next node: " + nextInternalNode + ": " + min_i + "(" + ri +") , " + min_j + "(" + rj + ") Q = " + minQ + " (" + cand_cnt + " cands; " + defunct_cnt +  " defunct);  R[" + ri + "] = " + R[ri] );
	
				redirect[nextInternalNode] = ri;
				
				curColInMem++;			
				int y;
				if (curColInMem == firstColInMem + pageFloats) {
			    	//write memD to diskD  ... there's just one buffer per row.
	
			    	// first, we append each row in memD to existing rows in the file (one disk block per row)
			    	x=firstActiveNode;
					while (x<nextInternalNode) {
						rx = redirect[x];
				    
						for ( y=0; y<pageFloats; y++) 
							fBuff[y] = memD[rx][y];							
						Arrays.floatToByte(fBuff, bBuff);
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
				/*
				checkpoint = 10;
				if (!arrayHeaps[7][9].testHeapSizeSensibility () ) {
					throw new Exception("bad heap size relationship after chkpt " + checkpoint);
				}*/

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
					
					// ... then add all new distances to the appropriate cluster pair
					x = firstActiveNode;
					
					while (x<nextInternalNode) {
						
						rx = redirect[x];
						d = memD[rx][nextInternalNode - firstColInMem];
						
						if (clustAssignments[ri]<=clustAssignments[rx]) {
							clA = clustAssignments[ri];
							clB = clustAssignments[rx];
						} else {
							clA = clustAssignments[rx];
							clB = clustAssignments[ri];
						}

/*						
						//if ( nextInternalNode == 58695) {
							xxx++;
							if (xxx==10034) {
								LogWriter.stdErrLogln("xxx = 10034");
								arrayHeaps[7][9].describeHeap();
							}
							
							if (!arrayHeaps[clA][clB].checkFreeSlots()){
								arrayHeaps[clA][clB].describeHeap();
								throw new Exception("free slots has duplicates. after chkpt " + checkpoint + " with xxx = " + xxx + " and clA = " + clA + " and clB = " + clB);
							}
						
							if ( !arrayHeaps[clA][clB].testHeapSizeSensibility () ) {
								arrayHeaps[clA][clB].describeHeap();
								throw new Exception("bad heap size relationship after chkpt " + checkpoint + " with xxx = " + xxx + " and clA = " + clA + " and clB = " + clB);
							}
						//}
*/
						
						arrayHeaps[clA][clB].insert(nextInternalNode, x, d);
						x = nextActiveNode[x];
						
					}

					nextInternalNode++;
					
				}

				
				
				checkpoint = 11;
				if (curColInMem == firstColInMem + pageFloats) 
					firstColInMem = curColInMem;

			}
		} catch (Exception e){
			LogWriter.stdErrLogln("\n\nException caught while building tree ");  
			LogWriter.stdErrLogln("building node: " + nextInternalNode + ", checkpoint: " + checkpoint );

			e.printStackTrace();
			
			throw e;
		} catch (OutOfMemoryError e){
			LogWriter.stdErrLogln("\n\nOut of memory error !!! ");   
			LogWriter.stdErrLogln("building node: " + nextInternalNode + ", checkpoint: " + checkpoint );

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
			long max_view_cnt_at_max_cnt = 0;
			long max_view_cnt = 0;
			long max_cnt_iter = 0;
			long max_view_cnt_iter = 0;
			float max_ratio = 0;
			float max_view_ratio = 0;
			long sum_possible = 0;
			long sum_cnt = 0;
			long sum_view_cnt = 0;

			for (i=1; i<candidateCountPerLoop.length; i++) {
				
				long viewCnt = candidateViewsCountPerLoop[i];	
				long cnt = candidateCountPerLoop[i];
				
				if (cnt > max_cnt) {
					max_cnt = cnt;
					max_view_cnt_at_max_cnt = viewCnt;
					max_cnt_iter = i;
				}
				sum_cnt += cnt;

				if (viewCnt > max_view_cnt) {
					max_view_cnt = viewCnt;
					max_view_cnt_iter = i;
				}
				sum_view_cnt += viewCnt;
								
				long all_pairs_cnt = ( (K-i) * (K-i-1) / 2 ) ;
				sum_possible += all_pairs_cnt;
				
				float ratio = (float)cnt / all_pairs_cnt;
				if (ratio>max_ratio) max_ratio = ratio;
				
				ratio = (float)viewCnt / all_pairs_cnt;
				if (ratio>max_view_ratio) max_view_ratio = ratio;
				
			}
			
//			LogWriter.stdErrLogln("max # candidates: " + max_cnt + " with " + max_rowCnt + " rows represtented, at iteration " + max_cnt_iter + " of " + candidateCountPerLoop.length);
			LogWriter.stdErrLogln("max # candidates: " + max_cnt + " (of which " + max_view_cnt_at_max_cnt+ " were viewed), at iteration " + max_cnt_iter + " of " + candidateCountPerLoop.length);
			LogWriter.stdErrLogln("max # viewed candidates: " + max_view_cnt+ ", at iteration " + max_view_cnt_iter );

			LogWriter.stdErrLogln( "max ratio of candidates to possible pairs: " + String.format("%.8f", (float)max_ratio/100) + "%");
			LogWriter.stdErrLogln( "max ratio of viewed candidates to possible pairs: " + String.format("%.8f", (float)max_view_ratio/100) + "%");
			
			LogWriter.stdErrLogln("average # candidates: " + (sum_cnt/(K-4)) );
			LogWriter.stdErrLogln("average # viewed candidates: " + (sum_view_cnt/(K-4)) );

			LogWriter.stdErrLogln("total # candidates: " + sum_cnt + "( " +  sum_view_cnt + " viewed)  ( of " + sum_possible + " possible)");

			LogWriter.stdErrLogln("avg ratio of candidates to possible pairs: " + String.format("%.8f", ((float)sum_cnt/sum_possible)/100 ) + "%");
			LogWriter.stdErrLogln("avg ratio of viewed candidates to possible pairs: " + String.format("%.8f", ((float)sum_view_cnt/sum_possible)/100 ) + "%");
			
//			LogWriter.stdErrLogln("avg number of candidates per row represented in candidates list: " + String.format("%.1f", ((float)sum_cnt/row_sum_cnt) ) );
//			LogWriter.stdErrLogln("number of candidates per row represented in candidates list, for max cand size: " + String.format("%.1f", cntPerRowMax ) );

		}
		
		finishMerging();
		
		return nodes;
		
	}

    /**
     * @return  provides a lower bound on the number of bytes this heap is using in memory
     */
/*	public long footprint () {
		long ret = 0;
	
		for (int i=0; i<clustCnt; i++) {
			for (int j=i; j<clustCnt; j++) {
				if (arrayHeaps[i][j]!= null)  {
					//LogWriter.stdErrLogln("Array Heap : " + i + ", " + j + " ... last function : " + arrayHeaps[i][j].curFunction);
					long v = arrayHeaps[i][j].footprint();
					ret += v;
				}
			}
		}
		
		LogWriter.stdErrLogln("memD size : " + memD.length * memD[0].length * 4);
		ret += memD.length * memD[0].length * 4;

		LogWriter.stdErrLogln("candidatesD size : " + candidatesD.length * 3 * 4);
		ret += candidatesD.length * 4 * 3; //3 int/float arrays
		
		
		return ret;
	}
	*/
	protected void finishMerging() {
		// only two trivial merges left:
		// this code will get cleaner
		int last_i=0, last_j=0, last_k=0;
		int i=0;
		while (redirect[i++]==-1);
		last_i=i-1;
		while (redirect[i++]==-1);
		last_j=i-1;
		while (redirect[i++]==-1);
		last_k=i-1;

		int nextNode = 2*K-3;
		
		nodes[nextNode].leftChild = nodes[last_i];
		nodes[nextNode].rightChild = nodes[last_j];

		nodes[nextNode+1].leftChild = nodes[nextNode];
		nodes[nextNode+1].rightChild = nodes[last_k];

	}
	
	void appendCandidate ( int i, int j, float d, int newK) throws Exception {
		
		
		if (usingSimpleCandidates) {
			
			int candCnt = lastCandidateIndex - freeCandidates.size() + 1 ;
			//if (candCnt > complexCandidateCount  || (newK > 10 && candCnt/newK > complexCandidateRatio ) ) {
			if ( candCnt >= 2000000 || candCnt/newK > complexCandidateRatio ) { 
				usingSimpleCandidates = false;
				CandidateHeap heap = new CandidateHeap(njTmpDir, null, newK, this);
				float q=0;
				for (int x=0; x<=lastCandidateIndex; x++ ) {
					if (candidatesActive[x] && redirect[candidatesI[x]] != -1 && redirect[candidatesJ[x]] != -1) {
						q = candidatesD[x] * (newK-2) - R[redirect[candidatesI[x]]] - R[redirect[candidatesJ[x]]];  
						heap.insert(candidatesI[x], candidatesJ[x], q);
					}
				}
				q = d * (newK-2) - R[redirect[i]] - R[redirect[j]];  
				heap.insert(i, j, q);

				//just cleared candidates ... clean up data structure
				lastCandidateIndex = -1;
				freeCandidates.clear();

				CandidateHeap expiringHeap;
				BinaryHeap_TwoInts H;
				int ri, rj;
				float qPrime;
				if (candHeapList.size() == 100 ) { // don't let the number of heaps exceed 100 ... merge the oldest 5 into this one
					for (int c=0; c<5;c++) {
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
				
				int freePos;
				//if (freeCandidatesPos == -1) {
				if (freeCandidates.isEmpty()) {
					freePos = lastCandidateIndex + 1;
					if (freePos > lastCandidateIndex) {
						lastCandidateIndex = freePos;
					}
				} else {
		//			freePos = freeCandidates[freeCandidatesPos--];
					freePos = freeCandidates.pop();
					if (lastCandidateIndex < freePos) lastCandidateIndex = freePos  ; 
				}
				
				
				//make sure we don't overflow
				if (freePos == candidatesD.length) {
					int newLength = candidatesD.length * (candidatesD.length < 1000000 ? 10 : 2 );
					float[] D = new float[newLength];
					int[] I = new int[newLength];
					int[] J = new int[newLength];
					//int[] C = new int[newLength];
					boolean[] act = new boolean[newLength];
					
		//			for (int x=0; x<=freeCandidatesPos; x++ ) 
		//				C[x] = freeCandidates[x];
					
					for (int x=0; x<candidatesD.length; x++ ) {
						D[x] = candidatesD[x];
						I[x] = candidatesI[x];
						J[x] = candidatesJ[x];
						act[x] = candidatesActive[x];
					}
					candidatesD = D;
					candidatesI = I;
					candidatesJ = J;
					candidatesActive = act;
					//freeCandidates = C;
				}
				candidatesD[freePos] = d;
				candidatesI[freePos] = i;
				candidatesJ[freePos] = j;
				candidatesActive[freePos] = true;
			}
				
		} else {
			
			d = d * (newK-2) - R[redirect[i]] - R[redirect[j]]; // really q' now  

			candHeapList.get(candHeapList.size()-1).insert(i, j, d);
		}
		
		
		
	}


	private void removeCandidate (int x) {
		
		//freeCandidates[++freeCandidatesPos] = x;
		freeCandidates.push(x);

		candidatesActive[x] = false;
		if (x == lastCandidateIndex) {
			//scan left to find it
			while (lastCandidateIndex>0 && !candidatesActive[lastCandidateIndex]) {lastCandidateIndex--;}
		}
	}
	
}
