package com.traviswheeler.libs;


//BinaryHeap class
//
// CONSTRUCTION: empty or with initial array.
//
// ******************PUBLIC OPERATIONS*********************
// void insert( x )       --> Insert x
// void deleteMin( )   --> Remove smallest item
//    note ... it doesn't return is. This is written to require the using application to explicitly grab the values it wants from the minimum cell
// boolean isEmpty( )     --> Return true if empty; else false
// void makeEmpty( )      --> Remove all items
// ******************ERRORS********************************
// Throws UnderflowException for findMin and deleteMin when empty

/**
 * Implements a binary heap.  Based directly off Mark Allen Weiss's implementation
 * @author Travis Wheeler
 */
public class BinaryHeap  {
	
	
	protected static final int DEFAULT_CAPACITY = 1000;
	protected int currentSize;      // Number of elements in heap
    private int nextUnusedPos;
    //Stack<Integer> freeSlots;
    public int[] freeSlots;
    protected int freeSlotPos;
    protected int numFields = 2;
	
    // These three arrays are in lieu of a single array of objects.
    public float[] keys; 
    public int[] val1s;
    public int[] heapArray;
    
    protected int maxCapacity = Integer.MAX_VALUE;
    
    /**
     * Construct the binary heap.
     */
    public BinaryHeap( ) {
    	init (0,DEFAULT_CAPACITY);
    }

    public BinaryHeap(int maxCapacity ) {
    	this.maxCapacity = maxCapacity;
    	init (0,DEFAULT_CAPACITY);
    }

    
    /**
     * Construct the binary heap from an array.
     * @param items the initial items in the binary heap.
     */
    public BinaryHeap( int[] val1s, float[] keys ) {
    	buildAgain (val1s, keys);
    }

    public BinaryHeap( int[] val1s, float[] keys, int maxCapacity ) {
    	this.maxCapacity = maxCapacity; 
    	buildAgain (val1s, keys);
    }
    
    public void buildAgain ( int[] val1s, float[] keys ) {
    	init (keys.length, keys.length);
        
        for( int i = 0; i < keys.length; i++ ) {
            this.keys[ i ] = keys[ i ];
            this.val1s[ i ] = val1s[ i ];
            heapArray[ i+1 ] = i;
        }
        
        buildHeap( );
    }
    
    protected void init (int startCount, int size ) {
    	nextUnusedPos = currentSize = startCount;
    	freeSlotPos = -1;
    	
    	if (freeSlots == null || keys.length < size) { 
    		freeSlots = new int[size];
            keys = new float[size];
            val1s = new int[size];
            heapArray = new int[size + 1];   
    	}
    }
    
    /**
     * Insert into the priority queue.
     * Duplicates are allowed.
     * @param x the item to insert.
     * @return null, signifying that decreaseKey cannot be used.
     * @throws Exception 
     */
    public int insert( int val1, float key) throws Exception {

        if( currentSize  == keys.length )
            doubleArray( );
        
        //stick input values into a slot in the supporting arrays,         
        int freePos;
    	if (freeSlotPos == -1) {
    		freePos = nextUnusedPos++;
    	} else {
    		freePos = freeSlots[freeSlotPos--];
    	}
    	
    	keys[freePos] = key;
        val1s[freePos] = val1;
        
        //then use that slot as the value stored in the PQ 
        int hole = ++currentSize;
        heapArray[0] = freePos;
        
        // Percolate up
        while( key < keys[ heapArray[hole/2] ] ) {
        	heapArray[hole] = heapArray[ hole/2 ];
        	hole /= 2;
        }
        
        heapArray[hole] = freePos;
        return freePos;
        
    }
    

    // Min value is at position 1.    
    /**
     * Remove the smallest item from the priority queue.
     * 
     * assumes you've already gotten at the min entries.
     */
    public void deleteMin( ) {
    	freeSlots[++freeSlotPos] = heapArray[1];
    	heapArray[1] = heapArray[currentSize--];
    	percolateDown( 1 );
    }
    
    /**
     * Remove the item at the specified slot from the priority queue.
     * 
     *//*
    public void delete( int x ) {
    	
    	if (x == 1) {
    		deleteMin();
    	} else {
        	freeSlots[++freeSlotPos] = heapArray[x];
	    	int moved = heapArray[currentSize--];
	    	
			while (keys[moved] < keys[x/2]) { // log number of steps, at most
				heapArray[x] = heapArray[x/2] ; // push parent down, so we can stick the moved one in the parent's slot
				x /= 2;
			}
	    	heapArray[x] = moved;
	    	
	    	percolateDown( x );
    	}
    }    	
    */
    
    /**
     * Establish heap order property from an arbitrary
     * arrangement of items. Runs in linear time.
     */
    protected void buildHeap( ) {
        for( int i = currentSize / 2; i > 0; i-- )
            percolateDown( i );
    }
    
    /**
     * Test if the priority queue is logically empty.
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty( ) {
        return currentSize == 0;
    }
    
    /**
     * Returns size.
     * @return current size.
     */
    public int size( ) {
        return currentSize;
    }
    
    /**
     * Make the priority queue logically empty.
     */
    public void makeEmpty( ) {
        currentSize = 0;
        nextUnusedPos = 0;
    	freeSlotPos = -1;

    }
        
    /**
     * Internal method to percolate down in the heap.
     * @param hole the index at which the percolate begins.
     */
    private void percolateDown( int hole ) {
        int child;
        int pos = heapArray[hole];

        while ( hole * 2 <= currentSize ) {
            child = hole * 2;

            if( child != currentSize && 
            		keys[ heapArray[child+1] ] < keys[ heapArray[child] ])
                child++;
            if( keys[ heapArray[child] ] < keys[ pos ]   ) {
            	heapArray[ hole ] = heapArray[ child ];
            }  else
                break;
            
            hole = child;
        }
        heapArray[ hole ] = pos;
    }
    
    /**
     * Internal method to extend array.
     * @throws Exception 
     */
    protected int doubleArray( ) throws Exception {
    	if (keys.length >= maxCapacity ) {
    		LogWriter.stdErrLogln("request will cause heap to grow in excess of maximum capacity (" + maxCapacity + ")" );
    		throw new Exception("request will cause heap to grow in excess of maximum capacity (" + maxCapacity + ")" );
    	}
    	
        int newLength = keys.length *2;
        
        if (newLength > maxCapacity)
        	newLength = maxCapacity;
        
        int [ ] newFreeSlots = new int [ newLength];
    	float [ ] newKeys = new float [newLength ];
        int [ ] newVals1 = new int [ newLength];
        int [ ] newHeapArray = new int [ newLength + 1];
        int i;
        for (i=0; i<= freeSlotPos; i++) 
        	newFreeSlots[i] = freeSlots[i];

        for( i = 0; i < keys.length; i++ ) { 
            newKeys[ i ] = keys[ i ];
            newVals1[ i ] = val1s[ i ];
            newHeapArray[ i ] = heapArray[ i ];           
        }
 
        newHeapArray[ i ] = heapArray[ i ];
        keys = newKeys;
        val1s = newVals1;
        heapArray = newHeapArray;
        freeSlots = newFreeSlots;
        
        return newLength;
    }
    
    
/*	public long footprint () {
		return (  (currentSize * numFields ) + freeSlots.size() * 4); 
	}
*/

    // Test program
    /*
    public static void main( String [ ] args ) {
        int numItems = 10000;
        BinaryHeap_TwoInts h1 = new BinaryHeap_TwoInts( );
        int [ ] items = new int[ numItems - 1 ];
        float [] f_items = new float [numItems -1];
        
        int i,j;
        
        for( i = 37, j = 0;    i != 0;     i = ( i + 37 ) % numItems, j++ ) {
            h1.insert( i, i, i );
            items[ j ] = i;
            f_items[j] = i;
        }
        
        int minPos;
        for( i = 1; i < numItems; i++ ) {
        	// look at the min entry
        	minPos = h1.heapArray[1];
        	float f = h1.keys[minPos];
        	int x = h1.val1s[minPos];
            if( x != i )
                System.out.println( "Oops 1! " + i );
            h1.deleteMin();
        
        }
        
        ArrayList<Integer> deleted = new ArrayList<Integer>();
        BinaryHeap h2 = new BinaryHeap( items, f_items );
        
        
        deleted.add(h2.val1s[h2.heapArray[8]]);
        h2.delete(8);
        
        deleted.add(h2.val1s[h2.heapArray[11]]);
        h2.delete(11);
        
        deleted.add(h2.val1s[h2.heapArray[5]]);
        h2.delete(5);

        deleted.add(h2.val1s[h2.heapArray[9]]);
        h2.delete(9);
        
        for( i = 1; i < numItems; i++ ) {
        	boolean ok = true;
        	for (int x : deleted) {
        		if (x == i) ok = false;
        	}
        	if (ok) {
	        	minPos = h2.heapArray[1];
	        	float f = h2.keys[minPos];
	        	int x = h2.val1s[minPos];
	            if( x != i )
	                System.out.println( "Oops 2! " + i );
	        	h2.deleteMin();
        	}
        }
                
        System.out.println("ok");
    }
    */
}
