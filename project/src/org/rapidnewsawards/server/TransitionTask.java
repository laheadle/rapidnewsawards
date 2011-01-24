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
		d.doTransition(Name.AGGREGATOR_NAME, new Integer(_from), null);
	}	
}
