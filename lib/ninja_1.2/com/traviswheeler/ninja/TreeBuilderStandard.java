package com.traviswheeler.ninja;


public class TreeBuilderStandard extends TreeBuilder {

	float[][] Q;

	
	public TreeBuilderStandard(String[] names, int[][] distances) {
		super(names, distances);
		
		Q = new float[K][];
		for (int i=0; i<K; i++) {
			Q[i] = new float[K - i - 1];
		}

		for (int i=0; i<K-1; i++) {
			for (int j=i+1; j<K; j++) { 
				Q[i][j-i-1] = D[i][j-i-1]*(K-2) - R[i] - R[j];
			}
		}

	}

	public TreeNode[] build () {

		int nextInternalNode = K;
		
		int first_i = 0;
		float minQ, q;
		int min_i, min_j;
		int ri, rj;
		while (nextInternalNode<2*K-3) {// until there are 3 left ... at which point the merging is obvious
			min_i = min_j = -1;
			minQ = Float.MAX_VALUE;

			int newK = 2*K - nextInternalNode;

			if (nextInternalNode == 1101 || nextInternalNode == 1102  || nextInternalNode == 1201 ) {

				float maxT = 0;
				float minT = Float.MAX_VALUE;
				float minD = Float.MAX_VALUE;
				for (int x=0; x<nextInternalNode; x++) {
					int rx = redirect[x];
					if (rx != -1) {
						if (R[rx] > maxT) 
							maxT = R[rx];
						if (R[rx] < minT) 
							minT = R[rx];
						for (int y=x+1; y<nextInternalNode; y++) {
							int ry = redirect[y];
							if (ry != -1) {
								float d = rx<ry ? D[rx][ry-rx-1] : D[ry][rx-ry-1];
								if (d < minD) 
									minD = d;
							}
						}
					}
				}

				int bins = 8;
				int binsize = 50;
				int hist[] = new int[bins];
				int histvals[] = new int[bins];
				float minDscaled = minD * (newK-2);
				histvals[0] = (int)(Math.ceil(minDscaled/binsize) + binsize);
				for (int i=1; i<bins; i++) {
					histvals[i] = histvals[i-1] + binsize; 
				}
				
				for (int i=first_i; i<nextInternalNode-1; i++) { 
					ri = redirect[i];
					if (ri != -1) {
						for (int j=i+1; j<nextInternalNode; j++) {								
							rj = redirect[j];
							if (rj != -1) {
								float d =  ri<rj ? D[ri][rj-ri-1] : D[rj][ri-rj-1];
								d *= (newK-2);
								for (int kk=0; kk<bins; kk++) {
									if (d < histvals[kk]) {
										hist[kk]++;
										break;
									}
								}							
							}
						}
					}
				}
				
/*				
				System.out.println("nextInternalNode: " + nextInternalNode);
				System.out.println("maxT: " + maxT);
				System.out.println("minT: " + minT);

				for (int kk=0; kk<bins; kk++) {
					System.out.println("<" + histvals[kk] + ": " + hist[kk]);
				}							
*/				
			}
			
			for (int i=first_i; i<nextInternalNode-1; i++) { 
				ri = redirect[i];
				if (ri != -1) {
					for (int j=i+1; j<nextInternalNode; j++) {										
						rj = redirect[j];
						if (rj != -1) {
							q = ri<rj ? Q[ri][rj-ri-1] : Q[rj][ri-rj-1];
							if (q < minQ) {
								min_i = i;
								min_j = j;
								minQ = q;
							}
						}
					}
				}
			}
			
			
			nodes[nextInternalNode].leftChild = nodes[min_i];
			nodes[nextInternalNode].rightChild = nodes[min_j];

			ri = redirect[min_i];
			rj = redirect[min_j];

//			if (nextInternalNode == 1101 || nextInternalNode == 1102  || nextInternalNode == 1201 ) 
//				System.err.println("(std) next node: " + nextInternalNode + ": " + min_i + "(" + ri +") , " + min_j + "(" + rj + ") Q= " + minQ);
			
			
			R[ri] = 0;
			redirect[min_i] = redirect[min_j] = -1;
			int Dxi, Dxj, Dij, tmp;
			for (int x=first_i; x<nextInternalNode; x++) {
				int rx = redirect[x];
				if (rx != -1) {
					
					Dxi = rx<ri ?  D[rx][ri-rx-1]  :  D[ri][rx-ri-1];
					Dxj = rx<rj ?  D[rx][rj-rx-1]  :  D[rj][rx-rj-1];
					Dij = ri<rj ? D[ri][rj-ri-1] : D[rj][ri-rj-1];
					tmp = (Dxi + Dxj - Dij) / 2;
										
					R[ri] += tmp;
					R[rx] += tmp - (Dxi + Dxj);
					
					if (rx < ri)  {
						D[rx][ri-rx-1] = tmp;
					} else { 
						D[ri][rx-ri-1] = tmp;
					}
				}
			}
			
			redirect[nextInternalNode++] = ri;
			
			
			newK = 2*K - nextInternalNode;
			for (int i=first_i; i<nextInternalNode-1; i++) {

				ri = redirect[i];
				if (ri != -1) {
					for (int j=i+1; j<nextInternalNode; j++) {
						rj = redirect[j];
						if (rj != -1) {							
							if (ri < rj)  {
								Q[ri][rj-ri-1] = D[ri][rj-ri-1]*(newK-2) - R[ri] - R[rj];
							} else {
								Q[rj][ri-rj-1] = D[rj][ri-rj-1]*(newK-2) - R[ri] - R[rj];
							}
						}
					}
				}
			}
		
			
			if (min_i == first_i || min_j == first_i) {
				while (redirect[first_i++] == -1);
				first_i--;
			}
		}
		
		finishMerging();
		
		return nodes;

	}

		
}
