package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;

public class SocialTask extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static DAO d = DAO.instance;

	public static void writeSocialEvent(Key<User> from, 
			Key<User> to, Key<Edition> e, boolean on, 
			Transaction txn) {

		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/social").method(TaskOptions.Method.GET)
				.param("fun", "writeSocialEvent")
				.param("from", Long.toString(from.getId()))
				.param("to", Long.toString(from.getId()))
				.param("e", e.getName())
				.param("on", Boolean.toString(on)));
	}

	public static void changePendingAuthority(Key<User> to,
			Key<Edition> e, int amount, Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/social").method(TaskOptions.Method.GET)
				.param("fun", "changePendingAuthority")
				.param("to", Long.toString(to.getId()))
				.param("e", e.getName())
				.param("amount", Integer.toString(amount)));
	}


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		String fun = request.getParameter("fun");
		if (fun == null) {
			throw new IllegalArgumentException("fun");
		}
		if (fun.equals("writeSocialEvent")) {
			String _from = request.getParameter("from");
			if (_from == null) {
				throw new IllegalArgumentException("from");
			}
			long from = Long.valueOf(_from);
			String _to = request.getParameter("to");
			if (_to == null) {
				throw new IllegalArgumentException("to");
			}
			long to = Long.valueOf(_to);
			String e = request.getParameter("e");
			if (e == null) {
				throw new IllegalArgumentException("e");
			}
			String _on = request.getParameter("on");
			if (_on == null) {
				throw new IllegalArgumentException("on");
			}
			boolean on = Boolean.valueOf(_on);
			d.social.writeSocialEvent( 
					// TODO use factory methods for these
					new Key<User>(User.class, from), 
					new Key<User>(User.class, to), 
					new Key<Edition>(Edition.class, e), 
					on);
		}
		else if (fun.equals("changePendingAuthority")) {
			String _to = request.getParameter("to");
			if (_to == null) {
				throw new IllegalArgumentException("to");
			}
			long to = Long.valueOf(_to);
			String e = request.getParameter("e");
			if (e == null) {
				throw new IllegalArgumentException("e");
			}
			String _amount = request.getParameter("amount");
			if (_amount == null) {
				throw new IllegalArgumentException("amount");
			}
			int amount = Integer.valueOf(_amount);
			d.social.changePendingAuthority(new Key<User>(User.class, to), 
					new Key<Edition>(Edition.class, e), 
					amount);
		}
	}
}