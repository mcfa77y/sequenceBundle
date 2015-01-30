/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is GraphMaker. The Initial Developer of the Original
 * Software is Nathan L. Fiedler. Portions created by Nathan L. Fiedler
 * are Copyright (C) 1999-2008. All Rights Reserved.
 *  
 * Contributor(s): Nathan L. Fiedler.
 *
 * *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *  *  *  *  *  *  *
 *  
 * This file has been placed in the com.traviswheeler.lib library, and modified 
 * as documented below. It is thus released under the original CDDL license, but it
 * should be noted that this applies only to this class/file. The rest of the work
 * in this library, and in applications using this library, are considered to 
 * fall under the description of "Larger Work", as descirbed in the CDDL license (3.6) 
 * 
 *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *  *  *  *  *  *  *

 * $Id: IntPairFibonacciHeap.java,v 1.7 2009/02/05 00:10:00 twheeler Exp $
 */
package com.bluemarsh.graphmaker.core.util;

import java.util.ArrayList;

import com.bluemarsh.graphmaker.core.util.FibonacciHeap.Node;

/**
 * This class implements a Fibonacci heap data structure. Much of the
 * code in this class is based on the algorithms in Chapter 21 of the
 * "Introduction to Algorithms" by Cormen, Leiserson, Rivest, and Stein.
 * The amortized running time of most of these methods is O(1), making
 * it a very fast data structure. Several have an actual running time
 * of O(1). removeMin() and delete() have O(log n) amortized running
 * times because they do the heap consolidation.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a set concurrently, and at least one of the
 * threads modifies the set, it <em>must</em> be synchronized externally.
 * This is typically accomplished by synchronizing on some object that
 * naturally encapsulates the set.</p>
 *
 * Modified by Travis Wheeler to replace the single "value" with a pair 
 * of ints. This is done to speed up the implementation of the 
 * graph-merging algorithm it'll be used in.
 * 
 * Also modified by Travis Wheeler to include a new type of node that holds two
 * additional ints, as required for the "external memory array heap" (which 
 * I regard as "Larger Work", and thus do not release under the CDDL).
 *
 * @author  Nathan Fiedler, Travis Wheeler
 * 
 * 
 */
public class IntPairFibonacciHeap {
    /** Points to the minimum node in the heap. */
    private Node min;
    /** Number of nodes in the heap. If the type is ever widened,
     * (e.g. changed to long) then recalcuate the maximum degree
     * value used in the consolidate() method. */
    private int n;

    /**
     * Removes all elements from this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     */
    public void clear() {
        min = null;
        n = 0;
    }

    /**
     * Consolidates the trees in the heap by joining trees of equal
     * degree until there are no more trees of equal degree in the
     * root list.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     */
    private void consolidate() {
        // The magic 45 comes from log base phi of Integer.MAX_VALUE,
        // which is the most elements we will ever hold, and log base
        // phi represents the largest degree of any root list node.
        Node[] A = new Node[45];

        // For each root list node look for others of the same degree.
        Node start = min;
        Node w = min;
        do {
            Node x = w;
            // Because x might be moved, save its sibling now.
            Node nextW = w.right;
            int d = x.degree;
            while (A[d] != null) {
                // Make one of the nodes a child of the other.
                Node y = A[d];
                if (x.key > y.key) {
                    Node temp = y;
                    y = x;
                    x = temp;
                }
                if (y == start) {
                    // Because removeMin() arbitrarily assigned the min
                    // reference, we have to ensure we do not miss the
                    // end of the root node list.
                    start = start.right;
                }
                if (y == nextW) {
                    // If we wrapped around we need to check for this case.
                    nextW = nextW.right;
                }
                // Node y disappears from root list.
                y.link(x);
                // We've handled this degree, go to next one.
                A[d] = null;
                d++;
            }
            // Save this node for later when we might encounter another
            // of the same degree.
            A[d] = x;
            // Move forward through list.
            w = nextW;
        } while (w != start);

        // The node considered to be min may have been changed above.
        min = start;
        // Find the minimum key again.
        for (Node a : A) {
            if (a != null && a.key < min.key) {
                min = a;
            }
        }
    }

    /**
     * Decreases the key value for a heap node, given the new value
     * to take on. The structure of the heap may be changed, but will
     * not be consolidated.
     *
     * <p><em>Running time: O(1) amortized</em></p>
     *
     * @param  x  node to decrease the key of
     * @param  k  new key value for node x
     * @exception  IllegalArgumentException
     *             if k is larger than x.key value.
     */
    public void decreaseKey(Node x, float k) {
        decreaseKey(x, k, false);
    }

    /**
     * Decrease the key value of a node, or simply bubble it up to the
     * top of the heap in preparation for a delete operation.
     *
     * @param  x       node to decrease the key of.
     * @param  k       new key value for node x.
     * @param  delete  true if deleting node (in which case, k is ignored).
     */
    private void decreaseKey(Node x, float k, boolean delete) {
        if (!delete && k > x.key) {
            throw new IllegalArgumentException("cannot increase key value");
        }
        x.key = k;
        Node y = x.parent;
        if (y != null && (delete || k < y.key)) {
            y.cut(x, min);
            y.cascadingCut(min);
        }
        if (delete || k < min.key) {
            min = x;
        }
    }

    /**
     * Deletes a node from the heap given the reference to the node.
     * The trees in the heap will be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @param  x  node to remove from heap.
     */
    public void delete(Node x) {
        // make x as small as possible
        decreaseKey(x, Float.MIN_VALUE, true);
        // remove the smallest, which decreases n also
        removeMin();
    }

    /**
     * Deletes all nodeForHeapArrays with the given level .
     *
     * <p><em>Running time: O(R log n) amortized, if there are R nodes with the level</em></p>
     *
     * @param  x  node to remove from heap.
     */
    public void deleteLevel(int level) {
    	if (min==null)
    		return;
    	ArrayList<Node> list = new ArrayList<Node>();
    	((NodeForArrayHeap)min).trackIfLevel(level, list);
    	for (Node n : list) 
    		delete(n);    		    	
    }

    
    /**
     * Deletes all nodeForHeapArrays with the given level and slot.
     *
     * <p><em>Running time: O(R log n) amortized, if there are R nodes with the level</em></p>
     *
     * @param  x  node to remove from heap.
     */
    public void deleteLevelAndSlot(int level, int slot) {
    	if (min==null)
    		return;    	    	
    	ArrayList<Node> list = new ArrayList<Node>();
    	((NodeForArrayHeap)min).trackIfLevelAndSlot(level, slot, list);
    	for (Node n : list) {
    		delete(n);
    	}
	}
    
    
    /**
     * Tests if the Fibonacci heap is empty or not. Returns true if
     * the heap is empty, false otherwise.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  true if the heap is empty, false otherwise.
     */
    public boolean isEmpty() {
        return min == null;
    }

    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new node is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  node    already-formed node to be inserted into heap.
     */
    public Node insert(Node node) {
    	   node.parent = node.child = null;
           node.right = node.left = node;

    	   node.degree = 0;
    	   node.mark = false;
    	   return finishInsert(node);
    }

    
    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new node is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  i    first int associated with node inserted into heap.
     * @param  j    second int associated with node inserted into heap.
     * @param  fl   misc float associated with node inserted into heap.
     * @param  key  key value associated with data object.
     * @return newly created heap node.
     */
    public Node insert(int i, int j, float key, int level, int slot) {
        Node node = new NodeForArrayHeap(i, j, key, level, slot);
        return finishInsert(node);
    }
    
    /**
     * Inserts a new data element into the heap. No heap consolidation
     * is performed at this time, the new node is simply inserted into
     * the root list of this heap.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  i    first int associated with node inserted into heap.
     * @param  j    second int associated with node inserted into heap.
     * @param  key  key value associated with data object.
     * @return newly created heap node.
     */
    public Node insert(int i, int j, float key) {
        Node node = new Node(i, j, key);
        return finishInsert(node);
    }

    
    private Node finishInsert (Node node) {
        // concatenate node into min list
        if (min != null) {
            node.right = min;
            node.left = min.left;
            min.left = node;
            node.left.right = node;
            if (node.key < min.key) {
                min = node;
            }
        } else {
            min = node;
        }
        n++;
        return node;
    }

    /**
     * Returns the smallest element in the heap. This smallest element
     * is the one with the minimum key value.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @return  heap node with the smallest key, or null if empty.
     */
    public Node min() {
        return min;
    }

    /**
     * Removes the smallest element from the heap. This will cause
     * the trees in the heap to be consolidated, if necessary.
     *
     * <p><em>Running time: O(log n) amortized</em></p>
     *
     * @return  Node with the smallest key.
     */
    public Node removeMin() {
    	
        Node z = min;
        if (z == null) {
            return null;
        }
        if (z.child != null) {
            z.child.parent = null;
            // for each child of z do...
            for (Node x = z.child.right; x != z.child; x = x.right) {
                // set parent[x] to null
                x.parent = null;
            }
            // merge the children into root list
            Node minleft = min.left;
            Node zchildleft = z.child.left;
            min.left = zchildleft;
            zchildleft.right = min;
            z.child.left = minleft;
            minleft.right = z.child;
        }
        // remove z from root list of heap
        z.left.right = z.right;
        z.right.left = z.left;
        if (z == z.right) {
            min = null;
        } else {
            min = z.right;
            consolidate();
        }
        // decrement size of heap
        n--;
        
        //sanity test
/*
        ArrayList<Node> list = new ArrayList<Node>();
        getNodeList (list);
        if (list.size() != n) {
        	System.err.println("heap just broke");
        	System.exit(1);
        }
  */      	
        
        return z;  
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
     * Joins two Fibonacci heaps into a new one. No heap consolidation is
     * performed at this time. The two root lists are simply joined together.
     *
     * <p><em>Running time: O(1)</em></p>
     *
     * @param  H1  first heap
     * @param  H2  second heap
     * @return  new heap containing H1 and H2
     */
    public static IntPairFibonacciHeap union(IntPairFibonacciHeap H1, IntPairFibonacciHeap H2) {
        IntPairFibonacciHeap H = new IntPairFibonacciHeap();
        if (H1 != null && H2 != null) {
            H.min = H1.min;
            if (H.min != null) {
                if (H2.min != null) {
                    H.min.right.left = H2.min.left;
                    H2.min.left.right = H.min.right;
                    H.min.right = H2.min;
                    H2.min.left = H.min;
                    if (H2.min.key < H1.min.key) {
                        H.min = H2.min;
                    }
                }
            } else {
                H.min = H2.min;
            }
            H.n = H1.n + H2.n;
        }
        return H;
    }


    public void buildForest (StringBuffer sb) {
    	if (min != null)
    	min.buildSubtree(sb);
    }

    public void getNodeList (ArrayList<Node> list) {
    	if (min != null) 
    		min.addSubtreeToList(list); 
    	
//    	for (Node n : list)
//   		n.seen = false;
    }
    
    /**
     * Implements a node of the Fibonacci heap. It holds the information
     * necessary for maintaining the structure of the heap. It acts as
     * an opaque handle for the data element, and serves as the key to
     * retrieving the data from the heap.
     *
     * TW: I've modified this to store two ints instead of an object, for 
     * faster access to what I need in NINJA
     *
     * @author  Nathan Fiedler
     */
    public static class Node {
        /** Data object for this node, holds the key value. */
        //private Object data;  ... replaced with the pair of vars below
    	public int i;
    	public int j;
    	
    	
        /** Key value for this node. */
        public float key;  // changed from double in standard implementation.
        /** Parent node. */
        private Node parent;
        /** First child node. */
        protected Node child;
        /** Right sibling node. */
        protected Node right;
        /** Left sibling node. */
        protected Node left;
        /** Number of children of this node. */
        private int degree;
        /** True if this node has had a child removed since this node was
         * added to its parent. */
        private boolean mark;
        
//        public boolean seen = false;

        /**
         * Three-arg constructor which sets the data and key fields to the
         * passed arguments. It also initializes the right and left pointers,
         * making this a circular doubly-linked list.
         *
         * @param  i     first int value to associate with this node
         * @param  j     second int value to associate with this node
         * @param  fl     misc float value to associate with this node
         * @param  key   key value for this data object (float)
         */
        public Node(int i, int j, float key) {
            this.i = i;
            this.j = j;
            this.key = key;
            right = this;
            left = this;
        }


        
        /**
         * Performs a cascading cut operation. Cuts this from its parent
         * and then does the same for its parent, and so on up the tree.
         *
         * <p><em>Running time: O(log n)</em></p>
         *
         * @param  min  the minimum heap node, to which nodes will be added.
         */
        public void cascadingCut(Node min) {
            Node z = parent;
            // if there's a parent...
            if (z != null) {
                if (mark) {
                    // it's marked, cut it from parent
                    z.cut(this, min);
                    // cut its parent as well
                    z.cascadingCut(min);
                } else {
                    // if y is unmarked, set it marked
                    mark = true;
                }
            }
        }

        /**
         * The reverse of the link operation: removes x from the child
         * list of this node.
         *
         * <p><em>Running time: O(1)</em></p>
         *
         * @param  x    child to be removed from this node's child list
         * @param  min  the minimum heap node, to which x is added.
         */
        public void cut(Node x, Node min) {
            // remove x from childlist and decrement degree
            x.left.right = x.right;
            x.right.left = x.left;
            degree--;
            // reset child if necessary
            if (degree == 0) {
                child = null;
            } else if (child == x) {
                child = x.right;
            }
            // add x to root list of heap
            x.right = min;
            x.left = min.left;
            min.left = x;
            x.left.right = x;
            // set parent[x] to nil
            x.parent = null;
            // set mark[x] to false
            x.mark = false;
        }

        /**
         * Make this node a child of the given parent node. All linkages
         * are updated, the degree of the parent is incremented, and
         * mark is set to false.
         *
         * @param  parent  the new parent node.
         */
        public void link(Node parent) {
            // Note: putting this code here in Node makes it 7x faster
            // because it doesn't have to use generated accessor methods,
            // which add a lot of time when called millions of times.
            // remove this from its circular list
            left.right = right;
            right.left = left;
            // make this a child of x
            this.parent = parent;
            if (parent.child == null) {
                parent.child = this;
                right = this;
                left = this;
            } else {
                left = parent.child;
                right = parent.child.right;
                parent.child.right = this;
                right.left = this;
            }
            // increase degree[x]
            parent.degree++;
            // set mark false
            mark = false;
        }

        public void addSubtreeToList (ArrayList<Node> list) {
            Node w = this;
            
            
            do {
//                if (w.seen) {
//               	w.seen = true;
//               }
                	
            	list.add(w);
//           	w.seen = true;
            		
            	if (w.child != null)
            		w.child.addSubtreeToList(list);
                w = w.right;
            } while (w != this);        	

        }
        
        public void buildSubtree (StringBuffer sb) {
            Node w = this;
            do {
            	sb.append( w.key );
            	if (w.parent != null)
            		sb.append ("  (" +w.parent.key + ")\n");
            	else
            		sb.append("\n");
            		
            	if (w.child != null)
            		w.child.buildSubtree(sb);
                w = w.right;
            } while (w != this);        	
        }

        /*    
        public void printSubtree (StringBuffer sb) {
            Node w = this;
            do {
            	sb.append(w.i + "," + w.j + ": (" + w.key + ")");
            	if (w.parent != null)
            		sb.append ("  (" +w.parent.i + "," + w.parent.j + ")\n");
            	else
            		sb.append("\n");
            		
            	if (w.child != null)
            		w.child.printSubtree(sb);
                w = w.right;
            } while (w != this);        	
        }
        */
    }
    

    /**
     * Implements a node of the Fibonacci heap. It holds the information
     * necessary for maintaining the structure of the heap. It acts as
     * an opaque handle for the data element, and serves as the key to
     * retrieving the data from the heap.
     *
     * TW: I've modified this to store two ints instead of an object, for faster 
     * access to what I need in NINJA.  It also stores two more ints (level and slot)
     * required for the external-memory ArrayHeap application.
     *
     * @author  Nathan Fiedler, Travis Wheeler
     */
    public static class NodeForArrayHeap extends Node { 
    	public int level;
    	public int slot;
    	
        /**
         * Four-arg constructor which sets the data and key fields to the
         * passed arguments. It also initializes the right and left pointers,
         * making this a circular doubly-linked list.
         *
         * @param  i     first int value to associate with this node
         * @param  j     second int value to associate with this node
         * @param  key   key value for this data object (float)
         * @param  level level to associate with this node
         * @param  slot  slot to associate with this node
         */
        public NodeForArrayHeap(int i, int j, float key, int level, int slot) {
        	super (i, j, key);
        	this.level = level;
        	this.slot = slot;
        }
 
        /**
         * Used to build a list of nodes that belong to a particular level,
         * so they can all be deleted en mass.
         * 
         * @param l
         * @param x
         */
        public void trackIfLevel(int l, ArrayList<Node> x) {
        	NodeForArrayHeap w = this;
            do {
            	if (w.level == l)
            		x.add(w);
            		
            	if (w.child != null)
            		((NodeForArrayHeap)w.child).trackIfLevel(l, x);
            	
                w = (NodeForArrayHeap)w.right;
                
            } while (w != this);        	

        }

        /**
         * Used to build a list of nodes that belong to a particular level,
         * so they can all be deleted en mass.
         * 
         * @param l
         * @param x
         */
        public void trackIfLevelAndSlot(int l, int s, ArrayList<Node> x) {
        	NodeForArrayHeap w = this;
            do {
            	if (w.level == l && w.slot == s)
            		x.add(w);
            		
            	if (w.child != null)
            		((NodeForArrayHeap)w.child).trackIfLevelAndSlot(l, s, x);
            	
                w = (NodeForArrayHeap)w.right;
                
            } while (w != this);        	


        }
        
    }
    
    
    /**
     * Implements a node of the Fibonacci heap. It holds the information
     * necessary for maintaining the structure of the heap. It acts as
     * an opaque handle for the data element, and serves as the key to
     * retrieving the data from the heap.
     *
     * TW: I've modified this to store two ints instead of an object, for faster 
     * access to what I need in NINJA.  It also stores an extra float (the d value)
     * required for the use on the candidate list.
     *
     * @author  Nathan Fiedler, Travis Wheeler
     */
/*    public static class NodeForCandidateList extends Node { 
    	public float d;
  */  	
        /**
         * Four-arg constructor which sets the data and key fields to the
         * passed arguments. It also initializes the right and left pointers,
         * making this a circular doubly-linked list.
         *
         * @param  i     first int value to associate with this node
         * @param  j     second int value to associate with this node
         * @param  key   key value for this data object (float)
         * @param  d   the d value for this data object (float)
         */
    /*
         public NodeForCandidateList(int i, int j, float key, float d) {
        	super (i,i,key);
        	this.d = d;
        }
    }
     */
}
