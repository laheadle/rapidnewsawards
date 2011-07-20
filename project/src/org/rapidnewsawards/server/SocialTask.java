package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;

import java.util.HashSet;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.JudgeInfluence;
import org.rapidnewsawards.core.User;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.DeferredTask;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

public class SocialTask {
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
	
	public static void writeSocialEvent(final Key<User> from, 
			final Key<User> to, final Key<Edition> e, final boolean on, 
			final boolean cancelPending, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.writeSocialEvent(from, to, e, on, cancelPending);
			}
			@Override
			public String fun() {
				return "writeSocialEvent";
			}		
		}));
	}

	public static void writeFutureFollowedBys(final Key<User> judge, final Key<User> editor,
			final Key<Edition> e, final boolean on, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.writeFutureFollowedBys(judge, editor, e, on);
			}
			@Override
			public String fun() {
				return "writeFutureFollowedBys";
			}		
		}));
	}

	public static void changePendingAuthority(final Key<User> to,
			final Key<Edition> e, final int amount, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.changePendingAuthority(to, e, amount);
			}
			@Override
			public String fun() {
				return "changePendingAuthority";
			}		
		}));
	}

	public static void join(final User user, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.join(user);
			}
			@Override
			public String fun() {
				return "join";
			}		
		}));
	}

	public static void rnaFollow(final User user, final Edition edition, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.rnaFollow(user, edition);
			}
			@Override
			public String fun() {
				return "rnaFollow";
			}		
		}));
	}

	public static void welcomeJudgeInfluence(final User usr, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.welcomeJudgeInfluence(usr);
			}
			@Override
			public String fun() {
				return "welcomeJudgeInfluence";
			}		
		}));
	}

	public static void finishWelcomeJudgeInfluence(final HashSet<JudgeInfluence> jiset, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.social.finishWelcomeJudgeInfluence(jiset);
			}
			@Override
			public String fun() {
				return "finishWelcomeJudgeInfluence";
			}		
		}));
	}
}