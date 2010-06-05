package org.rapidnewsawards.server;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Periodical;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;

public class TallyTask  extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//private static final Logger log = Logger.getLogger(TallyTask.class.getName());
	private static DAO d = DAO.instance;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		Edition current = d.getCurrentEdition(Name.JOURNALISM);
		if (current != null) {
			d.tally(current.getKey());
			Queue queue = QueueFactory.getDefaultQueue();
			queue.add(url("/tasks/tally").etaMillis(new Date().getTime() + 2 * MakeDataServlet.ONE_MINUTE).method(TaskOptions.Method.GET));
		}
	}

}
