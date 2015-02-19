package com.traviswheeler.ninja;

/**
 * Exception class for Ninja. If it should crash ... 
 * throw this so the wrapper can catch it 
 * @author Travis Wheeler
 */
public class GenericNinjaException extends RuntimeException {
    /**
     * Construct this exception object.
     * @param message the error message.
     */
    public GenericNinjaException( String message ) {
        super( message );
    }
}