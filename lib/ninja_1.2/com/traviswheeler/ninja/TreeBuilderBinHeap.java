	
package com.traviswheeler.ninja;

import java.util.Stack;

import com.traviswheeler.libs.BinaryHeap_IntKey;
import com.traviswheeler.libs.BinaryHeap_IntKey_TwoInts;
import com.traviswheeler.libs.LogWriter;

public class TreeBuilderBinHeap extends TreeBuilder {

		BinaryHeap_IntKey_TwoInts[][] heaps;

		int[] clustAssignments;
		long[] clustPercentiles;
		int[] clustersBySize;
		int[] clustSizes;
		
		int[] candidatesD;
		int[] candidatesI;
		int[] candidatesJ;
		boolean[] candidatesActive;
		Stack<Integer> freeCandidates;
		int lastCandidateIndex;
		
		public int[] candidateCountPerLoop;
		
		public TreeBuilderBinHeap(String[] names, int[][] distances) {
			super(names, distances);
			
			firstActiveNode = 0;
			nextActiveNode = new int[2*K-1];
			prevActiveNode = new int[2*K-1];
			for (int i=0; i<2*K-1; i++) {
				nextActiveNode[i] = i+1;
				prevActiveNode[i] = i-1;
			}
			
			candidateCountPerLoop = new int[K-1];

			
//			if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = K/4;

			try {
				clusterAndHeap(K);
			} catch (Exception e){
				LogWriter.stdErrLogln("Error in Binary Heap Tree Builder");
				LogWriter.stdErrLogln(e.getMessage());
			}
		}

		
		private void clusterAndHeap (int maxIndex ) throws Exception{
			
			// pick clusters
			clustAssignments = new int[K];
					
			long maxT = 0;
			long minT = Long.MAX_VALUE;
			int i,j, ri, rj;

			i = firstActiveNode;
			while (i<maxIndex) {
				ri = redirect[i];
				if (R[ri] > maxT) maxT = R[ri];
				if (R[ri] < minT) minT = R[ri];
				i=nextActiveNode[i];
			}
			clustPercentiles = new long[clustCnt];
			long seedTRange = maxT - minT;
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
			BinaryHeap_IntKey heap = new BinaryHeap_IntKey();
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
			if (verbose >= 4) {
				LogWriter.stdErrLogln("cluster sizes");
				for (i=0; i<clustCnt; i++) 
					LogWriter.stdErrLogln(i + " : " + clustSizes[i] + " ( " + clustPercentiles[i] + " )");
			}
			
			
			//	make PQs for each cluster pair (make candidate list, too)  ... this will allow old ones to be collected
			if (candidatesD == null) {
				candidatesD = new int[10000];
				candidatesI = new int[10000];
				candidatesJ = new int[10000];
				freeCandidates = new Stack<Integer>();
				candidatesActive = new boolean[10000];
			} //else - must already be created.  Just keep it the same size
			lastCandidateIndex = -1;

			if (heaps == null)
				heaps = new BinaryHeap_IntKey_TwoInts[clustCnt][clustCnt];
			
			for (i=0; i<clustCnt; i++) {
				for (j=i; j<clustCnt; j++) {
					if (heaps[i][j] != null) {
						heaps[i][j].makeEmpty();
					} else {
						heaps[i][j] = new BinaryHeap_IntKey_TwoInts();
					}
				}
			}
			
			int ra, rb, cra, crb ;
			int d;
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
					heaps[cra][crb].insert( i, j, d);   // this can be sped up for really big heaps by inserting all as one big list
					
//					if (cra == 13 && crb == 13)
//						System.out.println("h1.insert( " + i + ", " + j + ", (float)" + d + "); //" );
					
					j = nextActiveNode[j];
				}
				i = nextActiveNode[i];
			}

		}
		
		
		public TreeNode[] build () throws Exception{
			
			int nextInternalNode = K;

			int cand_cnt = 0;
			int defunct_cnt = 0;
			
			int i, j, x, ri, rj, rx, cluster, newK;
			int min_cand;
			int Dxi, Dxj, Dij, tmp;
			int a,b, ra, rb;
			int prev;
			int next;

			int clA, clB;
			
			long minQ, q, qLimit, minD;

			int[] maxT1 = new int[clustCnt]; 
			int[] maxT2 = new int[clustCnt];
			long[] maxT1val = new long[clustCnt];
			long[] maxT2val = new long[clustCnt];
			
			int stepsUntilRebuild = TreeBuilder.rebuildSteps;
			
			if ( stepsUntilRebuild == -1 ) stepsUntilRebuild = (int)(K * TreeBuilder.rebuildStepRatio);
			if ( stepsUntilRebuild < 500 ) 
				stepsUntilRebuild = K; //don't bother rebuilding for tiny trees

			
			
			newK = K;
			
			while (nextInternalNode<2*K-1) {// until there are 3 left ... at which point the merging is obvious
						
				
				//get two biggest T values for each cluster maxT1[] and maxT2[]
				for (i=0; i<clustCnt; i++) {
					maxT1[i] = maxT2[i] = -1;
					maxT1val[i] = maxT2val[i] = Long.MIN_VALUE;
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
		
				minQ = Long.MAX_VALUE;
				minD = Long.MIN_VALUE;
				
				//Go through current list of candidates, and find best so far.
				min_cand = -1;
				int clI, clJ;
				long maxTSum;
				int inactiveCnt = 0;
				for (x=lastCandidateIndex; x>=0; x--) {
					if (!candidatesActive[x]) {
						inactiveCnt++;
						continue;
					}
								
					ri = redirect[candidatesI[x]];
					rj = redirect[candidatesJ[x]];

					if (rj == -1 || ri == -1 /*leftovers from prior seqs redirected to this position*/) {
						candidatesActive[x] = false; // dead node ... can safely remove, 'cause we're going backwards through the list
						defunct_cnt++;
						if (x == lastCandidateIndex) {
							//scan left to find it
							int y = x;
							while (y>0 && !candidatesActive[y]) {y--;}
							lastCandidateIndex = y;
						}

//						if (nextInternalNode == 1118)	
//							System.out.println("defuncted " + x);
					} else {
						q = (long)candidatesD[x] * (newK-2) - R[ri] - R[rj]; 
						if (q <= minQ) {
//							if (nextInternalNode == 1150)	
//								System.out.println("(fib a) next node: " + nextInternalNode + ", cand: " + candidatesI[x] + ", " + candidatesJ[x] + " ( q=" +q + " , minQ=" + minQ +")");

							min_cand = x;
							minQ = q;
							minD = candidatesD[x];
						} 
					}
				}
				
				candidateCountPerLoop[K-newK] = lastCandidateIndex-inactiveCnt+1;

				
				/*	frequently (every 50 or 100 iters?), scan through the candidates, and return
				 * entries to the array heap that shouldn't be candidates any more
				 */								
				if ( (K-newK)%TreeBuilder.candidateIters == 0  && stepsUntilRebuild > TreeBuilder.candidateIters/2  ) {
					for (x=lastCandidateIndex; x>=0; x--) {
						if (!candidatesActive[x]) continue;
					
						ri = redirect[candidatesI[x]];
						rj = redirect[candidatesJ[x]];
						clI = clustAssignments[ri];
						clJ = clustAssignments[rj];
		
						maxTSum = maxT1val[clI] +  ( clI == clJ ? maxT2val[clI] :  maxT1val[clJ]) ;
						qLimit = (long)candidatesD[x] * (newK-2) - maxTSum;
								
						if (qLimit > minQ ) { 
							// it won't be picked as a candidate in next iter, so stick it back in the cluster heaps
							candidatesActive[x] = false;
							if (x == lastCandidateIndex) {
								//scan left to find it
								int y = x;
								while (y>0 && !candidatesActive[y]) {y--;}
								lastCandidateIndex = y;
							}
							if (clI<=clJ)
								heaps[clI][clJ].insert(candidatesI[x], candidatesJ[x], candidatesD[x]);
							else
								heaps[clJ][clI].insert(candidatesI[x], candidatesJ[x], candidatesD[x]);
						}
					}
				}

				//compact the candidate list
				if (lastCandidateIndex>0) {
					if (inactiveCnt > (float)lastCandidateIndex/5) {
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
								
								if (min_cand == right)
									min_cand = left;
	//							System.out.println(right + " --> " + left);
								
								left++;
								right--;
							}
						}
						lastCandidateIndex = right;
						freeCandidates.clear();
					}
				}
				
				//pull off entries for the fib-heaps that have some chance of having minQ
				int minPos, h_i, h_j;
				int h_d;
				BinaryHeap_IntKey_TwoInts h;
				for (a=0; a<clustCnt; a++) {
					for (b=a; b<clustCnt; b++) {
												
						clA = clustersBySize[a]<clustersBySize[b] ? clustersBySize[a] : clustersBySize[b];	
						clB = clustersBySize[a]<clustersBySize[b] ? clustersBySize[b] : clustersBySize[a];
						
						maxTSum = maxT1val[clA] +  ( clA == clB ? maxT2val[clA] :  maxT1val[clB]) ;
						
						h = heaps[clA][clB];			
						while (! h.isEmpty() ) {
				        	minPos = h.heapArray[1];
				        	h_d = h.keys[minPos];
				        	h_i = h.val1s[minPos];
				        	h_j = h.val2s[minPos];
							
				        	
							ri = redirect[h_i];
							rj = redirect[h_j];
								
							if (rj==-1 || ri==-1 /*that's an old pointer*/) {
								h.deleteMin();//pull it off
								
								
								defunct_cnt++;
								continue;
							}						
							q = (long)h_d * (newK-2);
							qLimit = q - maxTSum;
							q -=  R[ri] + R[rj];
								
							if (qLimit <= minQ) {
								// it's possible that this or one of the following nodes on the 
								// heap has Q-value less than the best I've seen so far			
								h.deleteMin();//pull it off
								
								int pos = appendCandidate(h_d, h_i, h_j);
//								LogWriter.stdErrLogln("appended " + heapNode.i + ", " + heapNode.j + " to " + pos + " (" + q + ")");
								cand_cnt++;
								
								if (q <= minQ) { // this is now the best I've seen								
									min_cand = pos;
									minQ = q;
									minD = h_d; 
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
				int bestI = candidatesI[min_cand];
				int bestJ = candidatesJ[min_cand];	
				candidatesActive[min_cand] = false;
				if (min_cand == lastCandidateIndex) {
					//scan left to find it
					int y = min_cand;
					while (y>0 && !candidatesActive[y]) {y--;}
					lastCandidateIndex = y;
				}
				freeCandidates.add(min_cand);

//				LogWriter.stdErrLogln("best was at " + min_cand);
				
				ri = redirect[bestI];
				rj = redirect[bestJ];
								
				nodes[nextInternalNode].leftChild = nodes[bestI];
				nodes[nextInternalNode].rightChild = nodes[bestJ];
				
				
				//assign branch lengths
				if (minD == Float.MIN_VALUE) {
					throw new Exception("minD was not assigned correctly");
				}
				if (newK==2) {
					nodes[bestI].length = nodes[bestJ].length = (float)minD / 200000000;
				} else {
					nodes[bestI].length = ((float)minD + (R[ri]-R[rj])/(newK-2)) / 200000000 ;
					nodes[bestJ].length = ((float)minD + (R[rj]-R[ri])/(newK-2)) / 200000000 ;
				}
				
				//if a length is negative, move root of that subtree around to compensate.
				if (nodes[bestI].length < 0) {
					nodes[bestJ].length += nodes[bestI].length;
					nodes[bestI].length = 0;
				} else if (nodes[bestJ].length < 0) {
					nodes[bestI].length += nodes[bestJ].length;
					nodes[bestJ].length = 0;
				}

				
				R[ri] = 0;
				redirect[bestI] = redirect[bestJ] = -1;
				
				// remove i,j from active list
				prev = prevActiveNode[bestI];
				next = nextActiveNode[bestI];
				prevActiveNode[next] = prev;
				if (prev == -1) 
					firstActiveNode = next;
				else 
					nextActiveNode[prev] = next;
				
				prev = prevActiveNode[bestJ];
				next = nextActiveNode[bestJ];
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
					
					//System.err.println(ri + ", " + rx + ": " + tmp + " (R[" + ri +"] = " + R[ri] + "), R[" + rx + "] = " + R[rx]);
					
					D[ra][rb-ra-1] = tmp;						

					x = nextActiveNode[x];
				}
				
				if (TreeBuilder.verbose >= 3) {
					LogWriter.stdErrLogln("(inmem) next node: " + nextInternalNode + ": " + bestI + "(" + ri +") , " + bestJ + "(" + rj + ") Q = " + minQ + " (" + cand_cnt + " cands; " + defunct_cnt +  " defunct); ");
					LogWriter.stdErrLogln("     lengths: " + nodes[bestI].length + ", " + nodes[bestJ].length );  
				}
				newK--;
					
				if ( stepsUntilRebuild == 0 ) {

					if (verbose >= 3) {
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
						heaps[cra][crb].insert(a, b, D[ra][rb-ra-1]);
						
						
						x = nextActiveNode[x];
					}
					//prev = prevActiveNode[nextInternalNode]; // last active node
					//nextActiveNode[prev] = nextInternalNode; // now this is the last active node.
					//prevActiveNode[nextInternalNode] = prev;
					redirect[nextInternalNode++] = ri;
					
				}
				
				
			}
			
					
			
			if (TreeBuilder.verbose >= 1) {				
				LogWriter.stdErrLogln(cand_cnt + " candidates added");
				LogWriter.stdErrLogln(defunct_cnt + " defunct nodes removed");
			}
			
			
			if (TreeBuilder.verbose >= 2) {				
				long max_cnt = 0;
				long max_cnt_iter = 0;
				float max_ratio = 0;
				long sum_possible = 0;
				long sum_cnt = 0;
				for (i=1; i<candidateCountPerLoop.length; i++) {
					long cnt = candidateCountPerLoop[i];
					if (cnt > max_cnt) {
						max_cnt = cnt;
						max_cnt_iter = i;
					}
					sum_cnt += cnt;
					
					long all_pairs_cnt = ( (K-i) * (K-i-1) / 2 ) ;
					sum_possible += all_pairs_cnt;
					
					float ratio = (float)cnt / all_pairs_cnt;
					if (ratio>max_ratio) max_ratio = ratio;
					
				}
				
				LogWriter.stdErrLogln("max # candidates: " + max_cnt + " at iteration " + max_cnt_iter + " of " + candidateCountPerLoop.length);
				LogWriter.stdErrLogln( "max ratio of candidates to possible pairs: " + String.format("%.7f", (float)max_ratio/100) + "%");
				
				LogWriter.stdErrLogln("average # candidates: " + (sum_cnt/(K-4)) );
				LogWriter.stdErrLogln("total # candidates: " + sum_cnt + "  ( of " + sum_possible + " possible)");

				LogWriter.stdErrLogln("avg ratio of candidates to possible pairs: " + String.format("%.7f", ((float)sum_cnt/sum_possible)/100 ) + "%");
				
			}

			return nodes;
		}

		int appendCandidate (int d, int i, int j) {
			int freePos;
			if (freeCandidates.isEmpty()) {
				freePos = lastCandidateIndex + 1;
			} else {
				freePos = freeCandidates.pop();
			}
			
			if (freePos > lastCandidateIndex) {
				lastCandidateIndex = freePos;
			}
			
			//make sure we don't overflow
			if (freePos == candidatesD.length) {
				int newLength = candidatesD.length * 10;
				int[] D = new int[newLength];
				int[] I = new int[newLength];
				int[] J = new int[newLength];
				boolean[] act = new boolean[newLength];
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
			}
			candidatesD[freePos] = d;
			candidatesI[freePos] = i;
			candidatesJ[freePos] = j;
			candidatesActive[freePos] = true;
			
			return freePos;
		}
			
	}
