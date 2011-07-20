package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ShadowUser;
import org.rapidnewsawards.core.User;


@SuppressWarnings("serial")
public class DoSomethingServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(DoSomethingServlet.class.getName());
	public static DAO dao = DAO.instance;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse resp)
	throws ServletException, IOException {
		String fun = request.getParameter("fun");
		if (fun.equals("become")) {
			String whom = request.getParameter("whom");
			if (whom.equals("clear")) {
				DAO.instance.ofy().delete(DAO.instance.ofy().query(ShadowUser.class));
				resp.getWriter().write("cleared");
			}
			else {
				User user = dao.users.findUserByLogin(whom, User.GMAIL);
				if (user == null) {
					resp.getWriter().write("failed");
				}
				else {
					ShadowUser su = new ShadowUser(user.getKey());
					DAO.instance.ofy().put(su);
					resp.getWriter().write("ok");
				}
			}
		}
		else {
			resp.getWriter().write("huh??");			
		}
	}
}