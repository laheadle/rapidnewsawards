package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.Periodical.EditionsIndex;
import org.rapidnewsawards.shared.User.JudgesIndex;


import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		makeData(10, FIVE_MINUTES);
		out.println("created 3 users");
		out.println("created 10 editions");
	}

	public final static long ONE_SECOND = 1000; 
	public final static long FIVE_MINUTES = 5 * 60 * 1000; 

	public static void makeData() {
		makeData(10, FIVE_MINUTES);
	}

	public static void makeData (int editionCount, long periodSize) {		
		// add users to first edition
		ArrayList<Edition> editions = makeEditions(editionCount, periodSize);

		Edition first = editions.get(0);
		makeEditor(first, "Megan Garber", "megangarber");
		makeEditor(first, "Josh Young", "jny2");
		makeEditor(first, "Steve Outing", "steveouting");	
	}


	public static User makeEditor(Edition e, String name, String username) {
		Objectify txn = DAO.instance.fact().beginTransaction();
		User r = new User(e, name, username);
		txn.put(r);
		User.VotesIndex vi = new User.VotesIndex(r);
		txn.put(vi);
		User.JudgesIndex ji = new User.JudgesIndex(r);
		// editors follow themselves
		ji.ensureState();
		ji.follow(r);
		txn.put(ji);
		txn.getTxn().commit();
		return r;
	}

	public static ArrayList<Edition> makeEditions(int editionCount, long periodSize) {
		final Periodical p = new Periodical("Journalism");
		Objectify txn = DAO.instance.fact().beginTransaction();
		txn.put(p);

		// create editions using a local function class
		// using arrays lets us mutate their contents from the local class method
		final int[] number = { 1 };
		final Date[] current = { new Date() };
		final class makeEd {
			final long duration;
			public makeEd(long l) { duration = l; }
			// this is called repeatedly to generate new editions
			public Edition make() { 
				current[0] = new Date(current[0].getTime() + duration); 
				return new Edition(current[0], number[0]++); 
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
			index.editions.add(e.getKey());
		}

		txn.put(index);

		p.setcurrentEditionKey(editions.get(0).getKey());
		txn.put(p);
		txn.getTxn().commit();

		return editions;
	}

}
