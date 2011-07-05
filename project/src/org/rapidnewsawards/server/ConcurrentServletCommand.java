package org.rapidnewsawards.server;

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


abstract class ConcurrentServletCommand {
	private static final Logger log = Logger.getLogger(ConcurrentServletCommand.class
			.getName());

	public int retries;
	public int maxTries;
	public int sleepInterval;

	public ConcurrentServletCommand() {
		retries = 0;
		maxTries = 60;
		sleepInterval = 10;
	}
	
	public Object run(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException, TooBusyException {
		
		String fun;
		if ((fun = request.getParameter("fun")) == null) {
			fun = "unknown";
		}
		log.info("begin call: " + fun);
		try {
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
		finally {
			assert(fun != null);
			log.info("end call: " + fun);			
		}
	}
	
	protected abstract Object perform(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException;

}