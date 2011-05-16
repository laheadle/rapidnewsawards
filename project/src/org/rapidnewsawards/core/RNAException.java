package org.rapidnewsawards.core;


public class RNAException extends Exception {
	private static final long serialVersionUID = 1L;
	public String message;

	public RNAException(String message) {
		super();
		this.message = message;
	}
}
