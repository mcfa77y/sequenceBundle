package com.traviswheeler.ninja;


abstract public class TreeBuilder {

	int K;
	String[] names;
	int[][] D;
	long[] R;
	TreeNode[] nodes;
	int[] redirect;
	
	public static boolean distInMem = false; 
	public static boolean rebuildStepsConstant = false; // otherwise, decreasing
	public static float rebuildStepRatio = (float)0.5;
	public static int rebuildSteps  = -1;
	public static int clustCnt = 30;
	public static int candidateIters = 50;

	int[] nextActiveNode;
	int[] prevActiveNode;
	int firstActiveNode;

	 
	public static int verbose = 1;
	
	public TreeBuilder (String[] names, int[][] distances) {
		this.names = names;
		D = distances;
		K = names.length;
		R = new long[K];
		redirect = new int[2*K-1];
		nodes = new TreeNode[2*K-1];	
		
		//if ( TreeBuilder.rebuildSteps == -1 ) TreeBuilder.rebuildSteps = (int)(K * rebuildStepRatio);

		int i,j;

		for (i=0; i<K; i++) {  
			redirect[i] = i;
			nodes[i] = new TreeNode(names[i]);
		}
		
		for (i=K; i<2*K-1; i++) {
			redirect[i] = -1;
			nodes[i] = new TreeNode();
		}

		for (i=0; i<K-1; i++) {
			for (j=i+1; j<K; j++) { 
				R[i] += D[i][j-i-1];
				R[j] += D[i][j-i-1];
			}
		}

	}
	 
	protected void finishMerging() {
		// only two trivial merges left:
		// this code will get cleaner
		int last_i=0, last_j=0, last_k=0;
		int ri, rj, rk;
		int i=0;
		while (redirect[i++]==-1);
		ri = redirect[i-1];
		last_i=i-1;
		
		while (redirect[i++]==-1);
		rj = redirect[i-1];
		last_j=i-1;
		
		while (redirect[i++]==-1);
		rk = redirect[i-1];
		last_k=i-1;

		int nextNode = 2*K-3;
		
		nodes[nextNode].leftChild = nodes[last_i];
		nodes[nextNode].rightChild = nodes[last_j];
		
		float d_ij = ri<rj? D[ri][rj] : D[rj][ri];
		nodes[last_i].length = (d_ij + (R[ri]-R[rj])/2) / 20000000 ;
		nodes[last_j].length = (d_ij + (R[rj]-R[ri])/2) / 20000000 ;
		
		//if a length is negative, move root of that subtree around to compensate.
		if (nodes[last_i].length < 0) {
			nodes[last_j].length -= nodes[last_i].length;
			nodes[last_i].length = 0;
		} else if (nodes[last_j].length < 0) {
			nodes[last_i].length -= nodes[last_j].length;
			nodes[last_j].length = 0;
		}

		
		float d_ik = ri<rk? D[ri][rk] : D[rk][ri];		
		float d_jk = rj<rk? D[rj][rk] : D[rk][rj];
		float d_ijk = (d_ik + d_jk - d_ij) / 2;
		
		nodes[nextNode+1].leftChild = nodes[nextNode];
		nodes[nextNode+1].rightChild = nodes[last_k];

		nodes[nextNode].length = (d_ijk + (R[ri]-R[rj])/2) / 20000000 ;
		nodes[last_k].length = (d_ijk + (R[rj]-R[ri])/2) / 20000000 ;
		
		//if a length is negative, move root of that subtree around to compensate.
		if (nodes[last_i].length < 0) {
			nodes[last_j].length -= nodes[last_i].length;
			nodes[last_i].length = 0;
		} else if (nodes[last_j].length < 0) {
			nodes[last_i].length -= nodes[last_j].length;
			nodes[last_j].length = 0;
		}

		
	}

	abstract public TreeNode[] build () throws Exception ;

	
}
