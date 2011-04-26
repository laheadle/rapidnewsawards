package org.rapidnewsawards.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Name;

public class TransitionTask  extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static DAO d = DAO.instance;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		String _from = request.getParameter("fromEdition");
		d.transition.doTransition(Name.AGGREGATOR_NAME, new Integer(_from), null);
	}	
}
