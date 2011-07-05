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
import com.googlecode.objectify.Objectify;


@SuppressWarnings("serial")
public class DoSomethingServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
	private static HttpServletResponse response;
	public static DAO d = DAO.instance;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp)
	throws ServletException, IOException {
		User rna = new User("__rnaEditor@gmail.com", "gmail.com", true);
		rna.id = 1L;
		d.ofy().put(rna);
		if (rna.id != 1L) {
			throw new IllegalStateException("bad rna ed");
		}
		if (d.ofy().get(User.getRNAEditor()).id != 1L) {
			throw new IllegalStateException("rna ed not stored");
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {}
		makeUser("jthomas100@gmail.com", true);
	}
	public static User makeUser(String email, boolean isEditor) {
		if (d.ofy().get(User.getRNAEditor()).id != 1L) {
			throw new IllegalStateException("rna ed not stored yet!");
		}

		User u = new User(email, "gmail.com", isEditor);
		u.id = null;
		d.ofy().put(u);
		
		if (u.id == 1L) {
			throw new IllegalStateException("WTF?");
		}
		return u;		
	}
}
