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
public class BinaryHeap_TwoInts extends BinaryHeap {
	

    // These three arrays are in lieu of a single array of objects.
    public int[] val2s;
    protected int numFields = 3;
    
    /**
     * Construct the binary heap.
     */
    public BinaryHeap_TwoInts( ) {
    	super();
    }
    
    public BinaryHeap_TwoInts(int maxCapacity ) {
    	super(maxCapacity);
    }

    
    /**
     * Construct the binary heap from an array.
     * @param items the initial items in the binary heap.
     */
    public BinaryHeap_TwoInts( int[] val1s, int[] val2s, float[] keys ) {    	
    	buildAgain(val1s,val2s,keys);        
    }
    
    public BinaryHeap_TwoInts( int[] val1s, int[] val2s, float[] keys, int maxCapacity ) {
    	this.maxCapacity = maxCapacity; 
    	buildAgain (val1s, val2s, keys);
    }

    
    public void buildAgain ( int[] val1s, int[] val2s, float[] keys ) {
    	super.buildAgain(val1s,keys);
    	for( int i = 0; i < keys.length; i++ ) {
            this.val2s[ i ] = val2s[ i ];
        }
    }
    
    protected void init (int startCount, int size ) {
    	super.init(startCount, size);
		if (val2s == null || val2s.length < size) 
			val2s = new int[size];
    }
    
    /**
     * Insert into the priority queue.
     * Duplicates are allowed.
     * @param x the item to insert.
     * @return null, signifying that decreaseKey cannot be used.
     * @throws Exception 
     */
    public int insert( int val1, int val2 , float key) throws Exception {
    	int pos = super.insert(val1, key);
    	val2s[pos] = val2;
    	
    	return pos;
    }
    
    
    /**
     * Internal method to extend array.
     * @throws Exception 
     */
    protected int doubleArray( ) throws Exception {
    	int newLength = super.doubleArray();
        int [ ] newVal2s = new int [ newLength ]; 
        for( int i = 0; i < val2s.length; i++ ) { 
        	newVal2s[ i ] = val2s[ i ];
        }
        val2s = newVal2s;
        return newLength;
    }
    
    
    public void chopBottomK (int[] val1s, int[] val2s, float[] keys ) {
    	int k = keys.length;
    	int heapIndex = currentSize - k + 1;
    	int pos;
    	for (int i=0; i<k; i++) {
    		pos = heapArray[heapIndex];
    		val1s[i] = this.val1s[pos];
    		val2s[i] = this.val2s[pos];
    		keys[i] = this.keys[pos];    		
    		heapIndex++; 
        	freeSlots[++freeSlotPos] = pos;
    	}
    	
    	currentSize -= k;
    	
    }

    /*
    public static void main( String [ ] args ) throws Exception {
        BinaryHeap_TwoInts h1 = new BinaryHeap_TwoInts( );
        float h_d ;
        int h_i, h_j, minPos;
        
        h1.insert( 4, 174, (float)0.51613); //
        h1.insert( 4, 175, (float)0.51613); //
        h1.insert( 4, 176, (float)0.51613); //
        h1.insert( 4, 177, (float)0.51613); //
        h1.insert( 4, 178, (float)0.51613); //
        h1.insert( 4, 179, (float)0.51613); //
        h1.insert( 4, 200, (float)0.4898); //
        h1.insert( 174, 175, (float)0.0); //
        h1.insert( 174, 176, (float)0.0); //
        h1.insert( 174, 177, (float)0.0); //
        h1.insert( 174, 178, (float)0.0); //
        h1.insert( 174, 179, (float)0.0); //
        h1.insert( 174, 200, (float)0.64516); //
        h1.insert( 175, 176, (float)0.0); //
        h1.insert( 175, 177, (float)0.0); //
        h1.insert( 175, 178, (float)0.0); //
        h1.insert( 175, 179, (float)0.0); //
        h1.insert( 175, 200, (float)0.64516); //
        h1.insert( 176, 177, (float)0.0); //
        h1.insert( 176, 178, (float)0.0); //
        h1.insert( 176, 179, (float)0.0); //
        h1.insert( 176, 200, (float)0.64516); //
        h1.insert( 177, 178, (float)0.0); //
        h1.insert( 177, 179, (float)0.0); //
        h1.insert( 177, 200, (float)0.64516); //
        h1.insert( 178, 179, (float)0.0); //
        h1.insert( 178, 200, (float)0.64516); //
        h1.insert( 179, 200, (float)0.64516); //
        h1.insert( 4, 1147, (float)0.61492);
        h1.insert( 1147, 174, (float)0.40625);
        h1.insert( 1147, 175, (float)0.40625);
        h1.insert( 1147, 176, (float)0.40625);
        h1.insert( 1147, 177, (float)0.40625);
        h1.insert( 1147, 178, (float)0.40625);
        h1.insert( 1147, 179, (float)0.40625);
        h1.insert( 1147, 200, (float)0.534275);

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();

        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();
        minPos = h1.heapArray[1];
        h_d = h1.keys[minPos];
        h_i = h1.val1s[minPos];
        h_j = h1.val2s[minPos];
        h1.deleteMin();


        h1.insert( 4, 1148, (float)0.6603487);
        h1.insert( 1148, 174, (float)0.5578663);
        h1.insert( 1148, 175, (float)0.5578663);
        h1.insert( 1148, 176, (float)0.5578663);
        h1.insert( 1148, 177, (float)0.5578663);
        h1.insert( 1148, 178, (float)0.5578663);
        h1.insert( 1148, 179, (float)0.5578663);
        h1.insert( 1148, 200, (float)0.5335037);
        h1.insert( 1148, 1147, (float)0.4489975);
        h1.insert( 4, 1149, (float)0.51613);
        h1.insert( 1149, 176, (float)0.0);
        h1.insert( 1149, 177, (float)0.0);
        h1.insert( 1149, 178, (float)0.0);
        h1.insert( 1149, 179, (float)0.0);
        h1.insert( 1149, 200, (float)0.64516);
        h1.insert( 1147, 1149, (float)0.40625);
        h1.insert( 1148, 1149, (float)0.5578663);

        System.out.println("ok");
    }
*/
    

    // Test program
    /*
    public static void main( String [ ] args ) {
        int numItems = 1000000;
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
        	float f = h1.Ds[minPos];
        	int x = h1.Is[minPos];
        	int y = h1.Js[minPos];
            if( x != i )
                System.out.println( "Oops 1! " + i );
            h1.deleteMin();
        
        }
        
        BinaryHeap_TwoInts h2 = new BinaryHeap_TwoInts( f_items, items, items );
        for( i = 1; i < numItems; i++ ) {
        	minPos = h2.heapArray[1];
        	float f = h2.Ds[minPos];
        	int x = h2.Is[minPos];
        	int y = h2.Js[minPos];
            if( x != i )
                System.out.println( "Oops 2! " + i );
        	h2.deleteMin();
        }
        
        System.out.println("ok");
    }
    */
}
