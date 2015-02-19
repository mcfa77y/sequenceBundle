/*
 * $Id: ArrayHeapExtMem.java,v 1.16 2009/09/29 01:03:35 twheeler Exp $
 */
package com.traviswheeler.libs;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

 

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
public class ArrayHeapExtMem {
			
	File tmpDir;
	
//	public static int openFileCnt = 0;
//	public static int heapCnt = 0;
	
	public static boolean chopBottom = true;
	public static boolean trimInactiveFromHeapArray = true;
	
	public int A = -1;
	public int B = -1;
	
	int maxLevels = 4;
	final float c = (float)1/85;  // was 1/7 in the paper, but I'm storing other values with the key, so I've changed the ratio
	int blockSize = 1024 ; //4KB blocks = ~1000 ints
	long mem;
	int cM;
	int numSlots;
	int numNodesPerBlock;
	int numFieldsPerBlock;
	int n=0;
	BinaryHeap_TwoInts H1;
	public BinaryHeap_FourInts H2;

	
	int loopCntr = 0;
	public boolean startCheckingValues = false;
	
	
	RandomAccessFile file;
	
	long[][] slotNodeCnt;
	int[][][] perSlotIntBuffer;
	int[][] slotBuffPos;
	int[][] cntOnHeap;
	long[][] slotPositions;
	ArrayList<LinkedList<Integer>> freeSlots;
	
	ArrayList<SlotPair> slotPairList = new ArrayList<SlotPair>();
	
	byte[] buffB;
	int[] buffI;
	byte[] bigBuffB;
	int[] bigBuffI;
	
	int numFields = 3;
	long[] cntMax = new long[maxLevels];

	BinaryHeap_TwoInts sortingHeap;
	
	public File tempFile;
	private int[] active;

	public ArrayHeapExtMem (File dir, int[] activeIJs) throws Exception {	
		initialize ( dir, activeIJs, (long)Math.pow(2,21));
	}
	
	public ArrayHeapExtMem (File dir, int[] activeIJs, long sizeExp) throws Exception {	
		initialize ( dir, activeIJs, sizeExp);
	}
	
	private void initialize (File dir, int[] activeIJs, long sizeExp) throws Exception {
		active = activeIJs;
		//heapCnt++; 
		tmpDir = dir;
		
		if (sizeExp >  Math.pow(2, 22)  /*4MB*/) {
			blockSize = 2048;
			if (sizeExp >  Math.pow(2, 23)  /*8MB*/) {
				blockSize = 4096;
			}
		}
		
		/* Default mem should probably be >= 2^21 (2MB), because doing so allows me simpler control of the files used to store
		 * the heap.  
		 * 
		 * With these settings (c = 1/85, page size = 4KB (1024 ints) ):
		 * 
		 * - cM = 24,672, so H1 will hold at most 49,344 entries. At 5 ints per entry (4 bytes each) that's 986,880 bytes (~1MB) 
		 *
		 * There are 23 slots per level.  Here's the disk storage per level
		 * 
		 * Level     max # per slot        total #     disk space (3 ints x 4bytes per)
		 * -----     --------------     -----------    -------------------
		 *   1           24,672            567,456        6,809,472  
		 *   2          594,441         13,672,143      164,065,716  
		 *   3       14,322,312        329,413,176    3,952,958,112  
		 *   4      345,078,225      7,936,799,175 ... 8.3 billion entries per heap - should be enough for most purposes 
		 *      
		 * 
		 *   Here's the amount of memory used: 
		 *   
		 *    23 slots * 4 level = 92 total slots 
		 *    Each slot can have up to 341 entries on H2.  
		 *    341 * 92 =  31,372 entries in H2.  7 ints per entry, 4 bytes per:
		 *    31,372 * 7*4 = 878,416 bytes.
		 *    
		 *    Max total in memory = 986,880  + 878,416.   That leaves around 130KB for other bits.  Fine.
		 *      
		 * =================================
		 * 
		 * If mem is (12MB - what I calculate for -Xmx8G), 
		 * 
		 * With these settings (c = 1/85, page size = 16KB (4096 ints) ):
		 * 
		 * - cM = 148,032, so H1 will hold at most 296,064 entries. At 5 ints per entry (4 bytes each) that's 5,921,280 bytes (~6MB) 
		 *
		 * There are 35 slots per level.  Here's the disk storage per level
		 * 
		 * Level     max # per slot        total #     disk space (3 ints x 4bytes per)
		 * -----     --------------     -----------    -------------------
		 *   1          148,032          5,181,120        
		 *   2        5,329,152        186,520,320    
		 *   3      191,849,472      6,714,731,520    
		 *   4    6,906,580,992     .. .....  82,878,971,904 bytes per slot.
		 *   								  over 20 level 4 slots will fit in a 2TB file.
		 *   								That's a total of ~160 billion entries. That's huge.                     
		 * 
		 *   Here's the amount of memory used: 
		 *   
		 *    35 slots * 3 level = 105 + 20 = 125 total slots 
		 *    Each slot can have up to 1365 entries on H2.  
		 *    125 * 1365 =  170,625 entries in H2.  7 ints per entry, 4 bytes per:
		 *    170,625 * 7*4 = 4,777,500 bytes.
		 *    
		 *    Max total in memory = 5,921,280  + 4,777,500.  That's ~11MB, which leaves around 1MB for other bits.  Fine.
		 *     
		 *     
		 */
		//mem = (int) Math.pow(2, sizeExp); 
		mem = sizeExp;
		
		
		cM = (int)(c*mem); 
		numSlots = (int)((cM/blockSize)-1);
		numNodesPerBlock = blockSize/numFields;
		numFieldsPerBlock = numNodesPerBlock*numFields;  // note, this might not be blockSize, because of rounding. 
		
//		LogWriter.stdErrLogln("cM=" + cM + ", numSlots=" + numSlots + ", numNodesPerBlock=" + numNodesPerBlock +
//				", numFieldsPerBlock=" + numFieldsPerBlock);
		
		
//		inserted = new HashSet<Integer>();
//		cntInFile = new int[maxLevels][numSlots];

		prepare();
	}
		
	public void closeAndDeleteFile () throws Exception {
		file.close();
		//LogWriter.stdErrLogln("I should be deleting " + tempFile.getAbsolutePath());
		tempFile.delete();
	}
	

    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new node is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(lg n)</em></p>
     *
     * @param  i int 
     * @param  j int
     * @param  fl float
     * @param  key  float
     */
    public void insert(int i, int j, float key)  throws Exception {

//    	if (key == (float)0.10484 ) {
 //   		cntOnH1++;
 //   		inserted.add(i);
 //   	}

//    	if (blather)
 //   		insertCount++;

    	
    	H1.insert(i, j, key);
    	n++;

    	if (H1.size() == 2* cM) {
    		//pull half the nodes off heap, put into a new Node array, then put into L1 (or push down to L2 or L3)
			
    		//I implement this by just removing the last half of the binary heap. 
    		//This seems worthwhile, as these will generally be lower valued entries, which
    		//are the ones we'd prefer to see dumped to file anyway.
    		

    		int[] is = new int[cM];
    		int[] js = new int[cM];
    		float[] keys = new float[cM];
    		    		
    		if (chopBottom) {
	    		/*new way - pulling from the bottom of the heap*/
	    	

	    		H1.chopBottomK(is, js, keys);
	    		
	    		
	    		//need to sort them. Use Heap sort
	    		if (sortingHeap == null)
	    			sortingHeap = new BinaryHeap_TwoInts(is, js, keys);
	    		else
	    			sortingHeap.buildAgain(is, js, keys);
	    		

	    		int minPos;
	    		int K = is.length;
	    		
	    		for (int ii=0; ii<K; ii++)  {
	            	minPos = sortingHeap.heapArray[1];
	            	is[ii] = sortingHeap.val1s[minPos];
	            	js[ii] = sortingHeap.val2s[minPos];
	            	keys[ii] = sortingHeap.keys[minPos];
	            	
	            	sortingHeap.deleteMin();
	            	
	    		}

	    		sortingHeap = null;
    		} else {
    		
	    		/* old way - pulling from top of heap*/
    			
	    		int minPos;
	    		for (int x=0; x<cM; x++) {    			
		        	minPos = H1.heapArray[1];
	
	    			keys[x] = H1.keys[minPos]; 
	    			is[x] = H1.val1s[minPos];
	    			js[x] = H1.val2s[minPos];
	    			
	    			H1.deleteMin();
	    		}
    		}
    		
    		//if there are no free slots at level 0, figure out which level has a free slot
    		int targetLevel=0;
    		while (freeSlots.get(targetLevel).peek() == null ) { 
    			if (mergeSlots(targetLevel) )
        			break;  // if two half-empty slots can be merged, then there's now room    	
    			targetLevel++;
    		}
    		
    		if (targetLevel == 0) {
    			int newSlot = store(0, is, js, keys, keys.length);        	
            	load(0, newSlot);
    		} else {
    			int slot = mergeLevels (targetLevel,  is, js, keys );
    			
    			load(targetLevel, slot);
    			 
    		}
    		
    	}
   
    }

    
	private long calcPos (int level, int slot) {
		long pos = 0;
		for (int i=0; i<level; i++) {
			pos += numSlots * cntMax[i] * numFields * 4;
		}
		pos += slot * cntMax[level] * numFields * 4;  // this is a conservative placement of the new slot, since cntMax is already conservative.

		return pos;
		//Want each slot to start at a page boundary - this possibly shifts further to the right to do this
		// turns out, this isn't helpful ... disk caching is doing a perfectly good job for us.
		/*
		 * 
		 * Didn't do this:
		 * A) it didn't seem to speed things up. disk caching is working just fine.
		 * B) The way this was written, the placement of one slot failed to account for the shift made in previous slots.
		 *       So ... it was prone to having the start of one slot begin before the end of the previous slot.
		 *       To fix this, need to replace the last line above with a loop involving the code below for each slot.  
		long blockByteSize = blockSize*4;
		long numBlocks = pos/blockByteSize;
		long newPos = blockByteSize*numBlocks;
		if (newPos < pos) 
			newPos += blockByteSize;
		//System.out.println("level : " + level + ", slot : " + slot + ", pos : " + pos + ", numblocks : " + numBlocks + ", newPos : " + newPos);
		
		return newPos;
		*/
	}
	

    
	private int mergeLevels (int targetLevel, int[] is, int[]js, float[] keys) throws Exception {

		if (targetLevel  > maxLevels-1) {
			LogWriter.stdErrLogln("unexpected occurance: External Memory Array heap tried to write to a level > " + maxLevels);
			throw new Exception("unexpected occurance: External Memory Array heap tried to write to a level > " + maxLevels);
		}
		
//		int cntr = 0;
		
		int targetSlot = freeSlots.get(targetLevel).remove();
	
		long outFilePos = calcPos (targetLevel, targetSlot);
				
		
		float[][] tmpKeys = new float[targetLevel][numSlots];
		int[] deletedLevels = new int[targetLevel];
		for (int level=0; level<targetLevel; level++) {
			deletedLevels[level] = level;
			
			for (int slot=0; slot<numSlots; slot++){
				tmpKeys[level][slot] = Float.MIN_VALUE;
				slotPositions[level][slot] -= cntOnHeap[level][slot]; // move back the pointers on the L_i arrays to include positions that are still on the heap 
				slotBuffPos[level][slot] = numFieldsPerBlock;
			}			
		}
		deleteLevels(H2, deletedLevels);
		
		
		long basePos=-1;
		
		int minLevel = -1;
		int minSlot = -1;
		float min;
		float key;
		int inputPos=0;
		long diskPos=-1;
		int buffPos;
		
		
		int outBuffPos = 0;
		
		int nonClearedSlots = targetLevel * numSlots ;
		
		// we are merging all the levels below targetLevel, plus the submitted values,
		// and putting them in an available slot in targetLevel
		
		int newCnt = 0;
		//int skippedCnt = 0;
		//boolean notYetWritten = true;
		boolean stillNeedNode;
		int[] intBuffer;
		
		
		while (nonClearedSlots > 0 || inputPos < keys.length) {
		
			
			if (inputPos < keys.length) {
				minLevel = -1;
				min = keys[inputPos];
			} else {
				min = Float.MAX_VALUE;
			}
		
			
			for (int level=0; level<targetLevel; level++) {
				
				for (int slot=0; slot<numSlots; slot++){
  
					stillNeedNode = true;
					
					
					while (stillNeedNode) {
												
						if (slotPositions[level][slot] == slotNodeCnt[level][slot]) {
							stillNeedNode = false;
							continue;
						}

						if (tmpKeys[level][slot] == Float.MIN_VALUE) { // pointer for this slot didn't move; use this so I don't have to do intBitsToFloat calculation for every entry
							// read next value from file buffer ... may need to refill buffer
							
							
							if (slotBuffPos[level][slot] == numFieldsPerBlock) { // refill buffer
								basePos = calcPos (level, slot);			
								diskPos = (long)numFields * 4 * slotPositions[level][slot]; // 12 bytes for each position: int,int,float
								file.seek(diskPos + basePos);
										
								file.read(buffB);
								
								Arrays.byteToInt(buffB, perSlotIntBuffer[level][slot]);
								
								slotBuffPos[level][slot] = 0;
							}
							intBuffer = perSlotIntBuffer[level][slot];
							buffPos = slotBuffPos[level][slot];
							
														
							try {
								if (trimInactiveFromHeapArray && active != null && (-1 == active[intBuffer[buffPos]] /*i*/ || -1 == active[intBuffer[buffPos+1]]/*j*/ )) {
									slotBuffPos[level][slot] += numFields;
									slotPositions[level][slot]++;
								//	skippedCnt++;
									n--;
									if (slotPositions[level][slot] == slotNodeCnt[level][slot])
										nonClearedSlots--;
	
									continue; // don't bother copying dead nodes to the merged slot
								}
							} catch (Exception e) {
								LogWriter.stdErrLogln("level=" + level + ", slot=" + slot + ", targetlevel=" + targetLevel + ", slotPos=" +
										slotPositions[level][slot] + ", slotNodeCnt=" + slotNodeCnt[level][slot] + ", buffPos=" +
										buffPos + ", a=" + intBuffer[buffPos] + ", b=" + intBuffer[buffPos+1] + ", c=" + intBuffer[buffPos+2] + 
										", active.len=" + active.length);

								LogWriter.stdErrLogln("And for good measure, the next slot position:  buffPos=" +
										(buffPos+3) + ", a=" + intBuffer[buffPos+3] + ", b=" + intBuffer[buffPos+4] + ", c=" + intBuffer[buffPos+5] + 
										", active.len=" + active.length);
								throw e;
							}
							tmpKeys[level][slot] = Float.intBitsToFloat(intBuffer[buffPos+2]);
						}
						
						stillNeedNode = false;
						key = tmpKeys[level][slot];
		    			if ( key < min) {
		    				minLevel = level;
		        			minSlot = slot;
		        			min = key;
		    			}
					}
				}
			}
			
			
			
			if (minLevel == -1) { // read from the arrays pulled from H1
				bigBuffI[outBuffPos++] = is[inputPos];
				bigBuffI[outBuffPos++] = js[inputPos];
				bigBuffI[outBuffPos++] = Float.floatToIntBits(keys[inputPos]);			
				
				inputPos++;
			} else { // read from the slots being merged
				buffPos = slotBuffPos[minLevel][minSlot];
				intBuffer = perSlotIntBuffer[minLevel][minSlot];

				bigBuffI[outBuffPos++] = intBuffer[buffPos];
				bigBuffI[outBuffPos++] = intBuffer[buffPos+1];
				bigBuffI[outBuffPos++] = intBuffer[buffPos+2];	
								
				slotPositions[minLevel][minSlot]++;
				
				if (slotPositions[minLevel][minSlot] == slotNodeCnt[minLevel][minSlot])
					nonClearedSlots--;
				
				slotBuffPos[minLevel][minSlot] += numFields;
				tmpKeys[minLevel][minSlot] = Float.MIN_VALUE; // 
			}
			
			newCnt++;

			
			if (outBuffPos == bigBuffI.length) {
				
				//need to write buffer to file if it's full
				Arrays.intToByte(bigBuffI, bigBuffB);
								file.seek(outFilePos);
				file.write(bigBuffB);
				outFilePos += bigBuffB.length;
				outBuffPos = 0;
				
			}
						
		}
		
		if (outBuffPos > 0) {
			//need to write buffer to file 
			
			
			Arrays.intToByte(bigBuffI, bigBuffB);
			file.seek(outFilePos);
			file.write(bigBuffB, 0, outBuffPos*4);
		}		
		
		//clean up old slots
		for (int level=0; level<targetLevel; level++) {
			LinkedList<Integer> l = freeSlots.get(level);
			l.clear();
	
			for (int slot=0; slot<numSlots; slot++) {
				l.add(slot);
				cntOnHeap[level][slot] = 0;
				slotNodeCnt[level][slot] = 0;
				slotPositions[level][slot] = 0;
			}
		}	

		slotNodeCnt[targetLevel][targetSlot] = newCnt;
		cntOnHeap[targetLevel][targetSlot] = 0;
		slotPositions[targetLevel][targetSlot] = 0;
		
		return targetSlot;
	}

	
	/*
	public void checkValue( long iter) throws Exception{
		
		//6638, 6685, 14186, (2712)

		
		//if (!startCheckingValues) return;
		if ( A!=2 || B!=3) return; 
		
		int[] locBuffI = new int[10*numFieldsPerBlock];
		byte[] locBuffB = new byte[10*numFieldsPerBlock*4];
		
		file.seek(44438940 + 843169792); //= 887608732
//		file.seek(887559592);
		file.read(locBuffB);
		Arrays.byteToInt(locBuffB, locBuffI);

		LogWriter.stdErrLogln( "* " + iter + " : " + locBuffI[2712] + ", " + locBuffI[2712+1] + ", " + locBuffI[2712+2]);

		//should be:  9983, 59130, 1058380941  (14994)
		// but instead it's:  6638, 31719, 1056343180			
	}
	*/
    
	boolean mergeSlots (int lvl) throws Exception {

		

		//   If it's levels 1-3, just store the slots in memory and write 'em all out (as below)
		//    but ... if it's lvl 4, then use a temporary file (or just a temporary space at the end of the file),
		//    read blocks and write them to that file/slot, 
		//    and when it's all done, reset the file handles to use that as one of the merged slots.
		  //  ... I haven't implemented the later part yet		
		if (lvl == maxLevels-1) {
			LogWriter.stdErrLogln("Request was made to merge two slots from level " + maxLevels + ". " +
					"  That's not implemented yet!");
			throw new Exception("Request was made to merge two slots from level " + maxLevels + ". " +
					"  That's not implemented yet!");
		}

		
		//scan through the slots at the given level, and see if two of them are less than half full.
		// The Ferragina paper provides for doing all this at deletion time, which allows a 
		//  guarantee that no more than one slot in a level will be less than half full -  I delay
		//  it until an insert, so I don't bother merging slots that might be emptied before an insert.  This 
		// requires a small amount of overhead to find such slots, but it only happens when a store
		// command is being called anyway, so the overhead is effectively irrelevant.
		long remaining;

		
		// because this implementation allows removal of expired nodes when slots are merged, the result of a merge 
		//might already be small.  No use immediately turning around to merge that one ... so here I merge 
		// multiple slots - as many as will fit in one new slot.  Pick slots to merge greedily
		slotPairList.clear();
		for (int slot=0; slot<numSlots; slot ++) {
			if (cntOnHeap[lvl][slot]>0) {
				remaining = slotNodeCnt[lvl][slot] - slotPositions[lvl][slot] + cntOnHeap[lvl][slot];
				if ( remaining <= cntMax[lvl]/2 ) {
					slotPairList.add(new SlotPair(slot, remaining));
				}
			}
		}
		
		if (slotPairList.size()<2) return false;
		
		//LogWriter.stdErrLogln("merging " + slotPairList.size() + " slots in level " + lvl);
		
		Collections.sort(slotPairList);
		
		int summedSize = (int)(slotPairList.get(0).remaining + slotPairList.get(1).remaining);
		int numSlotsToMerge = 2;
		while (numSlotsToMerge < slotPairList.size() && summedSize + slotPairList.get(numSlotsToMerge).remaining <= cntMax[lvl]) {
			summedSize += slotPairList.get(numSlotsToMerge++).remaining;
		}
		
		
		int nonClearedSlots = numSlotsToMerge ;
		int[] slots = new int[numSlotsToMerge];
		for (int i=0; i<numSlotsToMerge; i++) {
			slots[i] = slotPairList.get(i).slot; 
		}
		 
		float[] tmpKeys = new float[numSlots];
		
		//System.err.print("Merging slots on level " + lvl + ": ");

		
		for (int slot : slots){
			tmpKeys[slot] = Float.MIN_VALUE;
			slotPositions[lvl][slot] -= cntOnHeap[lvl][slot]; // move back the pointers on the L_i arrays to include positions that are still on the heap 
			slotBuffPos[lvl][slot] = numFieldsPerBlock;
			
			//System.err.print("[ " + slot + " @ " + slotPositions[lvl][slot] + " of " + slotNodeCnt[lvl][slot] + "] , ");
			
		}	
		//System.err.println("");
		

		int[] is = new int[summedSize];
		int[] js = new int[summedSize];
		float[] keys = new float[summedSize];
			 

		
		
		int newCnt = 0;
		boolean stillNeedNode;
		int[] intBuffer;
		int minSlot = -1;
		float min;
		float key;
		long basePos, diskPos;
		int buffPos;
		int outBuffPos = 0;

		while (nonClearedSlots > 0 ) {
			min = Float.MAX_VALUE;
		
			for (int slot : slots) {

				stillNeedNode = true;

				while (stillNeedNode) {

					if (slotPositions[lvl][slot] == slotNodeCnt[lvl][slot]) {
						stillNeedNode = false;
						continue;
					}
					
					if (tmpKeys[slot] == Float.MIN_VALUE) { // pointer for this slot moved; use this so I don't have to do intBitsToFloat calculation for every entry
						// read next value from file buffer ... may need to refill buffer
						if (slotBuffPos[lvl][slot] == numFieldsPerBlock) { // refill buffer
							basePos = calcPos (lvl, slot);			
							diskPos = (long)numFields * 4 * slotPositions[lvl][slot]; // 12 bytes for each position: int,int,float
							file.seek(diskPos + basePos);
							file.read(buffB);
							Arrays.byteToInt(buffB, perSlotIntBuffer[lvl][slot]);
							slotBuffPos[lvl][slot] = 0;
						}
						intBuffer = perSlotIntBuffer[lvl][slot];
						buffPos = slotBuffPos[lvl][slot];
						if (trimInactiveFromHeapArray && active != null && (-1 == active[intBuffer[buffPos]] /*i*/ || -1 == active[intBuffer[buffPos+1]]/*j*/ )) {
							slotBuffPos[lvl][slot] += numFields;
							slotPositions[lvl][slot]++;
						//	skippedCnt++;
							n--;
							if (slotPositions[lvl][slot] == slotNodeCnt[lvl][slot])
								nonClearedSlots--;

							continue; // don't bother copying dead nodes to the merged slot
						}
						tmpKeys[slot] = Float.intBitsToFloat(intBuffer[buffPos+2]);
					}
					
					stillNeedNode = false;
					key = tmpKeys[slot];
	    			if ( key < min) {
	        			minSlot = slot;
	        			min = key;
	    			}
				}
			}
			
			if (minSlot<0) { 
				
				for (int slot : slots){
					if ( slotPositions[lvl][slot] != slotNodeCnt[lvl][slot] ) { // otherwise, it's fine that we ended up here
						LogWriter.stdErrLogln("Surprising minSlot == -1.  Is it just 'cause we're at the end of the list, and they're all expired?");
						LogWriter.stdErrLog("\t");
						for (int slot2 : slots){
							LogWriter.stdErrLog("[ " + slot2 + " @ " + slotPositions[lvl][slot2] + " of " + slotNodeCnt[lvl][slot2] + "] , ");
						}	
						LogWriter.stdErrLogln("");

						throw new Exception ("Surprising minSlot == -1.");
					}
				}	
				if (nonClearedSlots > 0) {
					LogWriter.stdErrLogln("Strange.  nonClearedSlots should be 0 but isn't.  Making it 0 now. A=" +
							A + ", B=" + B );
					nonClearedSlots = 0;
				}
				
			} else {
				buffPos = slotBuffPos[lvl][minSlot];
				intBuffer = perSlotIntBuffer[lvl][minSlot];
	
				is[outBuffPos] = intBuffer[buffPos];
				js[outBuffPos] = intBuffer[buffPos+1];
				keys[outBuffPos] = intBuffer[buffPos+2];
				outBuffPos++;
			
				slotPositions[lvl][minSlot]++;
				
				if (slotPositions[lvl][minSlot] == slotNodeCnt[lvl][minSlot])
					nonClearedSlots--;
				
				slotBuffPos[lvl][minSlot] += numFields;
				tmpKeys[minSlot] = Float.MIN_VALUE; // 
				
				newCnt++;
			}

		
		}
		
				
		
		
		//remove the defunct entries from H2 (the appropriate number will get added back in a moment)
		deleteLevelAndSlots(H2, lvl, slots);

		for (int slot : slots) {
			freeSlots.get(lvl).add(slot);
			cntOnHeap[lvl][slot] = 0;
			slotPositions[lvl][slot] = 0;
			slotNodeCnt[lvl][slot] = 0;
		}

		
		int loc = store(lvl, is, js, keys, newCnt);
		load(lvl, loc);
				
		
		return true;

	}
	
	
	boolean mergeTwoSlots (int lvl) throws Exception {

		//scan through the slots at the given level, and see if two of them are less than half full.
		// The Ferragina paper provides for doing all this at deletion time, which allows a 
		//  guarantee that no more than one slot in a level will be less than half full -  I delay
		//  it until an insert, so I don't bother merging slots that might be emptied before an insert.  This 
		// requires a small amount of overhead to find such slots, but it only happens when a store
		// command is being called anyway, so the overhead is effectively irrelevant.
		long remaining;
		int smallSlot1=-1, smallSlot2=-1;
		for (int slot=0; slot<numSlots; slot ++) {
			if (cntOnHeap[lvl][slot]>0) {
				remaining = slotNodeCnt[lvl][slot] - slotPositions[lvl][slot] + cntOnHeap[lvl][slot];
				if ( remaining <= cntMax[lvl]/2 ) {
					if (smallSlot1==-1) {
						smallSlot1 = slot;
					} else {
						smallSlot2 = slot;
						break;
					}
				}
			}
		}
		if (smallSlot2==-1) 
			return false;

		//   If it's levels 1-3, just store the two slots in memory and write 'em all out (as below)
		//    but ... if it's lvl 4, then use a temporary file (or just a temporary space at the end of the file),
		//    read blocks and write them to that file/slot, 
		//    and when it's all done, reset the file handles to use that as one of the merged slots.
		  //  ... I haven't implemented the later part yet		
		if (lvl == maxLevels-1) {
			LogWriter.stdErrLogln("Request was made to merge two slots from level " + maxLevels + ". " +
					"  That's not implemented yet!");
			throw new Exception("Request was made to merge two slots from level " + maxLevels + ". " +
					"  That's not implemented yet!");
		}
		
		
		// move pointer back on these two slots, to include nodes remaining on heap
		slotPositions[lvl][smallSlot1] -= cntOnHeap[lvl][smallSlot1];
		slotPositions[lvl][smallSlot2] -= cntOnHeap[lvl][smallSlot2];
		//TODO: these would have to be LONGs to handle merges of the largest slots ...
		/// but then this method of using int arrays for temporary storage would no longer work
		int remNodes1 = (int)(slotNodeCnt[lvl][smallSlot1] - slotPositions[lvl][smallSlot1]) ;
		int remNodes2 = (int)(slotNodeCnt[lvl][smallSlot2] - slotPositions[lvl][smallSlot2]) ;
		int remNodes = remNodes1 + remNodes2;
		
		
		int[] is = new int[remNodes];
		int[] js = new int[remNodes];
		float[] keys = new float[remNodes];
		
		byte[] bBuff1 = new byte[remNodes1*4*numFields];
		int[] iBuff1 = new int[remNodes1*numFields];

		byte[] bBuff2 = new byte[remNodes2*4*numFields];
		int[] iBuff2 = new int[remNodes2*numFields];


		
		float key1= Float.MIN_VALUE, key2= Float.MIN_VALUE;
		int minSlot=0;
		int buffPos1, buffPos2;
				
		long basePos1 = calcPos (lvl, smallSlot1);
		long basePos2 = calcPos (lvl, smallSlot2);
		
		// fill buffers from two slots
		long slotPos = 4 * numFields * slotPositions[lvl][smallSlot1]; // 12 bytes for each position: int,int,float
		file.seek(slotPos + basePos1);
		file.read(bBuff1);
		Arrays.byteToInt(bBuff1, iBuff1);
		buffPos1 = 0;

		slotPos = 4 * numFields * slotPositions[lvl][smallSlot2]; // 12 bytes for each position: int,int,float
		file.seek(slotPos + basePos2);
		file.read(bBuff2);
		Arrays.byteToInt(bBuff2, iBuff2);
		buffPos2 = 0;
	
		int newCnt = 0;
//		int skippedCnt = 0;
		for (int i=0; i<remNodes; i++) {

			if (slotPositions[lvl][smallSlot1] == slotNodeCnt[lvl][smallSlot1]) {
				minSlot = smallSlot2;
			} else if (slotPositions[lvl][smallSlot2] == slotNodeCnt[lvl][smallSlot2]) {
				minSlot = smallSlot1;
			} else {
				//read next key for each slot from file buffer
				if (key1 == Float.MIN_VALUE) { // use this so I don't have to do intBitsToFloat calculation for every entry, since most pointers didn't move
					if (trimInactiveFromHeapArray && active != null && (-1 == active[iBuff1[buffPos1]] /*i*/ || -1 == active[iBuff1[buffPos1+1]]/*j*/ )) {
						buffPos1 += numFields;
						slotPositions[lvl][smallSlot1]++;
						n--;
						continue; // don't bother copying dead nodes to the merged slot
					}					
					key1 = Float.intBitsToFloat(iBuff1[buffPos1+2]);					
				}
					    					
				if (key2 == Float.MIN_VALUE) { // use this so I don't have to do intBitsToFloat calculation for every entry, since most pointers didn't move
					if (trimInactiveFromHeapArray && active != null && (-1 == active[iBuff2[buffPos2]] /*i*/ || -1 == active[iBuff2[buffPos2+1]]/*j*/ )) {
						buffPos2 += numFields;
						slotPositions[lvl][smallSlot2]++;
						//skippedCnt++;
						continue; // don't bother copying dead nodes to the merged slot
					}					
					
					key2 = Float.intBitsToFloat(iBuff2[buffPos2+2]);
				}
	
				if ( key1 < key2 ) {
					minSlot = smallSlot1;
					keys[i] = key1;
					is[i] = iBuff1[buffPos1];
					js[i] = iBuff1[buffPos1+1];
					
					key1 = Float.MIN_VALUE;
					buffPos1 += numFields;
				} else {
					minSlot = smallSlot2;
					keys[i] = key2;
					is[i] = iBuff2[buffPos2];
					js[i] = iBuff2[buffPos2+1];
					
					key2 = Float.MIN_VALUE;
					buffPos2 += numFields;
				}
			}
			
			slotPositions[lvl][minSlot]++;
			newCnt++;
		}
		
		//remove the defunct entries from H2 (the appropriate number will get added back in a moment)
		int[] slots = {smallSlot1, smallSlot2};
		deleteLevelAndSlots(H2, lvl, slots);

		//put both slots - one will get refilled in a moment
		freeSlots.get(lvl).add(smallSlot1);
		freeSlots.get(lvl).add(smallSlot2);
		cntOnHeap[lvl][smallSlot1] = 0;
		cntOnHeap[lvl][smallSlot2] = 0;
		slotPositions[lvl][smallSlot2] = 0;
		slotPositions[lvl][smallSlot2] = 0;
		slotNodeCnt[lvl][smallSlot1] = 0;
		slotNodeCnt[lvl][smallSlot2] = 0;

		
		int loc = store(lvl, is, js, keys, newCnt);
		load(lvl, loc);
				
		
		return true;

	}

    private void load (int level, int slot) throws Exception{

    	
    	long cntInSlot = slotNodeCnt[level][slot];
		if (slotPositions[level][slot] == cntInSlot) {
			slotNodeCnt[level][slot] = 0;
			slotPositions[level][slot] = 0;
			freeSlots.get(level).add(slot);
			cntOnHeap[level][slot] = 0;

			return;
		}
		
    	

		//long basePos = cntMax[level] * numFields * 4 * slot;
		long basePos = calcPos (level, slot);

		
		long slotPos = 4 * numFields * slotPositions[level][slot]; // 12 bytes for each position: int,int,float

		
		file.seek(slotPos + basePos);
		file.read(buffB);
		Arrays.byteToInt(buffB, buffI);
				
		long endPos = slotPositions[level][slot] + numNodesPerBlock;
		if (endPos > cntInSlot) endPos = cntInSlot;
		cntOnHeap[level][slot] = (int)(endPos - slotPositions[level][slot]);
		
		
		int i=0;
		float key;
		int val1, val2;
		while ( slotPositions[level][slot] < endPos )  {
			val1 = buffI[i++];
			val2 = buffI[i++];
			key = Float.intBitsToFloat(buffI[i++]);

			H2.insert(val1, val2, level, slot, key); // "key" holds the "d" value for the pair. That's what I'll put in "fl" for now 
			slotPositions[level][slot]++;
		}
				
    }

	private int store (int level, int[] is, int[] js, float[] keys, int cnt) throws Exception {
				
		int freeSlot = freeSlots.get(level).remove(); 
				
		int[] bI = new int[cnt * numFields];
		byte[] bB = new byte[cnt * numFields * 4];
		
		//convert to byte array
		int i=0;
		for (int j=0; j<cnt; j++) {
						
			bI[i++] = is[j];
			bI[i++] = js[j];
			bI[i++] = Float.floatToIntBits(keys[j]);
			
		}
		Arrays.intToByte(bI, bB);
		
		long basePos = calcPos (level, freeSlot);

		file.seek(basePos);
		file.write(bB);
		

		slotPositions[level][freeSlot] = 0;
		cntOnHeap[level][freeSlot] = 0;
		
		slotNodeCnt[level][freeSlot] = cnt;
		
		return freeSlot;
	}
	
	
	
	public void prepare() throws Exception{
		
		clear();
		if (H1 == null) H1 = new BinaryHeap_TwoInts(cM*2);
		if (H2 == null) H2 = new BinaryHeap_FourInts(); 	
		if (perSlotIntBuffer== null) perSlotIntBuffer = new int[maxLevels][numSlots][numFieldsPerBlock];
		if (slotNodeCnt == null) slotNodeCnt = new long[maxLevels][numSlots];	
		if (cntOnHeap == null) cntOnHeap = new int[maxLevels][numSlots];
		if (slotPositions == null) slotPositions = new long[maxLevels][numSlots];
		if (slotBuffPos== null) slotBuffPos = new int[maxLevels][numSlots];
		
		
		tempFile = File.createTempFile("arrayHeapExtMem", "" , tmpDir);
		if (file == null) file = new RandomAccessFile(tempFile,"rw");
		
		
		buffI = new int[numFieldsPerBlock];
		buffB = new byte[numFieldsPerBlock*4];
		bigBuffI = new int[10*numFieldsPerBlock];
		bigBuffB = new byte[10*numFieldsPerBlock*4];
		

		cntMax[0] = (long)cM;
		for (int level=1; level<maxLevels; level++)  {
			cntMax[level] = cntMax[level-1] * (numSlots + 1); 
			
		}
//		for (int level=0; level<maxLevels; level++)  {
//			LogWriter.stdErrLogln("cntMax for level " + level + " = " + cntMax[level]);
//		}
	}
	
    /**
     * Removes all elements from this heap, and closes existing file handles
     */
    public void clear() throws Exception{
    	
		n = 0;
		if (H1 != null) H1.makeEmpty();
		if (H2 != null) H2.makeEmpty(); 
			
		if (cntOnHeap != null) {
			for (int i=0; i<maxLevels; i++) {
				for (int j=0; j<numSlots; j++) {
					cntOnHeap[i][j] = 0;
					slotNodeCnt[i][j] = 0;
					slotPositions[i][j] = 0;
				}
			}
		}
		
		if (freeSlots == null)
			freeSlots = new ArrayList<LinkedList<Integer>>(maxLevels);
		else
			freeSlots.clear();
		
		
		for (int i=0; i<maxLevels; i++) {
			LinkedList<Integer> l = new LinkedList<Integer>();
			for (int j=0; j<numSlots; j++) {
				l.add(j);
			}
			freeSlots.add(l);	
		}
		
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

    
    public void describeHeap () {
    	
    	LogWriter.stdErrLogln("H1 has " + H1.currentSize + " nodes");
    	LogWriter.stdErrLogln("H2 has " + H2.currentSize + " nodes");
    	
    	LogWriter.stdErrLogln("Free slots:");
    	for (int i=0; i<3; i++) {
    		for (int x: freeSlots.get(i)) {
    			LogWriter.stdErrLogln(i + ", " + x);
    		}
    	}
    	
		LogWriter.stdErrLogln("slotNodeCnt:");
		for (int i=0; i<maxLevels; i++) 
			for (int j=0; j<numSlots; j++) 
				if (slotNodeCnt[i][j] > 0) 
					LogWriter.stdErrLogln( "\t" +  i + ", " + j + " : " + slotNodeCnt[i][j]); 

		LogWriter.stdErrLogln("cntOnHeap:");
		for (int i=0; i<maxLevels; i++) 
			for (int j=0; j<numSlots; j++) 
				if (cntOnHeap[i][j] > 0) 
					LogWriter.stdErrLogln( "\t" + i + ", " + j + " : " + cntOnHeap[i][j]); 
		
		LogWriter.stdErrLogln("slotPositions:");
		for (int i=0; i<maxLevels; i++) 
			for (int j=0; j<numSlots; j++) 
				if (slotPositions[i][j] > 0) 
					LogWriter.stdErrLogln("\t" +  i + ", " + j + " : " + slotPositions[i][j]); 
		
    }
    

    /**
     * Returns the smallest element in the heap. This smallest element
     * is the one with the minimum key value. (Doesn't remove it)
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  heap node with the smallest key, or null if empty.
     * @throws Exception 
     */
    public BinaryHeap_TwoInts getBinaryHeapWithMin() throws Exception {
    	
    	
    	if (H1.isEmpty() && H2.isEmpty()) {
    		if (n!=0) {
        		LogWriter.stdErrLogln("Surprising: heap should be size " + n + ", but H1 and H2 are empty");    		
        		describeHeap();
        		throw new Exception("H1 and H2 disagree with n");    			
    		}
    	//if (n==0)
    		return null;
    	} else if (H1.isEmpty())
    		return H2;
    	else if (H2.isEmpty())
    		return H1;

    	if (H1.keys[H1.heapArray[1]] <= H2.keys[H2.heapArray[1]]) 
    		return H1;
    	else 
    		return H2;
    	
    }

    /**
     * Removes the smallest element from the heap. This will cause
     * the trees in the heap to be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @return  nothing.  you should already have the values from the min node before deleting 
     */
    public void removeMin()  throws Exception{

    	
//    	if (blather)
//    		deleteCount++;

    	
    	BinaryHeap_TwoInts minH;
    	if (H1.isEmpty() && H2.isEmpty()) 
    		return;
    	else if (H1.isEmpty()) 
    		minH = H2;
    	else if (H2.isEmpty()) 
    		minH = H1;
    	else if (H1.keys[H1.heapArray[1]] <= H2.keys[H2.heapArray[1]]) 
    		minH = H1;
    	else 
    		minH = H2;
    	
    	
    	n--;

    	if (minH == H1) {
        	minH.deleteMin();    	    		
    	} else if (minH == H2) { // may be the last from it's slot - possibly refill

    		int lvl = H2.val3s[H2.heapArray[1]]; 
    		int slot = H2.val4s[H2.heapArray[1]];	
    		minH.deleteMin();
        	// max number of nodes in a slot.
    		
    		cntOnHeap[lvl][slot]--;
    		
    		if (cntOnHeap[lvl][slot] == 0) {
        		//if that was the last representative of one of the slots, then load another block from that slot 
    			load(lvl, slot);  // this will do nothing if the slot is now exhausted		
    			
    		}	   
    		
    		
    	}
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


    
    private void deleteLevelAndSlots (BinaryHeap_FourInts H, int level, int[] slots) {

    	boolean ok = false;
		for (int slot: slots) {
			if (cntOnHeap[level][slot] > 0) {
				cntOnHeap[level][slot] = 0; 
				ok = true;
			}
		}
		
		if (!ok) {
			//nothing to do
			return;
		}
		
		
    	// I can re-build the heap without the to-be-removed entries in linear time,
    	// and since it'll take linear time just to identify to to-be-removed entries
    	// ... that's what I'll do.
    	
    	
    	// get list of nodes without this level and slot
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	int pos;
    	for (int i=1; i<=H.size(); i++){
    		pos = H.heapArray[i];
    		ok = true;
    		if (H.val3s[pos] == level) { // otherwise, it's certainly ok
	    		for (int slot: slots) 
	    			if (H.val4s[pos] == slot)  ok = false;
    		}
    		if (ok) list.add(pos);
    	}
    	int K = list.size();
    	

    	if (K == 0) {
    		H.makeEmpty();
    	} else if (K < H.size()) { // it's possible that there are no entries to delete ... so no reason to rebuild
    	
	    	//get the values for those nodes, so we can create a new heap
	    	int[] v1 = new int[K];
	    	int[] v2 = new int[K];
	    	int[] v3 = new int[K];
	    	int[] v4 = new int[K];
	    	float[] key = new float[K];
	    	for (int i=0; i<K; i++){
	    		pos = list.get(i);
	    		v1[i] = H.val1s[pos];
	    		v2[i] = H.val2s[pos];
	    		v3[i] = H.val3s[pos];
	    		v4[i] = H.val4s[pos];
	    		key[i] = H.keys[pos];
	    	}
	    	
	    	H.buildAgain(v1, v2, v3, v4, key);
	    	
    	}    	
    }

    
    private void deleteLevels (BinaryHeap_FourInts H, int[] levels) {

    	// get list of nodes without this level 
    	
		for (int lvl : levels) {
			for (int slot=0; slot<numSlots; slot ++) {
				cntOnHeap[lvl][slot] = 0;
			}
		}
    	
    	ArrayList<Integer> list = new ArrayList<Integer>();
    	int pos;
    	boolean ok;
    	for (int i=1; i<=H.size(); i++){
    		pos = H.heapArray[i];
    		ok = true;
    		for (int lvl : levels) {
    			if (H.val3s[pos] == lvl)
    				ok = false;
    		}
    		if (ok)
    			list.add(pos);

    	}
    	int K = list.size();
    	
    	if (K == 0) {
    		H.makeEmpty();
    	} else if (K < H.size()) { // it's possible that there are no entries to delete ... so no reason to rebuild
	    	//get the values for those nodes, so we can create a new heap
	    	int[] v1 = new int[K];
	    	int[] v2 = new int[K];
	    	int[] v3 = new int[K];
	    	int[] v4 = new int[K];
	    	float[] key = new float[K];
	    	for (int i=0; i<K; i++){
	    		pos = list.get(i);
	    		v1[i] = H.val1s[pos];
	    		v2[i] = H.val2s[pos];
	    		v3[i] = H.val3s[pos];
	    		v4[i] = H.val4s[pos];
	    		key[i] = H.keys[pos];
	    	}
	    	
	    	H.buildAgain(v1, v2, v3, v4, key);
    	}
    }
    
    /**
     * Implements a node of the Array Heap. It just holds the key (double) and
     * value (int).
     * 
     * @author  Travis Wheeler
     */
    public static class Node {
        public int i;
        public int j;
        public float key;

        /**
         * Three-arg constructor which sets the data and key fields to the
         * passed arguments. 
         * 
         * @param  i  first for this data object
         * @param  j  first for this data object
         * @param  key    key for this data object
         * 
         */
        public Node(int i, int j, float key) {
            this.i = i;
            this.j = j;
            this.key = key;
        }


        
		public Node() {
			// TODO Auto-generated constructor stub
		}
    }
    
    

	public static void main (String[] args) {
		
		
		File njTmpDir = new File( System.getProperty("user.dir")  );
		
		double[] valsd =
		{0.0834,0.01187,0.10279,0.09835,0.09883,0.1001,0.1129,0.09599,0.09468,0.09063,0.09083,0.08194,0.10182,0.09323,0.08796,0.09972,0.09429,0.08069,0.09008,0.10346,0.10594,0.09416,0.06915,0.08638,0.0886,0.09538,0.08546,0.09271,0.0936,0.09941,0.08026,0.0952,0.09446,0.09309,0.09855,0.08682,0.09464,0.0857,0.09154,0.08024,0.08824,0.09442,0.09495,0.08731,0.08428,0.08959,0.07994,0.08034,0.09095,0.09659,0.10066,0.0821,0.09606,0.12346,0.07866,0.07723,0.08642,0.08076,0.07455,0.07961,0.07364,0.08911,0.06946,0.07509,0.087,0.071,0.08653,0.07899,0.09512,0.09456,0.09161,0.08412,0.09649,0.09994,0.10151,0.09751,0.1019,0.10499,0.0873,0.1085,0.10189,0.09987,0.08912,0.10606,0.09552,0.08902,0.09158,0.08046,0.10687,0.0906,0.09937,0.09737,0.09825,0.10234,0.09926,0.09147,0.09071,0.09659,0.09472,0.09327,0.0949,0.09316,0.09393,0.09328,0.01187,0.00848,0.02284,0.03053,0.08393,0.08167,0.10191,0.06527,0.06613,0.06863,0.0652,0.06848,0.06681,0.07466,0.06444,0.05991,0.07031,0.06612,0.06873,0.06598,0.07283,0.06862,0.06437,0.06599,0.07291,0.06355,0.0685,0.06599,0.06593,0.0869,0.07364,0.08118,0.07693,0.06779,0.06605,0.07286,0.05655,0.06352,0.06105,0.09177,0.08312,0.0978,0.07464,0.07977,0.06241,0.07227,0.06255,0.0675,0.07953,0.07806,0.06702,0.08429,0.08567,0.0933,0.087,0.08809,0.07888,0.06351,0.08651,0.08294,0.07282,0.11102,0.08711,0.06192,0.0652,0.06957,0.06763,0.07123,0.0687,0.06773,0.06338,0.06694,0.09871,0.09221,0.08962,0.0879,0.09625,0.09953,0.09532,0.09903,0.0946,0.09406,0.09704,0.09877,0.07257,0.1001,0.09458,0.10141,0.10581,0.09824,0.10668,0.09835,0.10816,0.09667,0.08962,0.08486,0.08572,0.08324,0.08826,0.08801,0.09744,0.09916,0.09996,0.10054,0.10761,0.105,0.10604,0.10161,0.09155,0.10162,0.08549,0.10342,0.09419,0.11429,0.09764,0.09505,0.09394,0.10411,0.08792,0.08887,0.08648,0.07637,0.08544,0.08034,0.12373,0.12963,0.13817,0.13904,0.12648,0.13207,0.10788,0.09605,0.12674,0.08139,0.08326,0.08835,0.10922,0.103,0.12225,0.09854,0.09326,0.11181,0.089,0.12674,0.11631,0.0879,0.09866,0.11393,0.09839,0.09738,0.09922,0.1145,0.09967,0.1032,0.11624,0.10472,0.09999,0.09762,0.1075,0.11558,0.10482,0.10237,0.10776,0.08781,0.08771,0.09751,0.09025,0.09201,0.08731,0.08537,0.0887,0.0844,0.0804,0.08217,0.10216,0.07789,0.08693,0.0833,0.08542,0.09729,0.0937,0.09886,0.092,0.08392,0.09668,0.09444,0.09401,0.08657,0.09659,0.08553,0.0834,0.0846,0.10167,0.10447,0.09838,0.09545,0.09163,0.10475,0.09761,0.09475,0.09769,0.09873,0.09033,0.09202,0.08637,0.0914,0.09146,0.09437,0.08454,0.09009,0.08888,0.0811,0.12672,0.10517,0.11959,0.10941,0.10319,0.10544,0.10717,0.11218,0.12347,0.10637,0.11558,0.1198,0.10133,0.09795,0.10818,0.11657,0.10836,0.11127,0.09611,0.08462,0.1056,0.09537,0.09815,0.10385,0.10246,0.11299,0.11926,0.104,0.10309,0.09494,0.10078,0.09966,0.08215,0.09136,0.10058,0.10078,0.10121,0.09711,0.10072,0.10881,0.09396,0.09925,0.09221,0.0939,0.08804,0.09234,0.09647,0.07966,0.09939,0.09651,0.10765,0.10154,0.07889,0.10452,0.1023,0.10275,0.08817,0.0923,0.09237,0.09481,0.09309,0.08683,0.09903,0.08784,0.09309,0.08876,0.08442,0.097,0.10054,0.09463,0.10038,0.08208,0.10209,0.10181,0.10416,0.08065,0.09581,0.08961,0.08553,0.10272,0.08432,0.08437,0.08946,0.07594,0.07751,0.07935,0.07751,0.07714,0.09572,0.09626,0.08606,0.08031,0.08196,0.09758,0.0754,0.08671,0.10245,0.07644,0.07965,0.09553,0.08362,0.07587,0.08234,0.08611,0.09835,0.09917,0.09264,0.09656,0.0992,0.10802,0.10905,0.09726,0.09911,0.11056,0.08599,0.09095,0.10547,0.08824,0.09831,0.08445,0.09562,0.09378,0.08482,0.08686,0.09192,0.09617,0.09142,0.1024,0.10415,0.10673,0.08337,0.10091,0.08162,0.08284,0.08472,0.1021,0.09073,0.10521,0.09252,0.08545,0.09849,0.0891,0.10849,0.08897,0.08306,0.10775,0.10054,0.09952,0.10851,0.10823,0.10827,0.11254,0.11344,0.10478,0.11348,0.10646,0.12112,0.10183,0.1197,0.12399,0.11847,0.11572,0.14614,0.13348,0.12449,0.12358,0.12792,0.12525,0.12265,0.1305,0.13037,0.12684,0.12374,0.12907,0.12858,0.1285,0.12857,0.15825,0.15937,0.1467,0.128305,0.118165,0.119619995,0.117565,0.12769,0.11013			};

		int reps = 10000;
		if (args.length>0) {
			reps = Integer.parseInt(args[0]);
		}
		System.err.println(reps + " repetitions");
		
		float[] vals = new float[valsd.length * reps];
		
		int i = 0;
		for (int j=0; j<reps; j++) {
			for (double d: valsd) {
				vals[i++] = (float)(d + (.0001 * j));
			}
		}
		ArrayHeapExtMem h = null;
		
		try {
			h = new ArrayHeapExtMem(njTmpDir, null);
			i=0;
			for (float f: vals) {	
//				if (i+1 == 573581)
//					i*=1;
				h.insert(i++,0, f);
			}
			java.util.Arrays.sort(vals);
	
			
			i = 0;
			
		
			
			while (!h.isEmpty()) {
//				if (i == 68320)
//					i *= 1;
				
				BinaryHeap_TwoInts bh = h.getBinaryHeapWithMin();
				int minPos = bh.heapArray[1];
				int val = bh.val1s[minPos];
				float key = bh.keys[minPos];
				
			    if (vals[i] != key) {
			    	System.err.println("ACK");
			    	System.out.println(i + " : " + vals[i] + " =? " + key  + " (" + val + ")");
			    	break;
			    }
			    i++;
			    
				h.removeMin();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("trouble in paradise");
		}
		System.out.println(i);
		
		if (njTmpDir != null) {
			//System.err.println("Should be deleting " + h.tempFile.getAbsolutePath());
			h.tempFile.delete();
		}		

		System.exit(0);
	}
    
    private final class SlotPair implements Comparable<SlotPair>{
        public final int slot;
        public final long remaining;
        public SlotPair(int s, long r){
            slot = s;
            remaining = r;
        }
        public int compareTo(SlotPair p){
               return remaining < p.remaining ? -1 : 1;
        }
    }

    
}
