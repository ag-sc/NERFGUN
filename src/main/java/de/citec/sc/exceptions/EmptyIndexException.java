package de.citec.sc.exceptions;

/**
 * This exception is thrown if the index-file does not contains any data.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class EmptyIndexException extends Exception {

	public EmptyIndexException() {
		super();
	}

	public EmptyIndexException(String msg) {
		super(msg);
	}

}
