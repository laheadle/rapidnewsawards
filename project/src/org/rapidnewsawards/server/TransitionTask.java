package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class TransitionTask  extends HttpServlet {
	private static final Logger log = Logger.getLogger(TransitionTask.class.getName());
	private static DAO d = DAO.instance;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		String _from = request.getParameter("fromEdition");
		Edition from = d.getEdition(Name.AGGREGATOR_NAME, new Integer(_from), null);

		Edition current = d.getCurrentEdition(Name.AGGREGATOR_NAME);
		Edition next = d.getNextEdition(Name.AGGREGATOR_NAME);
		
		if (from == null) {
			log.severe("Edition " + _from + " does not exist");
			return;
		}
		if (!from.equals(current)) {
			log.warning("edition 1 not current (2 is): " + from + ", " + current);
		}
		
		if (next == null) {
			d.finishPeriodical(Name.AGGREGATOR_NAME);
			log.info("End of periodical; last edition is" + current);
		}
		else {
			d.transitionEdition(Name.AGGREGATOR_NAME);
			d.socialTransition(next);
		}
		d.setEditionRevenue();
		d.fund(current);		
	}
	
}
