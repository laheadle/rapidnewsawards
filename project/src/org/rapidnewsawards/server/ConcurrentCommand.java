package org.rapidnewsawards.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import com.google.appengine.api.taskqueue.DeferredTask;

public abstract class ConcurrentCommand implements DeferredTask {

	// from lowest to highest priority tasks

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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

	public ConcurrentCommand(int maxTries, int sleepInterval) {
		this.setRetries(0);
		this.maxTries = maxTries;
		this.sleepInterval = sleepInterval;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public int getRetries() {
		return retries;
	}

	public abstract void rnaRun() throws RNAException;
	public abstract String fun();

	@Override
	public void run() {
		try {
			while (getRetries() < maxTries) {
				try {
					rnaRun();
					return;
				}
				catch (ConcurrentModificationException e) {
					setRetries(getRetries() + 1);
					try {
						Thread.sleep(sleepInterval);
					} catch (InterruptedException e1) {}
				} catch (RNAException e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw); 
					log.severe((e.getMessage() == null? "" : e.getMessage()) + sw.toString());
					return; // GIVE UP!
				}
			}
			throw new TooBusyException(getRetries());
		}
		finally {
			log.info(String.format("end DEFERRED call %s: %d tries", fun(), getRetries() + 1));
		}
	}
}
