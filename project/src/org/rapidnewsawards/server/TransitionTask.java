package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.RNAException;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

public class TransitionTask  extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static DAO d = DAO.instance;

	public static void scheduleTransition(Edition e) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(withUrl("/tasks/transition").method(TaskOptions.Method.GET)
				.param("fun", "transition")
				.param("fromEdition", Integer.toString(e.getNumber()))
				.etaMillis(e.end.getTime()));
	}

	// these are all called, in this order, directly or indirectly by doTransition

	public static void updateAuthorities(Transaction txn, int nextEdition) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/transition").method(TaskOptions.Method.GET)
				.param("fun", "updateAuthorities")
				.param("nextEdition", Integer.toString(nextEdition)));
	}	

	public static void setPeriodicalBalance(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		// no transaction, will retry if fails
		queue.add(txn, withUrl("/tasks/transition").method(TaskOptions.Method.GET)
				.param("fun", "setPeriodicalBalance"));		
	}


	public static void setSpaceBalance(Transaction txn, Edition e, int revenue) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/transition").method(TaskOptions.Method.GET)
				.param("fun", "setSpaceBalance")
				.param("edition", Integer.toString(e.getNumber()))
				.param("revenue", Integer.toString(revenue)));
	}

	public static void finish(Transaction txn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(txn, withUrl("/tasks/transition").method(TaskOptions.Method.GET)
				.param("fun", "finish"));
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		try {
			_doGet(request, response);
		} catch (RNAException e) {
			// TODO chain
			throw new IllegalStateException(e.message);
		}
	}

	private void _doGet(HttpServletRequest request, HttpServletResponse response) 
	throws RNAException {

		String fun = request.getParameter("fun");
		if (fun == null) {
			throw new IllegalArgumentException("fun");
		}
		if (fun.equals("transition")) {
			String _from = request.getParameter("fromEdition");
			if (_from == null) {
				throw new IllegalArgumentException("from");
			}
			int from = Integer.valueOf(_from);
			d.transition.doTransition(from);
		}
		else if (fun.equals("setPeriodicalBalance")) {
			d.transition.setPeriodicalBalance();
		}		
		else if (fun.equals("setSpaceBalance")) {
			String _edition = request.getParameter("edition");
			if (_edition == null) {
				throw new IllegalArgumentException("edition");
			}
			String _revenue = request.getParameter("revenue");
			if (_revenue == null) {
				throw new IllegalArgumentException("revenue");
			}
			int edition = Integer.valueOf(_edition);
			int revenue = Integer.valueOf(_revenue);
			d.editions.setSpaceBalance(edition, revenue);
		}		
		else if (fun.equals("finish")) {
			d.transition.finishTransition();
		}		
	}
	
	

}
