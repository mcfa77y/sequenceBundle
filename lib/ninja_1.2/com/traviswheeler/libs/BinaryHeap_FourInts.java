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
public class BinaryHeap_FourInts extends BinaryHeap_TwoInts {
	

    // These three arrays are in lieu of a single array of objects.
    public int[] val3s;
    public int[] val4s;
    protected int numFields = 5;
    
    /**
     * Construct the binary heap.
     */
    public BinaryHeap_FourInts( ) {
    	super();
    }
    
    public BinaryHeap_FourInts(int maxCapacity ) {
    	super(maxCapacity);
    }
    
    /**
     * Construct the binary heap from an array.
     * @param items the initial items in the binary heap.
     */
    public BinaryHeap_FourInts( int[] val1s, int[] val2s, int[] val3s, int[] val4s, float[] keys ) {
    	 buildAgain ( val1s, val2s, val3s, val4s, keys );
    }
    
    public BinaryHeap_FourInts( int[] val1s, int[] val2s, int[] val3s, int[] val4s, float[] keys, int maxCapacity ) {
    	this.maxCapacity = maxCapacity; 
    	buildAgain ( val1s, val2s, val3s, val4s, keys );
    }

    
    public void buildAgain ( int[] val1s, int[] val2s, int[] val3s, int[] val4s, float[] keys ) {
    	super.buildAgain(val1s,val2s,keys);
    	for( int i = 0; i < keys.length; i++ ) {
            this.val3s[ i ] = val3s[ i ];
            this.val4s[ i ] = val4s[ i ];
        }
    }

    
    protected void init (int startCount, int size ) {
    	super.init(startCount, size);
    	if (val3s == null || val3s.length < size) {
    		val3s = new int[size];
    		val4s = new int[size];
    	}
    }
    
    /**
     * Insert into the priority queue.
     * Duplicates are allowed.
     * @param x the item to insert.
     * @return null, signifying that decreaseKey cannot be used.
     * @throws Exception 
     */
    public int insert( int val1, int val2 , int val3, int val4, float key) throws Exception {
    	int pos = super.insert(val1, val2, key);
    	val3s[pos] = val3;
    	val4s[pos] = val4;
    	
    	return pos;
    }
    
    
    /**
     * Internal method to extend array.
     * @throws Exception 
     */
    protected int doubleArray( ) throws Exception {
    	int newLength = super.doubleArray();
        int [ ] newVal3s = new int [ newLength];
        int [ ] newVal4s = new int [ newLength];
        for( int i = 0; i < val3s.length; i++ ) { 
        	newVal3s[ i ] = val3s[ i ];
        	newVal4s[ i ] = val4s[ i ];
        }
        val3s = newVal3s;
        val4s = newVal4s;
        return newLength;
    }
    
    /*
    public static void main( String [ ] args ) {
        BinaryHeap_FourInts h1 = new BinaryHeap_FourInts( );
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
        int numItems = 100000;
        BinaryHeap_FourInts h1 = new BinaryHeap_FourInts( );
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
        	int a = h1.val1s[minPos];
        	int b = h1.val2s[minPos];
        	int c = h1.val3s[minPos];
        	int d = h1.val4s[minPos];
            if( a != i )
                System.out.println( "Oops 1! " + i );
            h1.deleteMin();
        
        }
        
        BinaryHeap_FourInts h2 = new BinaryHeap_FourInts( items, items, items, items, f_items );
        for( i = 1; i < numItems; i++ ) {
        	minPos = h2.heapArray[1];
        	float f = h2.keys[minPos];
        	int a = h1.val1s[minPos];
        	int b = h1.val2s[minPos];
        	int c = h1.val3s[minPos];
        	int d = h1.val4s[minPos];
            if( a != i )
                System.out.println( "Oops 2! " + i );
        	h2.deleteMin();
        }
        
        System.out.println("ok");
    }
    */
}
