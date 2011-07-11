package org.rapidnewsawards.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import com.google.appengine.api.taskqueue.DeferredTask;

public abstract class ConcurrentCommand implements DeferredTask {

	private static final int WARM  = 50;

	private static final int HOT = 200;

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
					log.severe((e.message == null? "" : e.message) + sw.toString());
					return; // GIVE UP!
				}
			}
			throw new TooBusyException(getRetries());
		}
		finally {
			int re = getRetries() + 1;
			if (re > HOT) {
				log.severe(String.format("HOT - end DEFERRED call %s: %d tries", fun(), re));
			}
			else if (re > WARM ) {
				log.warning(String.format("WARM - end DEFERRED call %s: %d tries", fun(), re));
			}
			else {
				log.info(String.format("end DEFERRED call %s: %d tries", fun(), re));
			}
		}
	}
}
