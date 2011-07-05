package org.rapidnewsawards.server;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.User;

import com.googlecode.objectify.Objectify;


@SuppressWarnings("serial")
public class DoSomethingServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
	public static DAO dao = DAO.instance;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp)
	throws ServletException, IOException {
		makeRoot();
		makeRnaEditor();
		makeUser("jjj@example.com");
		resp.getWriter().write("ok");
	}

	private void makeRoot() {
		final Root root = new Root();
		root.id = 1L;
		dao.ofy().put(root);
	}

	private void makeRnaEditor() {
		User rna = new User("__rnaEditor@gmail.com", "gmail.com", true);
		//rna.id = 1L;
		log.info(String.format("rna 1: %s", rna));
		dao.ofy().put(rna);
		log.info(String.format("rna 2: %s", rna));
	}
	public static void makeUser(String email) {
		User rna = dao.users.getRNAUser();
		log.info(String.format("rna 3: %s", rna));
		User u = new User(email, "gmail.com", true);
		log.info(String.format("u 1: %s", u));
		dao.ofy().put(u);
		log.info(String.format("u 2: %s", u));		
	}
}
