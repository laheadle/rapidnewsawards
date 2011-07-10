package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Donation;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditorInfluence;
import org.rapidnewsawards.core.EditorVote;
import org.rapidnewsawards.core.Follow;
import org.rapidnewsawards.core.FollowedBy;
import org.rapidnewsawards.core.JudgeInfluence;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.SocialEvent;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;

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
		result = kill(Donation.class);
		out.println(result + "<br>");
		result = kill(Edition.class);
		out.println(result + "<br>");
		result = kill(EditorInfluence.class);
		out.println(result + "<br>");
		result = kill(EditorVote.class);
		out.println(result + "<br>");
		result = kill(Follow.class);
		out.println(result + "<br>");
		result = kill(FollowedBy.class);
		out.println(result + "<br>");
		result = kill(JudgeInfluence.class);
		out.println(result + "<br>");
		result = kill(Link.class);
		out.println(result + "<br>");
		result = kill(Periodical.class);
		out.println(result + "<br>");
		result = kill(Root.class);
		out.println(result + "<br>");
		result = kill(ScoredLink.class);
		out.println(result + "<br>");
		result = kill(ScoreRoot.class);
		out.println(result + "<br>");
		result = kill(ScoreSpace.class);
		out.println(result + "<br>");
		result = kill(SocialEvent.class);
		out.println(result + "<br>");
		result = kill(User.class);
		out.println(result + "<br>");
		result = kill(Vote.class);
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