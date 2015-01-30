	
package com.traviswheeler.ninja;

import java.util.ArrayList;

import com.bluemarsh.graphmaker.core.util.FibonacciHeap;
import com.bluemarsh.graphmaker.core.util.IntPairFibonacciHeap;
import com.traviswheeler.libs.LogWriter;

public class TreeBuilder_FibOrig extends TreeBuilder {

		IntPairFibonacciHeap[][] fibHeaps;

		int[] clustAssignments;
		float[] clustPercentiles;
		int[] clustersBySize;
		int[] clustSizes;
		
		
		ArrayList<IntPairFibonacciHeap.Node> candidates;
		
		public TreeBuilder_FibOrig(String[] names, float[][] distances) {
			super(names, distances);
			
			firstActiveNode = 0;
			nextActiveNode = new int[2*K-1];
			prevActiveNode = new int[2*K-1];
			for (int i=0; i<2*K-1; i++) {
				nextActiveNode[i] = i+1;
				prevActiveNode[i] = i-1;
			}
			
			if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = K/4;

			
			clusterAndHeap(K);
		}

		
		private void clusterAndHeap (int maxIndex ) {
			
			// pick clusters
			clustAssignments = new int[K];
					
			float maxT = 0;
			float minT = Float.MAX_VALUE;
			int i,j, ri, rj;
//			for (i=0; i<maxIndex; i++) {
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
			if (verbose >= 4) {
				LogWriter.stdErrLogln("cluster sizes");
				for (i=0; i<clustCnt; i++) 
					LogWriter.stdErrLogln(i + " : " + clustSizes[i] + " ( " + clustPercentiles[i] + " )");
			}
			
			
			//	make PQs for each cluster pair (make candidate list, too)  ... this will allow old ones to be collected
			candidates = new ArrayList<IntPairFibonacciHeap.Node>();

			fibHeaps = new IntPairFibonacciHeap[clustCnt][clustCnt];			
			for (i=0; i<clustCnt; i++) {
				for (j=i; j<clustCnt; j++) {
					fibHeaps[i][j] = new IntPairFibonacciHeap();
				}
			}
			
			int ra, rb, cra, crb ;
			float d;
			i=firstActiveNode;
			while (i<maxIndex) {
				ri = redirect[i];
				j = nextActiveNode[i];
				while (j<maxIndex) {

					rj = redirect[j];
					if (ri<rj) {
						ra = ri;
						rb = rj;
					} else {
						ra = rj;
						rb = ri;
					}
					if (clustAssignments[ra] < clustAssignments[rb]) {
						cra = clustAssignments[ra]; 
						crb = clustAssignments[rb];
					} else {
						cra = clustAssignments[rb]; 
						crb = clustAssignments[ra];
					}
					d = D[ra][rb-ra-1];
					fibHeaps[cra][crb].insert(i, j, d);
					
//					if (cra == 13 && crb == 13)
//						System.out.println("h1.insert( " + i + ", " + j + ", " + d + "); //");

					
					j = nextActiveNode[j];
				}
				i = nextActiveNode[i];
			}

		}
		
		@SuppressWarnings("unchecked") 
		public TreeNode[] build () {
			int nextInternalNode = K;

			int cand_cnt = 0;
			int defunct_cnt = 0;
			
			int i, j, x, ri, rj, rx, cluster, newK;
			int min_cand;
			float Dxi, Dxj, Dij, tmp;
			int a,b, ra, rb;
			int prev;
			int next;

			int clA, clB;
			
			float minQ, q, qLimit;
			IntPairFibonacciHeap.Node heapNode;
			
			int[] maxT1 = new int[clustCnt]; 
			int[] maxT2 = new int[clustCnt];
			float[] maxT1val = new float[clustCnt];
			float[] maxT2val = new float[clustCnt];
			
			int stepsUntilRebuild = rebuildSteps;			
			
			while (nextInternalNode<2*K-3) {// until there are 3 left ... at which point the merging is obvious
						
				
				//get two biggest T values for each cluster maxT1[] and maxT2[]
				for (i=0; i<clustCnt; i++) {
					maxT1[i] = maxT2[i] = -1;
					maxT1val[i] = maxT2val[i] = Float.MIN_VALUE;
				}
				x=firstActiveNode;
				while (x < nextInternalNode) {
//				for (x=0; x<nextInternalNode; x++) {
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
				newK = 2*K - nextInternalNode;

				//Go through current list of candidates, and find best so far.
				min_cand = -1;
				int clI, clJ;
				float maxTSum;
				for (x=candidates.size()-1; x>=0; x--) {
					heapNode = candidates.get(x);			
					ri = redirect[heapNode.i];
					rj = redirect[heapNode.j];
										
					if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
						candidates.remove(x); // dead node ... can safely remove, 'cause we're going backwards through the list
						defunct_cnt++;
//						if (nextInternalNode == 1118)	
//							System.out.println("removed " + x); 
						min_cand--; // if we already found a good candidate, the removal of this one means we need to modify it's remembered location
					} else {
						q = heapNode.key * (newK-2) - R[ri] - R[rj]; 
						if (q <= minQ) {
//							if (nextInternalNode == 1150)	
//								System.out.println("(fib a) next node: " + nextInternalNode + ", cand: " + heapNode.i + ", " + heapNode.j + " ( q=" +q + " , minQ=" + minQ +")");

							min_cand = x;
							minQ = q;
						} 
					}
				}
				if ( (K-newK)%TreeBuilder.candidateIters == 0  && stepsUntilRebuild > TreeBuilder.candidateIters/2  ) { 					
					/*	frequently (every 50 or 100 iters?), scan through the candidates, and return
					 * entries to the array heap that shouldn't be candidates any more
					 */		
					for (x=candidates.size()-1; x>=0; x--) {
						heapNode = candidates.get(x);			
						ri = redirect[heapNode.i];
						rj = redirect[heapNode.j];
						clI = clustAssignments[ri];
						clJ = clustAssignments[rj];
	
						maxTSum = maxT1val[clI] +  ( clI == clJ ? maxT2val[clI] :  maxT1val[clJ]) ;
						qLimit = heapNode.key * (newK-2) - maxTSum;
							
						if (qLimit > minQ ) { 
							// it won't be picked as a candidate in next iter, so stick it back in the cluster heaps
							candidates.remove(x);
							if (min_cand > x) min_cand--;
							if (clI<=clJ)
								fibHeaps[clI][clJ].insert(heapNode.i,heapNode.j,heapNode.key);
							else
								fibHeaps[clJ][clI].insert(heapNode.i,heapNode.j,heapNode.key);
						}
					}
				}
	
				//pull off entries for the fib-heaps that have some chance of having minQ
				for (a=0; a<clustCnt; a++) {
					for (b=a; b<clustCnt; b++) {
						
						clA = clustersBySize[a]<clustersBySize[b] ? clustersBySize[a] : clustersBySize[b];	
						clB = clustersBySize[a]<clustersBySize[b] ? clustersBySize[b] : clustersBySize[a];
						
//						if (nextInternalNode == 1150 && clA == 13 && clB == 13) 
//							clA *= 1;
						
						maxTSum = maxT1val[clA] +  ( clA == clB ? maxT2val[clA] :  maxT1val[clB]) ;
												
						while (true) {
							heapNode = fibHeaps[clA][clB].min();
							if (heapNode == null)
								break;
							
							ri = redirect[heapNode.i];
							rj = redirect[heapNode.j];
								
							if (rj==-1 || ri==-1 /*that's an old pointer*/) {
/*								
								if (clA == 13 && clB == 13) {
									System.out.println("minPos = h.heapArray[1]; //");
									System.out.println("h_d = h.Ds[minPos];");
									System.out.println("h_i = h.Is[minPos];");
									System.out.println("h_j = h.Js[minPos];");
									System.out.println("h1.deleteMin();");
								} 
*/
								fibHeaps[clA][clB].removeMin();//pull it off
								defunct_cnt++;
								continue;
							}						
							q = heapNode.key * (newK-2);
							qLimit = q - maxTSum;
							q -=  R[ri] + R[rj];
								
							if (qLimit <= minQ) {
								// it's possible that this or one of the following nodes on the 
								// heap has Q-value less than the best I've seen so far			
/*
								if (clA == 13 && clB == 13) {
									System.out.println("minPos = h.heapArray[1];");
									System.out.println("h_d = h.Ds[minPos];");
									System.out.println("h_i = h.Is[minPos];");
									System.out.println("h_j = h.Js[minPos];");
									System.out.println("h1.deleteMin();");
								}
*/
								fibHeaps[clA][clB].removeMin();//pull it off
								
//								if (nextInternalNode == 1150 )	
//									System.out.println("(fib b) next node (" + clA + ", " + clB + "): " + nextInternalNode + ", cand: " + heapNode.i + ", " + heapNode.j + " ( q=" +q + " , minQ=" + minQ + ", qlimit=" + qLimit +")");

								candidates.add(heapNode);
								cand_cnt++;
								
								if (q <= minQ) { // this is now the best I've seen								
									min_cand = candidates.size()-1;
									minQ = q;
								}
							} else {
								break; // no use pulling more off the heap ... they can't beat the best so far.
							}
						}
					}
				}	
				
//				if (nextInternalNode == 1126 )	System.exit(1);
				
				//Now I know the position on the candidates array that has the best Q node.
				//Remove it from the candidates, merge the nodes, update D/T values
				// and possibly reset the candidates (every few iterations? when exceed some size?)
				heapNode = candidates.remove(min_cand);
								
				
				ri = redirect[heapNode.i];
				rj = redirect[heapNode.j];
				
								
				nodes[nextInternalNode].leftChild = nodes[heapNode.i];
				nodes[nextInternalNode].rightChild = nodes[heapNode.j];
				
				R[ri] = 0;
				redirect[heapNode.i] = redirect[heapNode.j] = -1;
				
				// remove i,j from active list
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
				x=firstActiveNode;
//				for (x=0; x<nextInternalNode; x++) {
				while (x<nextInternalNode) {
					
					rx = redirect[x];					
					Dxj = rx<rj ? D[rx][rj-rx-1] : D[rj][rx-rj-1];
					Dij = ri<rj ? D[ri][rj-ri-1] : D[rj][ri-rj-1];

					if (rx < ri)  {
						Dxi = D[rx][ri-rx-1];
						a = x;
						ra = rx;
						b = nextInternalNode;
						rb = ri;
					} else {
						Dxi = D[ri][rx-ri-1];
						a = nextInternalNode;
						ra = ri;
						b=x;
						rb = rx;
					}

					tmp = (Dxi + Dxj - Dij) / 2;
										
					R[ri] += tmp;
					R[rx] += tmp - (Dxi + Dxj);
					D[ra][rb-ra-1] = tmp;						

					x = nextActiveNode[x];
				}
				
				if (TreeBuilder.verbose >= 3)
					LogWriter.stdErrLogln("(fib) next node: " + nextInternalNode + ": " + heapNode.i + "(" + ri +") , " + heapNode.j + "(" + rj + ") Q = " + minQ + " (" + cand_cnt + " cands; " + defunct_cnt +  " defunct);  R[193] =" + R[193] + ", R[" + ri + "] = " + R[ri] );

					
				if ( stepsUntilRebuild == 0 ) {

					if (verbose >= 3) {
						LogWriter.stdErrLogln ("Resetting the clusters and corresponding PQs after " + (K-newK) + " iterations");
					}
					
					redirect[nextInternalNode++] = ri;
					clusterAndHeap(nextInternalNode);
					stepsUntilRebuild = rebuildSteps;
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
					int cra, crb;
					while (x<nextInternalNode) {
					//for (x=0; x<nextInternalNode; x++) {
					
						
						rx = redirect[x];
						if (rx < ri)  {
							a = x;
							ra = rx;
							b = nextInternalNode;
							rb = ri;
						} else {
							a = nextInternalNode;
							ra = ri;
							b=x;
							rb = rx;
						}


						if (clustAssignments[ra] < clustAssignments[rb]) {
							cra = clustAssignments[ra];
							crb = clustAssignments[rb];
						} else {
							crb = clustAssignments[ra];
							cra = clustAssignments[rb];
						}
						fibHeaps[cra][crb].insert(a, b, D[ra][rb-ra-1]);

//						if (cra == 13 && crb == 13)
//							System.out.println("h1.insert( " + a + ", " + b + ", " + D[ra][rb-ra-1] + ");" );
						
						x = nextActiveNode[x];
					}
					//prev = prevActiveNode[nextInternalNode]; // last active node
					//nextActiveNode[prev] = nextInternalNode; // now this is the last active node.
					//prevActiveNode[nextInternalNode] = prev;
					redirect[nextInternalNode++] = ri;
					
				}
				
				
			}
			
			if (TreeBuilder.verbose > 0) {				
				LogWriter.stdErrLogln(cand_cnt + " candidates added");
				LogWriter.stdErrLogln(defunct_cnt + " defunct nodes removed");
			}
			
			finishMerging();
			
			return nodes;
		}

			
	}
