package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.User;

import com.googlecode.objectify.Key;


@SuppressWarnings("serial")
public class DoSomethingServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
	private static HttpServletResponse response;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp)
	throws ServletException, IOException {
		response = resp;
		fixJosh();
	}

	public void fixJosh() throws IOException {
		PrintWriter out = response.getWriter();
		User josh = DAO.instance.users.findUserByLogin("JoshuaNYoung@gmail.com", "gmail.com");
		if (josh.isEditor)
			out.println("josh fixed");
		else {
			out.println("trying to fix josh");
			josh.isEditor = true;
			DAO.instance.ofy().put(josh);
		}
	}
	
	public void doThing() throws IOException {
		PrintWriter out = response.getWriter();

		Key<Edition> e = DAO.instance.editions.getCurrentEdition().getKey();
		
		if (e == null) {
			out.println("No current edition");
			return;			
		}
		
		// insert some votes in the current edition
		
		User megangarber = DAO.instance.users.findUserByLogin("megangarber@gmail.com", "gmail.com");
		User jny2 = DAO.instance.users.findUserByLogin("jny2@gmail.com", "gmail.com");
		User steveouting = DAO.instance.users.findUserByLogin("steveouting@gmail.com", "gmail.com");
		
		String url1 = "http://example.com";
		String url2 = "http://example2.com";
		String url3 = "http://example3.com";
		
		Link l1 = DAO.instance.users.createLink(url1, "Title A B C", megangarber.getKey());
		Link l2 = DAO.instance.users.createLink(url2, "Title D E F", jny2.getKey());
		Link l3 = DAO.instance.users.createLink(url3, "Title G H I", megangarber.getKey());

		DAO.instance.users.voteFor(megangarber, e, l1, true);
		DAO.instance.users.voteFor(jny2, e, l1, true);
		DAO.instance.users.voteFor(steveouting, e, l1, true);

		DAO.instance.users.voteFor(megangarber, e, l2, true);
		DAO.instance.users.voteFor(steveouting, e, l2, true);

		DAO.instance.users.voteFor(jny2, e, l3, true);
		DAO d = DAO.instance;
		
		Edition current = d.editions.getCurrentEdition();
		Edition next = d.editions.getNextEdition();
		
		
		d.transition.transitionEdition();
		if (next == null) {
			log.info("End of periodical; last edition is" + current);
		}
		out.println("transition done");
	}
}
