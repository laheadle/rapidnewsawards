package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;

import java.util.Set;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;

public class TallyTask {
	private static DAO d = DAO.instance;

	public static abstract class Task extends ConcurrentCommand {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Task() {
			super(MANY, BRIEF);
		}

		public abstract void rnaRun() throws RNAException;
	}

	public static void writeVote(Transaction txn,
			final Key<User> u, final Key<Edition> e, final Key<Link> l, final boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.users.writeVote(u, e, l, on);
			}
			@Override
			public String fun() {
				return "writeVote";
			}		
		}));
	}

	public static void tallyVote(Transaction txn, final Key<Vote> v, final boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.tallyVote(v, on);
			}
			@Override
			public String fun() {
				return "tallyVote";
			}		
		}));
	}

	public static void addJudgeScore(Transaction txn, final Key<Vote> v, final int authority, final boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.addJudgeScore(v, authority, on);
			}
			@Override
			public String fun() {
				return "addJudgeScore";
			}		
		}));
	}

	public static void findEditorsToScore(Transaction txn, final Key<Vote> v, final boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.findEditorsToScore(v, on);
			}
			@Override
			public String fun() {
				return "findEditorsToScore";
			}		
		}));
	}

	public static void deleteEditorVotes(Transaction txn, final Key<Vote> v,
			final Set<Key<User>> editors, final Key<Edition> edition) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.deleteEditorVotes(v, editors, edition);
			}
			@Override
			public String fun() {
				return "deleteEditorVotes";
			}		
		}));
	}

	public static void addEditorVotes(Transaction txn, final Key<Vote> vkey,
			final Set<Key<User>> editors, final Key<Edition> edition, boolean on, final Key<User> ukey, final Key<Link> lkey) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.addEditorVotes(vkey, editors, edition, ukey, lkey);
			}
			@Override
			public String fun() {
				return "addEditorVotes";
			}		
		}));
	}

	public static void deleteVote(Transaction txn, final Key<Vote> vkey, final Set<Key<User>> editors, final Key<Edition> edition) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.deleteVote(vkey, editors, edition);
			}
			@Override
			public String fun() {
				return "deleteVote";
			}		
		}));
	}

	public static void addEditorScore(Transaction txn, final Set<Key<User>> editors, 
			final Key<Edition> edition, final boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.addEditorScore(editors, edition, on);
			}
			@Override
			public String fun() {
				return "addEditorScore";
			}		
		}));
	}

	public static void releaseUserLock(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				d.releaseUserLock();
			}
			@Override
			public String fun() {
				return "releaseUserLock";
			}		
		}));
	}
}
