package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;


public class VoteServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(VoteServlet.class.getName());
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		Edition e = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		if (e == null) {
			out.println("No current edition");
			return;			
		}
		
		User u = DAO.instance.findUserByUsername(request.getParameter("username"));
		if (u == null) {
			out.println("No such voter");
			return;
		}
		
		String url = request.getParameter("href");
		
		// TODO broken on some complex hrefs
		Link l = DAO.instance.findOrCreateLinkByURL(url);
		
		try	{
			DAO.instance.voteFor(u, e, l);
		}
		// TODO handle malformed urls
		catch (IllegalArgumentException ex) {
			log.warning("BAD VOTE: " + u.getUsername() + ", " + url);
			out.println("vote already counted");	
			return;
		}

		log.info("VOTE: " + u.getUsername() + ", " + url);
		out.println("vote counted");
		
	}
}
