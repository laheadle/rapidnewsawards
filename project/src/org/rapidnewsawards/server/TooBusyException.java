package org.rapidnewsawards.server;

public class TooBusyException extends Exception {
	private static final long serialVersionUID = 1L;
	public int tries;

	public TooBusyException(int tries) {
		super();
		this.tries = tries;
	}

}
