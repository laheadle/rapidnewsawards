package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.EntityNotFoundException;

import rapidnews.shared.Link;
import rapidnews.shared.Reader;

public class VoteServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Reader r = null;

		try {
			r = DAO.instance.findReaderByUsername(request.getParameter("username"), false);
		} catch (EntityNotFoundException e1) {
			assert(false); // only thrown when fillrefs = true
		}
		
		out.println("got reader " + r.toString()); // xxx

		// TODO broken on some complex hrefs
		Link l = DAO.instance.findOrCreateLinkByURL(request.getParameter("href"));
		
		try	{
			DAO.instance.voteFor(r, l);
		}
		// TODO handle malformed urls
		catch (IllegalArgumentException e) {
			out.println(e);//"vote already counted");	
			return;
		}
		
		out.println("vote counted");
		
	}
}
