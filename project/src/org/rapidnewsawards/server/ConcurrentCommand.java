package org.rapidnewsawards.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.logging.Logger;

import com.google.appengine.api.taskqueue.DeferredTask;

public abstract class ConcurrentCommand implements DeferredTask {

	private static final int WARM  = 150;

	private static final int HOT = 2000;

	// from lowest to highest priority tasks

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// assume 25ms latency, so 5m total
	public static final int MANY = 12000;
	public static final int BRIEF = 1;

	private static final Logger log = Logger.getLogger(ConcurrentServletCommand.class
			.getName());

	private static final int WAY_TOO_LONG = 1500;

	private static final int LONG_TIME = 500;

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
		long begin = new Date().getTime();
		try {
			log.info(String.format("BEGIN deferred call %s", fun()));
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
					log.severe("Fatal: " + (e.message == null? "" : e.message) + sw.toString());
					return; // GIVE UP!
				}
			}
			throw new TooBusyException(getRetries());
		}
		finally {
			int re = getRetries() + 1;
			if (re > HOT) {
				log.severe(String.format("HOT: %s: %d tries", fun(), re));
			}
			else if (re > WARM ) {
				log.warning(String.format("WARM: %s: %d tries", fun(), re));
			}
			else {
				log.info(String.format("%s: %d tries", fun(), re));
			}
			long diff = new Date().getTime() - begin;
			if (diff > WAY_TOO_LONG) {
				log.severe(String.format("VERY SLOW: %d ms", diff));
			}
			else if (diff > LONG_TIME ) {
				log.warning(String.format("SLOW: %d ms", diff));
			}
			else {
				log.info(String.format("%d ms", diff));
			}
		}
	}
}
