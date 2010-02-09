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
import rapidnews.shared.Periodical.EditionsIndex;
import rapidnews.shared.Reader;

import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		int editions = makeData();
		out.println("created 3 readers");
		out.println("created " + editions + " editions");
	}

	public final static long ONE_SECOND = 1000; 
	public final static long FIVE_MINUTES = 5 * 60 * 1000; 
	
	public static int makeData() {
		return makeData(10, FIVE_MINUTES);
	}
	
	public static int makeData(int editionCount, long periodSize) {
		Reader mg = new Reader("Megan Garber", "megangarber");
		Reader jy = new Reader("Josh Young", "jny2");
		Reader so = new Reader("Steve Outing", "steveouting");
		DAO.instance.ofy().put(Arrays.asList(mg, jy, so));
		DAO.instance.ofy().put(new Reader.VotesIndex(mg));
		DAO.instance.ofy().put(new Reader.VotesIndex(jy));
		DAO.instance.ofy().put(new Reader.VotesIndex(so));

		final Periodical p = new Periodical("Journalism");
		Objectify txn = DAO.instance.fact().beginTransaction();
		txn.put(p);

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
				return new Edition(current[0], i[0]++); 
			}
		}

		ArrayList<Edition> editions = new ArrayList<Edition>();
		for(int i1 = 0;i1 < editionCount;i1++) {
			editions.add(new makeEd(periodSize).make());
		}

		// generate keys
		// don't use the same transaction -- different entity groups
		DAO.instance.ofy().put(editions);

		EditionsIndex index = new EditionsIndex(p, editions);
		for(Edition e : editions) {
			index.editions.add(e.getOKey());
		}
		
		txn.put(index);
		
		p.setcurrentEditionKey(editions.get(0).getOKey());
		txn.put(p);
		txn.getTxn().commit();
		return (i[0] - 1);
	}
}
