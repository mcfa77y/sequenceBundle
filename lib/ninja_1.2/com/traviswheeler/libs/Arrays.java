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
 * are Copyright (C) 2004-2007. All Rights Reserved.
 *
 * Contributor(s): Nathan L. Fiedler.
 *
 * *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *  *  *  *  *  *  *
 *  
 * This file has been placed in the com.traviswheeler.lib library with no 
 * modification from it's original form in Fiedler's graphmaker package. 
 * All my uses of this are believed to fall under the description of
 * "Larger Work", as descirbed in the CDDL license (3.6) 
 * 
 *  *  *  *  *  *  *  *  *  *  *  *  *  *  * *  *  *  *  *  *  *

 *
 * $Id: Arrays.java,v 1.5 2009/07/08 05:34:55 twheeler Exp $
 */

package com.traviswheeler.libs;

//import java.lang.reflect.Array;

/**
 * Utility methods for arrays.
 *
 */
public class Arrays {
    
    
    /**
     * Joins two arrays of bytes. If one is null, the other
     * is returned. If both are null, null is returned. Otherwise, a new array
     * of size equal to the length of both arrays is returned, with the elements
     * of arr1 appearing before the elements of arr2.
     *
     * @param  arr1  first array.
     * @param  arr2  second array.
     * @return  joined arrays, or null if both arrays are null.
     * 
     * @author Travis Wheeler    
     */
    public static byte[] join(byte[] arr1, byte[] arr2) {

    	if (arr1 != null && arr2 != null) {
            byte[] arr = new byte[ arr1.length + arr2.length];
            System.arraycopy(arr1, 0, arr, 0, arr1.length);
            System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);
            return arr;
        } else if (arr2 == null) {
            return arr1;
        } else {
        	return arr2;
        }
    }
    
    /** Convert float array to byte array.
    @param arrF  float array to convert.
    @param arrB  byte array in which result should be placed.
	 */
	static final public void floatToByte(float [] arrF, byte[] arrB) throws Exception {
		if (arrB.length != arrF.length * 4) {
			LogWriter.stdErrLogln("arrays do not have agreeable length");
			throw new Exception();
		}
		int x;
		int bPos=0;
	    for(int i=0; i<arrF.length;i++) {
	    	x = Float.floatToIntBits(arrF[i]);
	    	arrB[bPos++] =(byte)((x >>> 24) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 16) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 8) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 0) & 0x000000FF);
    	
	    }
	}
	
    /** Convert int array to byte array.
    @param arrI  int array to convert.
    @param arrB  byte array in which result should be placed.
	 */
	static final public void intToByte(int [] arrI, byte[] arrB) throws Exception {
		if (arrB.length != arrI.length * 4) {
			LogWriter.stdErrLogln("arrays do not have agreeable length");
			throw new Exception();
		}
		int x;
		int bPos=0;
	    for(int i=0; i<arrI.length;i++) {
	    	x = arrI[i];
	 
	    	arrB[bPos++] =(byte)((x >>> 24) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 16) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 8) & 0x000000FF);
	    	arrB[bPos++] =(byte)((x >>> 0) & 0x000000FF);
    	
	    }
	}

	
	/** Convert byte array to float array.
    @param arrB  byte array to convert.
    @param arrF  float array in which result should be placed.
	*/
	static final public  void byteToFloat(byte[] arrB, float [] arrF) throws Exception{
		byteToFloat (arrB, arrF, arrF.length);
	}

	/** Convert byte array to float array.
    @param arrB  byte array to convert.
    @param arrF  float array in which result should be placed.
	*/
	static final public  void byteToFloat(byte[] arrB, float [] arrF, int cnt) throws Exception{
		if (arrB.length != arrF.length * 4) {
			LogWriter.stdErrLogln("arrays do not have agreeable length");
			throw new Exception();
		}
			
		int x;
		int bPos=0;
	    for(int i=0; i<cnt;i++) {
	    		x = (arrB[bPos++] & 0x000000FF) << 24;
	    		x += (arrB[bPos++] & 0x000000FF) << 16;
	    		x += (arrB[bPos++] & 0x000000FF) << 8;	 
	    		x += (arrB[bPos++] & 0x000000FF) << 0;
	    		arrF[i] = Float.intBitsToFloat(x);
	    		
	    }
	}
	
	/** Convert byte array to int array.
    @param arrB  byte array to convert.
    @param arrI  float array in which result should be placed.
	*/
	static final public void byteToInt(byte[] arrB, int [] arrI) throws Exception{
		if (arrB.length != arrI.length * 4) {
			LogWriter.stdErrLogln("arrays do not have agreeable length");
			throw new Exception();
		}
			
		int x;
		int bPos=0;
		int i=0;
	    for(i=0; i<arrI.length;i++) {
	    		x = (arrB[bPos++] & 0x000000FF) << 24;
	    		x += (arrB[bPos++] & 0x000000FF) << 16;
	    		x += (arrB[bPos++] & 0x000000FF) << 8;	 
	    		x += (arrB[bPos++] & 0x000000FF) << 0;
	    		arrI[i] = x;
	    }
	}


	
	static final public void byteToIntSpecial(String str, byte[] arrB, int [] arrI) throws Exception{
		if (arrB.length != arrI.length * 4) {
			LogWriter.stdErrLogln("arrays do not have agreeable length");
			throw new Exception();
		}
		
		LogWriter.stdErrLogln("Special Byte to Int : " + str);
			
		int x;
		int bPos=0;
		int i=0;
	    for(i=0; i<arrI.length;i++) {
	    		x = (arrB[bPos++] & 0x000000FF) << 24;
	    		x += (arrB[bPos++] & 0x000000FF) << 16;
	    		x += (arrB[bPos++] & 0x000000FF) << 8;	 
	    		x += (arrB[bPos++] & 0x000000FF) << 0;
	    		arrI[i] = x;
	    		
	    		LogWriter.stdErrLog(x + ", ");
	    		if (i%3 == 2) {
	    			LogWriter.stdErrLogln("(" + (i-2) + ")");
	    		}
	    }
	    
		LogWriter.stdErrLogln("" );
	}

	
}
