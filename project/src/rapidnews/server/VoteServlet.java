package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Link;

public class VoteServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		PersistenceManager pm = PMF.getPersistenceManager();

		Reader r = Reader.findByUsername(request.getParameter("username"));
		out.println("got reader " + r.toString()); // xxx

		Transaction tx = pm.currentTransaction();

		try {
			tx.begin();
			Link l = new Link(request.getParameter("href"));
			r.voteFor(l);
			pm.makePersistent(r);
			tx.commit();
			out.println("vote counted");
		} finally {
			if (tx.isActive()) {
				tx.rollback();
				out.println("FAIL: rolled back");
			}
			pm.close();
		}
	}
}
