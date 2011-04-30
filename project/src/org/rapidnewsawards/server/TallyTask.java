package org.rapidnewsawards.server;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.messages.Name;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;

public class TallyTask  extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//private static final Logger log = Logger.getLogger(TallyTask.class.getName());
	private static DAO d = DAO.instance;

	public static void scheduleImmediately() {
		// immediately tally all votes
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(null, url("/tasks/tally").method(TaskOptions.Method.GET));
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		Edition current = d.editions.getCurrentEdition(Name.AGGREGATOR_NAME);
		if (current != null) {
			d.editions.fund(current);
		}
	}

}
