package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Vote;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;

public class TallyTask  extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//private static final Logger log = Logger.getLogger(TallyTask.class.getName());
	private static DAO d = DAO.instance;

	public static void tallyVote(Transaction txn, Vote v) {
		// immediately tally one vote
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/tally").method(TaskOptions.Method.GET)
				.param("vote", v.id.toString())
				.param("user", Long.toString(v.voter.getId())));
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
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

}
