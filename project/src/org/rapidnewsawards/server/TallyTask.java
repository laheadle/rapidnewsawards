package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

public class TallyTask  extends HttpServlet {
	private static final String DELIMIT = ",";
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(TallyTask.class.getName());
	private static DAO d = DAO.instance;

	public static void writeVote(Transaction txn,
			User u, Key<Edition> e, Link l, boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "writeVote")
				.param("user", Long.toString(u.id))
				.param("edition", e.getName())
				.param("link", Long.toString(l.id))
				.param("on", Boolean.toString(on)));		
	}

	public static void tallyVote(Transaction txn, Vote v) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "tallyVote")
				.param("vote", v.id.toString())
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void addJudgeFunding(Transaction txn, Vote v, int fund) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "addJudgeFunding")
				.param("vote", v.id.toString())
				.param("fund", Integer.toString(fund))
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void findEditorsToFund(Transaction txn, Vote v, int fund) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "findEditorsToFund")
				.param("vote", v.id.toString())
				.param("fund", Integer.toString(fund))
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void addEditorFunding(Transaction txn, Set<Key<User>> editors, 
			Key<Edition> edition, int fund) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "addEditorFunding")
				.param("editors", encodeUsers(editors))
				.param("edition", edition.getName())
				.param("fund", Integer.toString(fund)));
	}

	public static void releaseUserLock(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "releaseUserLock"));
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		ConcurrentServletCommand command = new ConcurrentServletCommand() {
			@Override
			public Object perform(HttpServletRequest request, HttpServletResponse resp) 
			throws RNAException {
				_doGet(request, resp);
				return Boolean.TRUE;
			}
		};
		try {
			command.run(request, response);
			if (command.retries > 0) {
				log.warning(String.format(
						"command %s needed %d retries.", request, command.retries));
			}			
		} catch (RNAException e) {
			throw new IllegalStateException(e);
		} catch (TooBusyException e) {
			throw new ConcurrentModificationException("too many retries");
		}
	}

	public void _doGet(HttpServletRequest request, HttpServletResponse response) {
		String fun = request.getParameter("fun");
		if (fun == null) {
			throw new IllegalArgumentException("fun");
		}
		if (fun.equals("writeVote")) {
			String _user = request.getParameter("user");
			if (_user == null) {
				throw new IllegalArgumentException("user");
			}
			long user = Long.valueOf(_user);
			String edition = request.getParameter("edition");
			if (edition == null) {
				throw new IllegalArgumentException("edition");
			}
			String _link = request.getParameter("link");
			if (_link == null) {
				throw new IllegalArgumentException("link");
			}
			long link = Long.valueOf(_link);
			String _on = request.getParameter("on");
			if (_on == null) {
				throw new IllegalArgumentException("on");
			}
			boolean on = Boolean.valueOf(_on);
			d.users.writeVote(new Key<User>(User.class, user), 
					new Key<Edition>(Edition.class, edition), 
					new Key<Link>(Link.class, link), 
					on);
		}
		else if (fun.equals("tallyVote")) {
			String votestr = request.getParameter("vote");
			String userstr = request.getParameter("user");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			Long voteId = Long.valueOf(votestr);
			Long userId = Long.valueOf(userstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.tallyVote(vkey);
		}
		else if (fun.equals("addJudgeFunding")) {
			String votestr = request.getParameter("vote");
			String fundstr = request.getParameter("fund");
			String userstr = request.getParameter("user");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (fundstr == null) {
				throw new IllegalArgumentException("fund");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			long voteId = Long.valueOf(votestr);
			int fund = Integer.valueOf(fundstr);
			long userId = Long.valueOf(userstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.addJudgeFunding(vkey, fund);
		}
		else if (fun.equals("findEditorsToFund")) {
			String votestr = request.getParameter("vote");
			String fundstr = request.getParameter("fund");
			String userstr = request.getParameter("user");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (fundstr == null) {
				throw new IllegalArgumentException("fund");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			long voteId = Long.valueOf(votestr);
			int fund = Integer.valueOf(fundstr);
			long userId = Long.valueOf(userstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.findEditorsToFund(vkey, fund);
		}
		else if (fun.equals("addEditorFunding")) {
			String editorsstr = request.getParameter("editors");
			String editionstr = request.getParameter("edition");
			String fundstr = request.getParameter("fund");
			if (editorsstr == null) {
				throw new IllegalArgumentException("editors");
			}
			if (editionstr == null) {
				throw new IllegalArgumentException("edition");
			}
			if (fundstr == null) {
				throw new IllegalArgumentException("fund");
			}
			Set<Key<User>> editors = decodeUsers(editorsstr);
			int fund = Integer.valueOf(fundstr);
			d.addEditorFunding(editors, Edition.createKey(Integer.parseInt(editionstr)), fund);
		}
		else if (fun.equals("releaseUserLock")) {
			try {
				d.releaseUserLock();
			} catch (RNAException e) {
				// TODO chain
				throw new IllegalStateException(e.message);
			}
		}
	}

	private static String encodeUsers(Set<Key<User>> editors) {
		String result = null;
		int count = 0;
		for (Key<User> k : editors) {
			if (count++ == 0) {
				 result = Long.toString(k.getId());
			}
			else {
				result += DELIMIT + Long.toString(k.getId());
			}
		}
		return result;
	}

	private Set<Key<User>> decodeUsers(String editorsstr) {
		StringTokenizer tok = new StringTokenizer(editorsstr, DELIMIT);
		Set<Key<User>> result = new HashSet<Key<User>>();
		while (tok.hasMoreTokens()) {
			long id = Long.parseLong(tok.nextToken());
			result.add(new Key<User>(User.class, id));
		}
		return result;
	}

}
