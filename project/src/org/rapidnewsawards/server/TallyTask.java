package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.RNAException;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

public class TallyTask  extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//private static final Logger log = Logger.getLogger(TallyTask.class.getName());
	private static DAO d = DAO.instance;

	public static void tallyVote(Transaction txn, Vote v) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "tallyVote")
				.param("vote", v.id.toString())
				.param("user", Long.toString(v.voter.getId())));
	}

	public static void releaseUserLock(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("fun", "releaseUserLock"));
	}

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

	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		DAO.log.info("boo");
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
		else if (fun.equals("releaseUserLock")) {
			try {
				d.releaseUserLock();
			} catch (RNAException e) {
				// TODO chain
				throw new IllegalStateException(e.message);
			}
		}
	}

}
