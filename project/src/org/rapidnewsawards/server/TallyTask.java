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

	public static void tallyVote(Transaction txn, Vote v, boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "tallyVote")
				.param("vote", v.id.toString())
				.param("on", Boolean.toString(on))
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void addJudgeScore(Transaction txn, Vote v, int authority, boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "addJudgeScore")
				.param("vote", v.id.toString())
				.param("authority", Integer.toString(authority))
				.param("on", Boolean.toString(on))
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void findEditorsToScore(Transaction txn, Vote v, boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "findEditorsToScore")
				.param("vote", v.id.toString())
				.param("on", Boolean.toString(on))
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void deleteVote(Transaction txn, Vote v, Set<Key<User>> editors, Key<Edition> edition) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "deleteVote")
				.param("editors", encodeUsers(editors))
				.param("edition", edition.getName())
				.param("vote", v.id.toString())
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void addEditorScore(Transaction txn, Set<Key<User>> editors, 
			Key<Edition> edition, boolean on) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "addEditorScore")
				.param("editors", encodeUsers(editors))
				.param("edition", edition.getName())
				.param("on", Boolean.toString(on)));
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
			String onstr = request.getParameter("on");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			if (onstr == null) {
				throw new IllegalArgumentException("on");
			}
			Long voteId = Long.valueOf(votestr);
			Long userId = Long.valueOf(userstr);
			boolean on = Boolean.valueOf(onstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.tallyVote(vkey, on);
		}
		else if (fun.equals("addJudgeScore")) {
			String votestr = request.getParameter("vote");
			String authoritystr = request.getParameter("authority");
			String userstr = request.getParameter("user");
			String onstr = request.getParameter("on");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (authoritystr == null) {
				throw new IllegalArgumentException("authority");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			if (onstr == null) {
				throw new IllegalArgumentException("on");
			}
			long voteId = Long.valueOf(votestr);
			int authority = Integer.valueOf(authoritystr);
			long userId = Long.valueOf(userstr);
			boolean on = Boolean.valueOf(onstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.addJudgeScore(vkey, authority, on);
		}
		else if (fun.equals("findEditorsToScore")) {
			String votestr = request.getParameter("vote");
			String onstr = request.getParameter("on");
			String userstr = request.getParameter("user");
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (onstr == null) {
				throw new IllegalArgumentException("on");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			long voteId = Long.valueOf(votestr);
			boolean on = Boolean.valueOf(onstr);
			long userId = Long.valueOf(userstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.findEditorsToScore(vkey, on);
		}
		else if (fun.equals("deleteVote")) {
			String editorsstr = request.getParameter("editors");
			String editionstr = request.getParameter("edition");
			String votestr = request.getParameter("vote");
			String userstr = request.getParameter("user");
			if (editorsstr == null) {
				throw new IllegalArgumentException("editors");
			}
			if (editionstr == null) {
				throw new IllegalArgumentException("edition");
			}
			if (votestr == null) {
				throw new IllegalArgumentException("vote");
			}
			if (userstr == null) {
				throw new IllegalArgumentException("user");
			}
			Set<Key<User>> editors = decodeUsers(editorsstr);
			Long voteId = Long.valueOf(votestr);
			Long userId = Long.valueOf(userstr);
			Key<User> ukey = new Key<User>(User.class, userId);
			Key<Vote> vkey = new Key<Vote>(ukey, Vote.class, voteId);

			d.deleteVote(vkey, editors, Edition.createKey(Integer.parseInt(editionstr)));
		}
		else if (fun.equals("addEditorScore")) {
			String editorsstr = request.getParameter("editors");
			String editionstr = request.getParameter("edition");
			String onstr = request.getParameter("on");
			if (editorsstr == null) {
				throw new IllegalArgumentException("editors");
			}
			if (editionstr == null) {
				throw new IllegalArgumentException("edition");
			}
			if (onstr == null) {
				throw new IllegalArgumentException("on");
			}
			Set<Key<User>> editors = decodeUsers(editorsstr);
			boolean on = Boolean.valueOf(onstr);
			d.addEditorScore(editors, Edition.createKey(Integer.parseInt(editionstr)), on);
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
