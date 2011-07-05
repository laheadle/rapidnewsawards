package org.rapidnewsawards.server;

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


abstract class ConcurrentServletCommand {

	// from lowest to highest priority tasks
	
	// 2s total
	public static final int FEW = 20;
	public static final int LONG = 100;

	// 1s total
	public static final int MANY = 100;
	public static final int BRIEF = 10;
	
	// 5s total
	public static final int TONS = 2500;
	public static final int VERY_BRIEF = 2;

	
	private static final Logger log = Logger.getLogger(ConcurrentServletCommand.class
			.getName());

	private final int maxTries;
	private final int sleepInterval;
	private int retries;

	public ConcurrentServletCommand(int maxTries, int sleepInterval) {
		this.setRetries(0);
		this.maxTries = maxTries;
		this.sleepInterval = sleepInterval;
	}
	
	public Object run(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException, TooBusyException {
		
		String fun;
		if ((fun = request.getParameter("fun")) == null) {
			fun = "unknown";
		}
		log.info("begin call " + fun);
		try {
			while (getRetries() < maxTries) {
				try {
					return perform(request, resp);
				}
				catch (ConcurrentModificationException e) {
					setRetries(getRetries() + 1);
					try {
						Thread.sleep(sleepInterval);
					} catch (InterruptedException e1) {}
				}
			}
			throw new TooBusyException(getRetries());
		}
		finally {
			assert(fun != null);
			log.info(String.format("end call %d: %d tries", fun, getRetries()));			
		}
	}
	
	protected abstract Object perform(HttpServletRequest request, HttpServletResponse resp) 
	throws RNAException;

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public int getRetries() {
		return retries;
	}

}