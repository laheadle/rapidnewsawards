package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;

import java.util.Date;

import org.rapidnewsawards.core.Edition;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;

public class TransitionTask {
	private static DAO d = DAO.instance;

	public static abstract class Task extends ConcurrentCommand implements DeferredTask {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Task() {
			super(MANY, BRIEF);
		}

		public abstract void rnaRun() throws RNAException;
	}
	
	public static void scheduleTransition(final Edition e) {
		scheduleTransitionAt(e.getKey(), e.end);
	}

	public static void scheduleTransitionAt(final Key<Edition> e, Date date) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.transition.doTransition(e);
			}
			@Override
			public String fun() {
				return "doTransition";
			}		
		}).etaMillis(date.getTime()));
	}

	// these are all called, in this order, directly or indirectly by transitionEdition

	public static void setEditionFinished(final Transaction txn, final Key<Edition> e) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.editions.setEditionFinished(Edition.getNumber(e));
			}
			@Override
			public String fun() {
				return "setEditionFinished";
			}		
		}));
	}
	
	public static void setPeriodicalBalance(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.transition.setPeriodicalBalance();
			}
			@Override
			public String fun() {
				return "setPeriodicalBalance";
			}		
		}));
	}

	public static void setSpaceBalance(Transaction txn, final Edition e, final int balance) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.editions.setSpaceBalance(e.number, balance);
			}
			@Override
			public String fun() {
				return "setSpaceBalance";
			}		
		}));
	}

	public static void finish(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.transition.finishTransition();
			}
			@Override
			public String fun() {
				return "finishTransition";
			}		
		}));
	}	

}
