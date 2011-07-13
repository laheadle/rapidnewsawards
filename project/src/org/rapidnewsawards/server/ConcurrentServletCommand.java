package org.rapidnewsawards.server;

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


abstract class ConcurrentServletCommand {
	
	private static final Logger log = Logger.getLogger(ConcurrentServletCommand.class
			.getName());

	private final int maxTries;
	private final int sleepInterval;
	private int retries;
	private int cacheWaits;
	
	public ConcurrentServletCommand(int maxTries, int sleepInterval) {
		this.setRetries(0);
		this.setCacheWaits(0);
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
				catch (CacheWait e) {
					setRetries(getRetries() + 1);
					setCacheWaits(getCacheWaits() + 1);
					try {
						Thread.sleep(sleepInterval);
					} catch (InterruptedException e1) {}
				}
			}
			throw new TooBusyException(getRetries());
		}
		finally {
			assert(fun != null);
			log.info(String.format("end call %s: %d tries, %d cache waits", fun, getRetries() + 1, getCacheWaits()));			
		}
	}
	
	private void setCacheWaits(int i) {
		cacheWaits = i;
	}

	public int getCacheWaits() {
		return cacheWaits;
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