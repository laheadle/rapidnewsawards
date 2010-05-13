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


@SuppressWarnings("serial")
public class DoSomethingServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		Edition e = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		if (e == null) {
			out.println("No current edition");
			return;			
		}
		
		// insert some votes in the current edition
		
		User megangarber = DAO.instance.findUserByUsername("megangarber");
		User jny2 = DAO.instance.findUserByUsername("jny2");
		User steveouting = DAO.instance.findUserByUsername("steveouting");
		
		String url1 = "http://example.com";
		String url2 = "http://example2.com";
		String url3 = "http://example3.com";
		
		Link l1 = DAO.instance.findOrCreateLinkByURL(url1, megangarber.getKey());
		Link l2 = DAO.instance.findOrCreateLinkByURL(url2, jny2.getKey());
		Link l3 = DAO.instance.findOrCreateLinkByURL(url3, megangarber.getKey());

		DAO.instance.voteFor(megangarber, e, l1);
		DAO.instance.voteFor(jny2, e, l1);
		DAO.instance.voteFor(steveouting, e, l1);

		DAO.instance.voteFor(megangarber, e, l2);
		DAO.instance.voteFor(steveouting, e, l2);

		DAO.instance.voteFor(jny2, e, l3);

		DAO.instance.tally(e);
		
		out.println("tally done");
		
	}
}
