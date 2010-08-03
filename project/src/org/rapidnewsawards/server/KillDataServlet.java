package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Donation;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Follow;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.Root;
import org.rapidnewsawards.shared.ScoredLink;
import org.rapidnewsawards.shared.SocialEvent;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.Vote;

public class KillDataServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DAO d;
	private int limit = 200;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		
		d = DAO.instance;
		
		String result;
		result = kill(Edition.class);
		out.println(result + "<br>");
		result = kill(Periodical.class);
		out.println(result + "<br>");
		result = kill(User.class);
		out.println(result + "<br>");
		result = kill(Root.class);
		out.println(result + "<br>");
		result = kill(Vote.class);
		out.println(result + "<br>");
		result = kill(SocialEvent.class);
		out.println(result + "<br>");
		result = kill(Follow.class);
		out.println(result + "<br>");
		result = kill(Link.class);
		out.println(result + "<br>");
		result = kill(Donation.class);
		out.println(result + "<br>");
		result = kill(ScoredLink.class);
		out.println(result + "<br>");

	}

	private <T> String kill(Class<T> class1) {
		if (d.ofy().query(class1).limit(1).get() != null) {
			d.ofy().delete(d.ofy().query(class1).limit(limit));
			return "yes: " + class1;
		}
		else {
			return "no: " + class1;
		}
	}
}