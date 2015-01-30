package com.traviswheeler.ninja;

import java.io.File;

import com.traviswheeler.libs.ArrayHeapExtMem;
import com.traviswheeler.libs.BinaryHeap_TwoInts;


public class CandidateHeap extends ArrayHeapExtMem {

	int kPrime;
	float[] rPrimes;
	float[] rDeltas;
	int[] rowCounts;
	
	int[] nextActiveNode;
	int[] prevActiveNode;
	int firstActiveNode = -1;
	
	float k_over_kprime;

	float minDeltaSum;
	
	public int origSize = 0;
	public boolean expired = false;
	public int representedRowCount = 0;	
	
	TreeBuilderExtMem tb;
	
	public CandidateHeap(File dir, int[] activeIJs, int kPrime, TreeBuilderExtMem tb, long sizeExp) throws Exception {
		super(dir, activeIJs, sizeExp);
		initialize( kPrime, tb);
	}

	public CandidateHeap(File dir, int[] activeIJs, int kPrime, TreeBuilderExtMem tb) throws Exception {
		super(dir, activeIJs);
		initialize( kPrime, tb);
	}
	
	private void initialize (int kPrime, TreeBuilderExtMem tb) {
		this.tb = tb;
		this.kPrime = kPrime;
		rowCounts = new int[tb.nextInternalNode+1];
		rDeltas = new float[tb.nextInternalNode+1];
		nextActiveNode = new int[tb.nextInternalNode+1];
		prevActiveNode = new int[tb.nextInternalNode+1];
		
		rPrimes = new float[tb.R.length];
		for (int i=0; i< rPrimes.length; i++ ) {
			rPrimes[i] = tb.R[i];
		}
	}
	
    public void insert(int i, int j, float key)  throws Exception {
    	super.insert(i, j, key);
    	
    	
    	rowCounts[i]++;
    	rowCounts[j]++;
    }
    

    public void buildNodeList () {
    	origSize = size();
    	
    	int prev=-1;
    	for (int i=0; i<rowCounts.length; i++) {
    		if (rowCounts[i] > 0) {
    			representedRowCount++;
    			if (firstActiveNode==-1) {
    				firstActiveNode = i;
    			} else {
    				prevActiveNode[i] = prev;
    				nextActiveNode[prev] = i;
    			}
    			prev = i;
    		}
    	}
    	prevActiveNode[0] = -1;
    	nextActiveNode[prev] = -1;
    }
    
    
    public void removeMin()  throws Exception{
    	BinaryHeap_TwoInts H = getBinaryHeapWithMin();
    	int i = H.val1s[H.heapArray[1]];
    	int j = H.val2s[H.heapArray[1]];

		
    	int prev, next;    	
    	if (--rowCounts[i] == 0) { // compact list
    		representedRowCount--;
    		prev = prevActiveNode[i];
    		next = nextActiveNode[i];
    		if (next != -1) prevActiveNode[next] = prev;
    		if (prev != -1) nextActiveNode[prev] = next;
    	}

    	if (--rowCounts[j] == 0) { // compact list
    		representedRowCount--;
    		prev = prevActiveNode[j];
    		next = nextActiveNode[j];
    		if (next != -1) prevActiveNode[next] = prev;
    		if (prev != -1) nextActiveNode[prev] = next;
    	}    	
    	
    	super.removeMin();
    }
    
    
    
    public void calcDeltaValues (int newK) {
    	//prevActiveNode[0] = -1;
    	//nextActiveNode[0] = -1;
    	int x=firstActiveNode;
  
    	float minRdelt1, minRdelt2;
    	minRdelt1 = Float.MAX_VALUE;	
    	minRdelt2 = Float.MAX_VALUE;
    	
		k_over_kprime = (float)(newK-2)/(kPrime-2);

    	int rx, prev, next;
		while (x != -1) {
			rx = tb.redirect[x];
			
	    	if (rx == -1) { 
	    		prev = prevActiveNode[x];
	    		next = nextActiveNode[x];
	    		if (next != -1) prevActiveNode[next] = prev;
	    		if (prev != -1) nextActiveNode[prev] = next;
	    		x = next;
	    	} else {
	    		rDeltas[x] = k_over_kprime * rPrimes[rx]  - tb.R[rx];
	    	
	    		if (rDeltas[x] < minRdelt1) {
	    			minRdelt2 = minRdelt1;
	    			minRdelt1 = rDeltas[x];
	    		} else if (rDeltas[x] < minRdelt2) {
	    			minRdelt2 = rDeltas[x];
	    		}

	    		x = nextActiveNode[x];
	    	}
		}
		minDeltaSum =  minRdelt1 + minRdelt2;
    	
    }

}
