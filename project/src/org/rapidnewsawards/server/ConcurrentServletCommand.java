package org.rapidnewsawards.server;

import java.util.ConcurrentModificationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


abstract class ConcurrentServletCommand {

	public int retries;
	public int maxTries;
	public int sleepInterval;

	public ConcurrentServletCommand() {
		retries = 0;
		maxTries = 30;
		sleepInterval = 100;
	}
	
	public Object run(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException, TooBusyException {
		while (retries < maxTries) {
			try {
				return perform(request, resp);
			}
			catch (ConcurrentModificationException e) {
				retries++;
				try {
					Thread.sleep(sleepInterval);
				} catch (InterruptedException e1) {}
			}
		}
		throw new TooBusyException(retries);
	}
	
	protected abstract Object perform(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException;

}