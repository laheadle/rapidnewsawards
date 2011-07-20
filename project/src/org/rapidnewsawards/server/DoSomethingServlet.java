package org.rapidnewsawards.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withPayload;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.ShadowUser;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.server.SocialTask.Task;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;


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
		else if (fun.equals("makeUsers")) {
			User u = null;
			u = new User("a@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime() + 500, "a");
			u = new User("b@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime() + 2500, "b");
			u = new User("c@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime() + 5000, "c");
			u = new User("d@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime() + 7500, "d");
			u = new User("e@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime()+ 10000, "e");
			u = new User("f@gmail.com", User.GMAIL, false);
			DAO.instance.ofy().put(u);
			welcome(u, new Date().getTime() + 12500, "alkjakldjfkjakdjfkljadslkfja;kdjf;akjdsf;kjad;fkjadsfa kjljlkjj");
			resp.getWriter().write("ok");			
		}
		else {
			resp.getWriter().write("huh??");			
		}
	}

	private void welcome(final User u, long time, final String nickname) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(withPayload(new Task() {
			private static final long serialVersionUID = 1L;
			@Override
			public void rnaRun() throws RNAException {
				DAO.instance.users.welcomeUser(u, nickname, "true", "example.com");
			}
			@Override
			public String fun() {
				return "fakeWelcome";
			}		
		}).etaMillis(time));
	}
}