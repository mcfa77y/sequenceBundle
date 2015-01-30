package com.traviswheeler.ninja;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.bluemarsh.graphmaker.core.util.FibonacciHeap;
import com.bluemarsh.graphmaker.core.util.IntPairFibonacciHeap;
import com.bluemarsh.graphmaker.core.util.IntPairFibonacciHeap.Node;
import com.traviswheeler.libs.ArrayHeapExtMem;
import com.traviswheeler.libs.Arrays;
import com.traviswheeler.libs.LogWriter;



public class TreeBuilderExtMem_w_candheap {

	int pageBlockSize;
	int K;
	String[] names;
	TreeNode[] nodes;
	int[] redirect;
	
	public static int clustCnt = 30;
	public static int candidateIters = 50;
	int[] nextActiveNode;
	int[] prevActiveNode;
	int firstActiveNode;

//	IntPairFibonacciHeap[][] fibHeaps;
	ArrayHeapExtMem[][] fibHeaps;

	int[] clustAssignments;
	float[] clustPercentiles;
	int[] clustersBySize;
	int[] clustSizes;
	
	int numDCols=0;
	int numDRowsPerFile=0;
	RandomAccessFile[] diskD=null; 
	float[] R;
	float[] R_prime;
	float[][] memD;
	int firstColInMem;
	int curColInMem;
	int pageFloats;

	float[] fBuff;
	byte[] bBuff;

	File njTmpDir;

	int nextInternalNode;
	int K_prime;
	int minRdeltPos1, minRdeltPos2;
	float minRdelt1, minRdelt2;

	Node node1;
	Node node2;
	
	//ArrayList<IntPairFibonacciHeap.Node> candidates;
	IntPairFibonacciHeap candidates =  new IntPairFibonacciHeap();
	ArrayList<IntPairFibonacciHeap.Node> candHoldingPen = new ArrayList<IntPairFibonacciHeap.Node>();
	ArrayList<IntPairFibonacciHeap.Node> recentCandidates = new ArrayList<IntPairFibonacciHeap.Node>();
	
	public TreeBuilderExtMem_w_candheap (String[] names, float R[], File njTmpDir, RandomAccessFile[] diskD, 
			int numDCols, int numDRowsPerFile, int pageBlockSize, int floatSize) throws Exception{
		
		this.njTmpDir = njTmpDir;
		this.names = names;
		this.numDCols = numDCols;
		this.numDRowsPerFile = numDRowsPerFile;
		this.diskD = diskD; 
		this.pageBlockSize = pageBlockSize; 
		pageFloats = pageBlockSize / floatSize;
		fBuff = new float[pageFloats];
		bBuff = new byte[pageBlockSize];
		
		this.R = R;		
		R_prime = new float[R.length];
		
		nextInternalNode = K = names.length;
		
		memD = new float[K][pageFloats];
		curColInMem = firstColInMem = K;
		
		if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = K/4;

		
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
		
		// pick clusters
		clustAssignments = new int[K];

		// past data points, used to restrict candidate consideration
		for (int i=0; i<R.length; i++) 
			R_prime[i] = R[i];
		K_prime = 2*K - nextInternalNode;

		
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
		clustPercentiles = new float[clustCnt];
		float seedTRange = maxT - minT;
		for (i=0; i<clustCnt-1; i++) {
			clustPercentiles[i] = minT + (i+1)*seedTRange/clustCnt;
		}
		clustPercentiles[clustCnt-1] = maxT;
		clustSizes = new int[clustCnt];
		i=firstActiveNode;
		while (i<maxIndex) {
			ri = redirect[i];
			for (j=0; j<clustCnt; j++) {
				if (R[ri]<=clustPercentiles[j]) {
					clustAssignments[ri] = j;
					clustSizes[j]++; 
					break;
				}
			}
			i=nextActiveNode[i];
		}
						

		//sort using a heap
		FibonacciHeap fib = new FibonacciHeap();
		for (i=0; i<clustCnt; i++) 
			fib.insert(new Integer(i), clustSizes[i]);
		clustersBySize = new int[clustCnt];
		for (i=0; i<clustCnt; i++) 
			clustersBySize[i] = ((Integer)fib.removeMin()).intValue();
			
		
		// tell me about cluster sizes
		if (TreeBuilder.verbose >= 2) {
			LogWriter.stdErrLogln("cluster sizes");
			for (i=0; i<clustCnt; i++) 
				LogWriter.stdErrLogln(i + " : " + clustSizes[i] + " ( " + clustPercentiles[i] + " )");
		}
		
		
		//	make PQs for each cluster pair (make candidate list, too)  ... this will allow old ones to be collected
//		candidates = new ArrayList<IntPairFibonacciHeap.Node>();
		candidates.clear();// = new IntPairFibonacciHeap();
		candHoldingPen.clear();
		recentCandidates.clear();
		
		if (fibHeaps == null)
			fibHeaps = new ArrayHeapExtMem[clustCnt][clustCnt];
		
		for (i=0; i<clustCnt; i++) {
			for (j=i; j<clustCnt; j++) {
				if (fibHeaps[i][j]!= null) {
					fibHeaps[i][j].initialize();
				} else {
					fibHeaps[i][j] = new ArrayHeapExtMem(njTmpDir);
				}
//				System.err.println(i + ", " + j + ": created " + ArrayHeapExtMem.totalFileCount + " files for array heap so far");
			}
		}
		
		int cra, crb ;
		float d;
    	int diskFile, diskPos;
    	int buffStart;
    	
		i=firstActiveNode;
		while (i<maxIndex) {
			ri = redirect[i];
			buffStart = -pageFloats; // get a new buffer
			diskFile =  (int)(Math.floor((double)ri / numDRowsPerFile));

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

						diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile))  + buffStart );
						diskD[diskFile].seek(diskPos);
						diskD[diskFile].read(bBuff);
						Arrays.byteToFloat(bBuff, fBuff);
					}
					d = fBuff[j - buffStart];					

				}

				fibHeaps[cra][crb].insert(i, j, d);
				j = nextActiveNode[j];
			}
			i = nextActiveNode[i];
		}

	}
	
	@SuppressWarnings("unchecked") 
	public TreeNode[] build ()  throws Exception{

		int cand_cnt = 0;
		int defunct_cnt = 0;
		
		int i, j, x, ri, rj, rx, cluster, newK;
		float Dxi, Dxj, Dij, tmp;
		int a,b;
		int prev;
		int next;

		int clA, clB;
		
		float minQ, q, qLimit;
		IntPairFibonacciHeap.Node heapNode;
		int min_cand = -1;
		
		int[] maxT1 = new int[clustCnt]; 
		int[] maxT2 = new int[clustCnt];
		float[] maxT1val = new float[clustCnt];
		float[] maxT2val = new float[clustCnt];
		
		int stepsUntilRebuild = TreeBuilder.rebuildSteps;
		
		newK = K;
		
		while (nextInternalNode<2*K-3) {// until there are 3 left ... at which point the merging is obvious
								
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
			

			//Go through current lists of candidates, and find best so far.
			heapNode = candidates.min();
			if (heapNode != null) {
				float k_over_kprime = (float)(newK-2)/(K_prime-2);
				float minMod = minRdelt1 + minRdelt2;
				
				while ( heapNode != null &&  k_over_kprime * heapNode.key + minMod  < minQ) { // "key" hold the q_prime value (q at time of insertion to candidate list)
					ri = redirect[heapNode.i];
					rj = redirect[heapNode.j];
					candidates.removeMin(); // remove the one we're looking at
					if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {	
						//don't bother to keep it for return to the minheap
						defunct_cnt++;
					} else {
						q = heapNode.fl * (newK-2) - R[ri] - R[rj]; //"fl" is d_ij  ... convert to a double for better precision
						// we know  k_over_kprime * heapNode.key + minMod  < minQ 
						//push onto some temporary list, from which it will eventually be pushed back onto the heap.
						candHoldingPen.add(heapNode);
						if (q <= minQ) {
							if (nextInternalNode == 1376)	
								System.out.println("(extmem a) next node: " + nextInternalNode + ", cand: " + heapNode.i + ", " + heapNode.j + " ( q=" +q + " , minQ=" + minQ +")");
							min_cand = candHoldingPen.size()-1;
							minQ = q;
						}
						
					}
					heapNode = candidates.min();
				}
		
			}
			if (TreeBuilder.verbose >= 3) {
				LogWriter.stdErrLog(nextInternalNode + " : Number candidates viewed : " + 
					candHoldingPen.size() + "/" + (candidates.size() + candHoldingPen.size() ) +
					" (heap) ;; " ) ;
			}

			int rc_size = recentCandidates.size();
			for (i=recentCandidates.size()-1; i>=0; i--) {
				Node n = recentCandidates.get(i);
				ri = redirect[n.i];
				rj = redirect[n.j];
				if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {	
					defunct_cnt++;
					recentCandidates.remove(i);
				} else {
					q = n.key * (newK-2) - R[ri] - R[rj]; 
					if (q <= minQ) {
						//push onto some temporary list, from which it will eventually be pushed back to this list.
						if (nextInternalNode == 1376)	
							System.out.println("(extmem a2) next node: " + nextInternalNode + ", cand: " + n.i + ", " + n.j + " ( q=" +q + " , minQ=" + minQ +")");

						candHoldingPen.add(n);
						recentCandidates.remove(i);
						min_cand = candHoldingPen.size()-1;
						minQ = q;
					}
				}
			}
			if (TreeBuilder.verbose >= 3) {
				LogWriter.stdErrLogln( rc_size + " (recent) " ) ;
			}

			//pull off entries for the fib-heaps that have some chance of having minQ
			for (a=0; a<clustCnt; a++) {
				for (b=a; b<clustCnt; b++) {
					clA = clustersBySize[a]<clustersBySize[b] ? clustersBySize[a] : clustersBySize[b];	
					clB = clustersBySize[a]<clustersBySize[b] ? clustersBySize[b] : clustersBySize[a];
					
					float maxTSum = maxT1val[clA];
					if (clA == clB) 
						maxTSum +=  maxT2val[clA];
					else
						maxTSum += maxT1val[clB];
						
					
					while (true) {
						heapNode = fibHeaps[clA][clB].min();
						if (heapNode == null)
							break;
						
						ri = redirect[heapNode.i];
						rj = redirect[heapNode.j];
							
						if (rj==-1 || ri==-1 /*that's an old pointer*/) {
							fibHeaps[clA][clB].removeMin();//pull it off
							defunct_cnt++;
							continue;
						}						
						q = heapNode.fl * (newK-2); //"fl" is d_ij  ... convert to a double for better precision
						qLimit = q - maxTSum;
						q -=  R[ri] + R[rj];
							
						
						if (qLimit <= minQ) {
							// it's possible that this or one of the following nodes on the 
							// heap has Q-value less than the best I've seen so far	
							
							fibHeaps[clA][clB].removeMin();//pull it off
							candHoldingPen.add(heapNode);
							cand_cnt++;
							if (nextInternalNode == 1376 )	
								System.out.println("(extmem b) next node: " + nextInternalNode + ", cand: " + heapNode.i + ", " + heapNode.j + " ( q=" +q + " , minQ=" + minQ +")");
							
							if (q <= minQ) { // this is now the best I've seen								
								min_cand = candHoldingPen.size()-1;
								minQ = q;
							}
						} else {
							break; // no use pulling more off the heap ... they can't beat the best so far.
						}
					}
				}
			}	
				
			
			//Now I know the position on the candidates array that has the best Q node.
			//Remove it from the candidates, merge the nodes, update D/T values
			// and possibly reset the candidates (every few iterations? when exceed some size?)
			heapNode = candHoldingPen.remove(min_cand);
			
			//need to move everything from the holding pen back to the cands PQ  or list (depending on age)
			int nodeAtKprime = 2*K - K_prime - 1;
			for (Node n : candHoldingPen) {
				if (n.i > nodeAtKprime || n.j > nodeAtKprime) { 
					recentCandidates.add(n);
				} else { // it's older, so stick it on the PQ
					// I'll want candidates sorted by q, not d, so change the key					
					n.key = n.fl * (K_prime-2) -  R_prime[redirect[n.i]] - R_prime[redirect[n.j]];
					candidates.insert(n);
				}
			}
			candHoldingPen.clear();
				
			ri = redirect[heapNode.i];
			rj = redirect[heapNode.j];
							
			nodes[nextInternalNode].leftChild = nodes[heapNode.i];
			nodes[nextInternalNode].rightChild = nodes[heapNode.j];
			
			// remove i,j from active list
			redirect[heapNode.i] = redirect[heapNode.j] = -1;
			
			prev = prevActiveNode[heapNode.i];
			next = nextActiveNode[heapNode.i];
			prevActiveNode[next] = prev;
			if (prev == -1) 
				firstActiveNode = next;
			else 
				nextActiveNode[prev] = next;
			
			prev = prevActiveNode[heapNode.j];
			next = nextActiveNode[heapNode.j];
			prevActiveNode[next] = prev;
			if (prev == -1) 
				firstActiveNode = next;
			else 
				nextActiveNode[prev] = next;


			//calculate new D and T values
	    	//    	for all these D reads, should check if the i/j/x value is greater than the most recently written-to-disk index
	    	//   	 ... if not, then read from the memD, not diskD
			R[ri] = 0;
			int diskFile_i = -1;
	    	int diskFile_j = -1;
	    	int diskPos;
	    	
	    	diskFile_i =  (int)(Math.floor((double)ri / numDRowsPerFile));
	    	diskFile_j =  (int)(Math.floor((double)rj / numDRowsPerFile));

	    	// I need to get this before going into the "foreach x" loop, 'cause I need Dij for all new vals.
			if (heapNode.i>=firstColInMem) {
				Dij = memD[rj][heapNode.i-firstColInMem];
			} else if (heapNode.j>=firstColInMem) {
					Dij = memD[ri][heapNode.j-firstColInMem];
			} else {	    	 
		    	diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile_i))  + heapNode.j );
		    	diskD[diskFile_i].seek(diskPos);
		    	Dij = diskD[diskFile_i].readFloat();
			}
			
	    	float[] fBuff_i = new float[pageFloats];
	    	float[] fBuff_j = new float[pageFloats];
	    	byte[] bBuff = new byte[4*pageFloats];
	    	int buffStart_i = -pageFloats;
	    	int buffStart_j = -pageFloats;
	    	float rxDelta;
	    	minRdeltPos1 = minRdeltPos2 = -1;
	    	minRdelt1 = minRdelt2 = Float.MAX_VALUE;
	    	
	    	x=firstActiveNode;
			float k_over_kprime = (float)(newK-3)/(K_prime-2);

			while (x<nextInternalNode) {
				
				rx = redirect[x];
				

				if (heapNode.i>=firstColInMem) {
					Dxi = memD[rx][heapNode.i-firstColInMem];
				} else if (x>=firstColInMem) {
					Dxi = memD[ri][x-firstColInMem];
				} else {					
					if (x >= buffStart_i + pageFloats) {	
						//read in next page;
						while ( (buffStart_i += pageFloats) + pageFloats <= x); // probably just move to the next page

						diskPos = 4 * (numDCols * (ri-(numDRowsPerFile*diskFile_i))  + buffStart_i );
						diskD[diskFile_i].seek(diskPos);
						diskD[diskFile_i].read(bBuff);
						Arrays.byteToFloat(bBuff, fBuff_i);
					}
					Dxi = fBuff_i[x - buffStart_i];					
				}

				
				if (heapNode.j>=firstColInMem) {
					Dxj = memD[rx][heapNode.j-firstColInMem];
				} else if (x>=firstColInMem) {
					Dxj = memD[rj][x-firstColInMem];
				} else {
					if (x >= buffStart_j + pageFloats) {	
						//read in next page;
						while ( (buffStart_j += pageFloats) + pageFloats <= x); // probably just move to the next page
						
						diskPos = 4 * (numDCols * (rj-(numDRowsPerFile*diskFile_j))  + buffStart_j );
						diskD[diskFile_j].seek(diskPos);
						diskD[diskFile_j].read(bBuff);
						Arrays.byteToFloat(bBuff, fBuff_j);
					}
					Dxj = fBuff_j[x - buffStart_j];					
					
				}
				
				tmp = (Dxi + Dxj - Dij) / 2;					
				R[ri] += tmp;				
				
				R[rx] += tmp - (Dxi + Dxj);
				
				if ( x < nodeAtKprime ) { // I only want changes in old nodes to affect my limits
					rxDelta = k_over_kprime * R_prime[rx]  - R[rx];
					if (rxDelta < minRdelt1) {
						minRdelt2 = minRdelt1;
						minRdeltPos2 = minRdeltPos1;
						minRdelt1 = rxDelta;
						minRdeltPos1 = rx;
					} else if (rxDelta < minRdelt2) {
						minRdelt2 = rxDelta;
						minRdeltPos2 = rx;					
					}
				} else {
					rxDelta = k_over_kprime * R_prime[rx]  - R[rx];
					rxDelta *=1;
				}
//				instead of writing to the file, write to an in-memory D buffer.  if that buffer is full, then write it to disk
				//D[ra][rb-ra-1] = tmp;
				memD[rx][nextInternalNode - firstColInMem] = tmp;
				if (x>=firstColInMem) 
					memD[ri][x - firstColInMem] = tmp;
				
				
				x = nextActiveNode[x];
			}

			R_prime[ri] = -1;
			
			if (TreeBuilder.verbose >= 2)
				LogWriter.stdErrLogln("(ext) next node: " + nextInternalNode + ": " + heapNode.i + "(" + ri +") , " + heapNode.j + "(" + rj + ") Q = " + minQ + " (" + cand_cnt + " cands; " + defunct_cnt +  " defunct);  R[193] =" + R[193] + ", R[" + ri + "] = " + R[ri] );

			redirect[nextInternalNode] = ri;
			
			curColInMem++;			
			int y, diskFile;
			if (curColInMem == firstColInMem + pageFloats) {
		    	//write memD to diskD  ... there's just one buffer per row.

		    	// first, we append each row in memD to existing rows in the file (one disk block per row)
		    	x=firstActiveNode;
				while (x<nextInternalNode) {
					rx = redirect[x];
			    	diskFile =  (int)(Math.floor((double)rx / numDRowsPerFile));

					for ( y=0; y<pageFloats; y++) 
						fBuff[y] = memD[rx][y];							
					Arrays.floatToByte(fBuff, bBuff);
					diskPos = 4 * (numDCols * (rx-(numDRowsPerFile*diskFile)) + firstColInMem )   ;
			    	diskD[diskFile].seek(diskPos);			    	
			    	diskD[diskFile].write(bBuff);
								    	
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

		    		diskFile =  (int)(Math.floor((double)ry / numDRowsPerFile));

		    		x=firstActiveNode;
		    		while (x<nextInternalNode) {
		    			//many entries in the buffer are left in default.
		    			// minor speedup could be had in the conversion routine (but I won't bother)
		    			rx = redirect[x];
		    		
		    			fBuff_horiz[x] = memD[rx][i-firstColInMem];	    	
		    			x = nextActiveNode[x];
		    		}
					Arrays.floatToByte(fBuff_horiz, bBuff_horiz);
					diskPos = 4 * (numDCols * (ry-(numDRowsPerFile*diskFile))  )   ;
					diskD[diskFile].seek(diskPos);
			    	diskD[diskFile].write(bBuff_horiz);
		    		
		    	}
				
			}

			newK--;
			
			if ( stepsUntilRebuild == 0 ) {

				if (TreeBuilder.verbose ==2) {
					LogWriter.stdErrLogln ("Resetting the clusters and corresponding PQs after " + (K-newK-1) + " iterations");
				}
				nextInternalNode++;
//				redirect[nextInternalNode++] = ri;
				clusterAndHeap(nextInternalNode);
				stepsUntilRebuild = TreeBuilder.rebuildSteps;
				//stepsUntilCandClear = clearCandidateSteps;
			} else {
				stepsUntilRebuild--;
	
				// re-set the max levels for clusters (based on one-iteration-old data)
				for (j=0; j<clustCnt; j++) {
					//LogWriter.stdErrLogln("updating cluster " + j + " from " + clustPercentiles[j] + " to "  + maxT1val[j]);	
					clustPercentiles[j] = maxT1val[j];	
				}
				//then pick new cluster for ri, based on it's T value;
				for (j=0; j<clustCnt; j++) {
					if (R[ri]<=clustPercentiles[j]) {
						clustAssignments[ri] = j;
						break;
					}
				}
				
				// ... then add all new distances to the appropriate cluster pair
				x = firstActiveNode;
				float d;
				while (x<nextInternalNode) {
					rx = redirect[x];
					d = memD[rx][nextInternalNode - firstColInMem];
					if (clustAssignments[ri] < clustAssignments[rx])   
						fibHeaps[clustAssignments[ri]][clustAssignments[rx]].insert(nextInternalNode, x, d);
					else 
						fibHeaps[clustAssignments[rx]][clustAssignments[ri]].insert(nextInternalNode, x, d);

					x = nextActiveNode[x];
				}
//				redirect[nextInternalNode++] = ri;
				nextInternalNode++;
				
				
				
				/*	frequently (every 50 or 100 iters?), update K' and the R' values:
				then, scan through the candidates, and:
				1) if it would just get put back on candidate list in next iter, update it's q to new K' 
				2) otherwise, stick back on the appropriate cluster heap
				 */			
				if ( (K-newK)%candidateIters == 0  && stepsUntilRebuild > candidateIters/2  ) {

					K_prime = 2*K - nextInternalNode;
					
					x=firstActiveNode;
					while (x < nextInternalNode) {
						rx = redirect[x];
						R_prime[rx] = R[rx];
						
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
					

					ArrayList<IntPairFibonacciHeap.Node> list = new ArrayList<IntPairFibonacciHeap.Node>();
					candidates.getNodeList(list);
					candidates.clear();
					minQ = Float.MAX_VALUE;
					//loop through once to find the minimum
					for (IntPairFibonacciHeap.Node n : list) {
						ri = redirect[n.i];
						rj = redirect[n.j];
						if (ri!=-1 && rj!=-1) {
							q = n.fl * (newK-2) - R[ri] - R[rj];
							if (q == -1186.5923)
								q *= 1;
							if (q<minQ) minQ = q;
						}
					}
					//then again to either return nodes to the big pool, or back to the candidates list
					float maxTSum;
					int clI, clJ;
					for (IntPairFibonacciHeap.Node n : list) {
						ri = redirect[n.i];
						rj = redirect[n.j];
						if (ri == -1 || rj == -1) {
							//dead entry. Don't do anything with it
							defunct_cnt++;
						} else {
							clI = clustAssignments[ri];
							clJ = clustAssignments[rj];

							maxTSum = maxT1val[clI];
							if (clI == clJ) 
								maxTSum +=  maxT2val[clI];
							else
								maxTSum += maxT1val[clJ];


							n.key = n.fl * (newK-2); //"fl" is d_ij  ... convert to a double for better precision
							qLimit = n.key - maxTSum;
							
							if (qLimit > minQ ) { 
								// it won't be picked as a candidate in next iter, so stick it back in the cluster heaps
								if (clI<=clJ)
									fibHeaps[clI][clJ].insert(n.i,n.j,n.fl);
								else
									fibHeaps[clJ][clI].insert(n.i,n.j,n.fl);
								if (TreeBuilder.verbose >=3)
									LogWriter.stdErrLog("|");
							}  else {
								//back to the candidates list, but with a new Q value
								n.key  -=  R[ri] + R[rj];
								candidates.insert(n);
								if (TreeBuilder.verbose >=3)
									LogWriter.stdErrLog(".");
							}
							
						}
					}
					if (TreeBuilder.verbose >=3)
						LogWriter.stdErrLogln("");
					
					
				}

				
			}

			if (curColInMem == firstColInMem + pageFloats) 
				firstColInMem = curColInMem;

			
		}
		
		
		if (TreeBuilder.verbose > 0) {				
			LogWriter.stdErrLogln(cand_cnt + " candidates added");
			LogWriter.stdErrLogln(defunct_cnt + " defunct nodes removed");
		}
		
		finishMerging();
		
		return nodes;
		
	}

	
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

	
}
