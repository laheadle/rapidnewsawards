package rapidnews.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rapidnews.shared.Edition;
import rapidnews.shared.Periodical;
import rapidnews.shared.Reader;

import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();

		Reader mg = new Reader("Megan Garber", "megangarber");
		Reader jy = new Reader("Josh Young", "jny2");
		Reader so = new Reader("Steve Outing", "steveouting");
		DAO.instance.ofy().put(Arrays.asList(mg, jy, so));
		out.println("created 3 readers");

		final Periodical p = new Periodical("Journalism");
		Objectify ofy = DAO.instance.fact().beginTransaction();
		ofy.put(p);

		// create editions using a local function class
		// using arrays lets us mutate their contents from the local class method
		final int[] i = { 1 };
		final Date[] current = { new Date() };
		final class makeEd {
			final long duration;
			public makeEd(long l) { duration = l; }
			// this is called repeatedly to generate new editions
			public Edition make() { 
				current[0] = new Date(current[0].getTime() + duration); 
				return new Edition(p, current[0], i[0]++); 
			}
		}

		ArrayList<Edition> editions = new ArrayList<Edition>();
		final long FIVE_MINUTES = 5 * 60 * 1000; 
		for(int i1 = 0;i1 < 10;i1++) {
			editions.add(new makeEd(FIVE_MINUTES).make());
		}

		DAO.instance.ofy().put(editions);


		p.setcurrentEditionKey(editions.get(0).getOKey());
		ofy.put(p);
		ofy.getTxn().commit();


		out.println("created 4 editions");
	}
}
