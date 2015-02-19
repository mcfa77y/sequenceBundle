/*
 * $Id: ArrayHeap.java,v 1.2 2009/02/05 00:10:00 twheeler Exp $
 */
package com.traviswheeler.libs;

import java.util.ArrayList;
import java.util.LinkedList;
import com.bluemarsh.graphmaker.core.util.IntPairFibonacciHeap;

/**
 * This class implements an Array heap data structure. 
 * The run time of most of these methods is O(log n).
 * It's greatest value is as a reference implementations for the
 * (slightly) more complicated External-Memory Array Heap
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a set concurrently, and at least one of the
 * threads modifies the set, it <em>must</em> be synchronized externally.
 * This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the set.</p>
 *
 * @author  Travis Wheeler
 */
public class ArrayHeap {

	final float c = (float)1/7;
	final int blockSize = 1024;
	int mem;
	int cM;
	int numSlots;
	int numNodesPerBlock;
	int n=0;
	IntPairFibonacciHeap H1, H2;
	Node[][][] L; //4 levels, each level containing a set of slots (arrays).
				// in the External memory heap, this will be a 2-D array of filehandles.
	int[][] cntOnHeap;
	int[][] slotPositions;
	int[] slotsUnderHalfFull; // there will be at most one per level
	ArrayList<LinkedList<Integer>> freeSlots;
	
	
	public ArrayHeap (int mem) {	
		this.mem = mem;
		cM = (int)(c*mem); 
		numSlots = (int)((cM/blockSize)-1);
		numNodesPerBlock = blockSize/3;
		slotsUnderHalfFull = new int[4];

		clear();
	}
	

    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new node is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(lg n)</em></p>
     *
     * @param  value1  
     * @param  value2  
     * @param  key  
     */
    public void insert(int value1, int value2, float key)  { // I use a float here because all I'm storing are d values, but allow the fibheap node to be a double, so I can track q values with better precision
    	H1.insert(value1, value2, key);
    	n++;
    	    	
    	if (H1.size() == 2* cM) {
    		//pull all nodes off heap, put into a new Node array, then put into L1 (or push down to L2, L3, or L4)
    		IntPairFibonacciHeap.Node fibNode;
    		Node[] tmpNodeArray = new Node[cM];
    		for (int i=0; i<cM; i++) {
    			fibNode = H1.removeMin();
    			tmpNodeArray[i] = new Node();
    			tmpNodeArray[i].key = (float)fibNode.key;
    			tmpNodeArray[i].value1 = fibNode.i;
    			tmpNodeArray[i].value2 = fibNode.j;
    		}
    		
    		int lvl=0;
    		while (freeSlots.get(lvl).peek() == null ) { 
    			// no free slot at L_i, so need to pull all the L_i entries off and push them to L_(i+1) (or further)
    			tmpNodeArray = mergeLevel (lvl, tmpNodeArray );

    			//remove the defunct entries from H2 (the appropriate number will get added back in a moment)
    			H2.deleteLevel(lvl);

    			lvl++;
    		}    		
        	int newSlot = store(lvl,tmpNodeArray);        	
        	load(lvl, newSlot);
        	
    	}
    }

    private void load (int level, int slot) {
		int[] slotPos = slotPositions[level];
		Node[] nodes = L[level][slot];
		Node node;
		
		int endPos = slotPos[slot] + numNodesPerBlock;
		if (endPos > nodes.length) endPos = nodes.length;

		cntOnHeap[level][slot] = endPos - slotPos[slot];
		while ( slotPos[slot] < endPos )  {			
			node = nodes[slotPos[slot]];
			H2.insert(node.value1, node.value2, node.key, level, slot);
			slotPos[slot]++;
		}
		
		int remaining = nodes.length - slotPos[slot] + cntOnHeap[level][slot];
		int cntMax = (int)( Math.pow(cM, level+1) / Math.pow(blockSize, level));
		
		if (remaining  <= cntMax/2 ) 
			slotsUnderHalfFull[level] = slot;

    }
    
	private int store (int level, Node[] nodes) {
		int freeSlot = freeSlots.get(level).remove(); 
		
		L[level][freeSlot] = nodes;
		slotPositions[level][freeSlot] = 0;
		cntOnHeap[level][freeSlot] = 0;
		
		return freeSlot;
	}

	
	private Node[] mergeLevel (int level, Node[] appendedNodes ) {
		Node[][] Li = L[level];
		int[] slotPos= slotPositions[level];

		int newCnt = appendedNodes.length;
		for (int j=0; j<numSlots; j++){
			slotPos[j] -= cntOnHeap[level][j]; // move back the pointers on the L_i arrays to include positions that are still on the heap 
			newCnt += Li[j].length  - slotPos[j];
		}
		
		
		//For slot at level i, it will be of size at most (c*mem)^i / blockSize^(i-1)
/*
		int newCntMax = (int)( Math.pow(cM, level+2) / Math.pow(blockSize, level+1)); 
		if (newCnt > newCntMax) {
			LogWriter.stdErrLogln("Too many new nodes at level " + level);
			throw new Exception(); 
		}
		*/
		Node[] nextNodes = new Node[newCnt];

		int minSlot = -1;
		float min;
		int appPos=0;
		for (int i=0; i<newCnt; i++){ // that's how many nodes need to be copied
			if (appPos < appendedNodes.length) {
				minSlot = numSlots;
				min = appendedNodes[appPos].key;
			} else {
				min = Float.MAX_VALUE;
			}
			for (int j=0; j<numSlots; j++){
				if (slotPos[j] == Li[j].length) continue;
    			if ( Li[j][slotPos[j]].key < min) {
        			minSlot = j;
        			min = Li[j][slotPos[j]].key;	            				
    			}
			}
			if (minSlot == numSlots) {
				nextNodes[i] = appendedNodes[appPos];
				appPos++;
			} else {
				nextNodes[i] = Li[minSlot][slotPos[minSlot]];
				slotPos[minSlot]++;
			}
		}
		
		//clean up old slots
		LinkedList<Integer> l = freeSlots.get(level);
		l.clear();
		for (int j=0; j<numSlots; j++) {
			Li[j] = null;
			l.add(j);
		}

		return nextNodes;

	}
	
    /**
     * Removes all elements from this heap.
     */
    public void clear() {
		n = 0;
		H1 = new IntPairFibonacciHeap();
		H2 = new IntPairFibonacciHeap(); 
		
		cntOnHeap = new int[4][numSlots];
		slotPositions = new int[4][numSlots];
		L = new Node[4][numSlots][];
		
		freeSlots = new ArrayList<LinkedList<Integer>>(4);
		for (int i=0; i<4; i++) {
			LinkedList<Integer> l = new LinkedList<Integer>();
			for (int j=0; j<numSlots; j++) 
				l.add(j);
			freeSlots.add(l);	
		}

		for (int i=0; i<4; i++)
			slotsUnderHalfFull[i] = -1;
    }


    
    /**
     * Tests if the Array Heap is empty or not. Returns true if
     * the heap is empty, false otherwise.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  true if the heap is empty, false otherwise.
     */
    public boolean isEmpty() {
        return n==0;
    }



    /**
     * Returns the smallest element in the heap. This smallest element
     * is the one with the minimum key value. (Doesn't remove it)
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  heap node with the smallest key, or null if empty.
     */
    public IntPairFibonacciHeap.Node min() {
    	IntPairFibonacciHeap.Node n1 = H1.min();
    	IntPairFibonacciHeap.Node n2 = H2.min();
    	if (n1 == null)
    		return n2;
    	
    	if (n2 == null)
    		return n1;
    	
    	if (n1.key < n2.key) 
    		return n1;
    	else 
    		return n2;
    	
    }

    /**
     * Removes the smallest element from the heap. This will cause
     * the trees in the heap to be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @return  heap node with the smallest key, or null if empty.
     */
    public IntPairFibonacciHeap.Node removeMin() {
    	IntPairFibonacciHeap.Node n1 = H1.min();
    	IntPairFibonacciHeap.Node n2 = H2.min();
    	
    	IntPairFibonacciHeap.Node returnNode;
    	
    	if (n1 == null && n2 == null)
    		return null;
    	else if (n1 == null)
    		returnNode = H2.removeMin();
    	else if (n2 == null || n1.key < n2.key)
    		returnNode = H1.removeMin();
    	else  
    		returnNode = H2.removeMin();

    	if (returnNode == n2) {
    		int lvl = ((IntPairFibonacciHeap.NodeForArrayHeap)returnNode).level;
    		int slot = ((IntPairFibonacciHeap.NodeForArrayHeap)returnNode).slot;
    		
    		
    		cntOnHeap[lvl][slot]--;

    		if (cntOnHeap[lvl][slot] == 0) {
        		//if that was the last representative of one of the slots, then load another block from that slot    			
    			// but first, check if some other slot is < 1/2 full
    			int	smallSlot1 = slotsUnderHalfFull[lvl]; 
    			
    			load(lvl, slot);		
    		
    			if (smallSlot1 != -1 && slotsUnderHalfFull[lvl] != smallSlot1) { // two small slots ... merge 'em
    				int	smallSlot2 = slotsUnderHalfFull[lvl]; 
    			    				
	    			// move point back on these two slots, to include nodes remaining on heap
    				slotPositions[lvl][smallSlot1] -= cntOnHeap[lvl][smallSlot1];
    				slotPositions[lvl][smallSlot2] -= cntOnHeap[lvl][smallSlot2];
    				int remSlots = L[lvl][smallSlot1].length - slotPositions[lvl][smallSlot1] + L[lvl][smallSlot2].length - slotPositions[lvl][smallSlot2] ;

	    			
	    			Node[] nodes = new Node[remSlots];

    				Node tmpN1, tmpN2;
	    			int minSlot;
	    			for (int i=0; i<remSlots; i++) {
	    				
	    				if (slotPositions[lvl][smallSlot1] == L[lvl][smallSlot1].length)
	    					minSlot = smallSlot2;
	    				else if (slotPositions[lvl][smallSlot2] == L[lvl][smallSlot2].length)
	    					minSlot = smallSlot1;
	    				else {
	    					tmpN1 = L[lvl][smallSlot1][slotPositions[lvl][smallSlot1]];
		    				tmpN2 = L[lvl][smallSlot2][slotPositions[lvl][smallSlot2]];
		    				if ( tmpN1.key < tmpN2.key ) {
		    					minSlot = smallSlot1;
		    				} else {
		    					minSlot = smallSlot2;
		    				}
	    				}
    					nodes[i] = L[lvl][minSlot][slotPositions[lvl][minSlot]];
    					slotPositions[lvl][minSlot]++;
	    			}
	    			//put them in one and free the other
	    			freeSlots.get(lvl).add(smallSlot1);
	    			freeSlots.get(lvl).add(smallSlot2);
	    			slotsUnderHalfFull[lvl] = -1;
	    			
	    			//remove the defunct entries from H2 (the appropriate number will get added back in a moment)
	    			H2.deleteLevelAndSlot(lvl, smallSlot1);
	    			H2.deleteLevelAndSlot(lvl, smallSlot2);
	    			
	    			int loc = store(lvl, nodes);
	    			load(lvl, loc);
	    		}
    		}	    		
    	}
    	return returnNode;
    }

    /**
     * Returns the size of the heap which is measured in the
     * number of elements contained in the heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  number of elements in the heap.
     */
    public int size() {
        return n;
    }


    /**
     * Implements a node of the Array Heap. It just holds the key (double) and
     * value (int).
     * 
     * @author  Travis Wheeler
     */
    public static class Node {
        public int value1;
        public int value2;
        public float key;

        /**
         * Two-arg constructor which sets the data and key fields to the
         * passed arguments. 
         * 
         * @param  key    key for this data object
         * @param  value  value for this data object
         */
        public Node(int value1, int value2, float key) {
            this.value1 = value1;
            this.value2 = value2;
            this.key = key;
        }

		public Node() {
			// TODO Auto-generated constructor stub
		}
    }
}
