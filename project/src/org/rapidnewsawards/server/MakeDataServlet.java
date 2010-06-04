package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.shared.Cell;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.Root;
import org.rapidnewsawards.shared.User;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.*;

public class MakeDataServlet extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		Cell<Integer> numUsers = new Cell<Integer>(null);
		int numEditions = 3;
		makeData(numEditions, 1 * ONE_MINUTE, numUsers);
		out.println("created " + numUsers.value + " users");
		out.println("created " + numEditions + " editions");
	}

	public static boolean testing = false;
	
	public final static long ONE_SECOND = 1000; 
	public final static long ONE_MINUTE = 60 * 1000; 	
	public final static long FIVE_MINUTES = 5 * ONE_MINUTE; 

	public static void makeData (int editionCount, long periodSize, Cell<Integer> numUsers) {		
		// add users to first edition
		makeEditions(editionCount, periodSize);

		makeEditor("megangarber@gmail.com");
		makeEditor("jny2@gmail.com");
		makeEditor("steveouting@gmail.com");	
		
		if (numUsers != null)
			numUsers.value = new Integer(3);
	}


	public static User makeEditor(String email) {
		Objectify txn = DAO.instance.fact().beginTransaction();
		User u = new User(email, "gmail.com");
		txn.put(u);
		//DAO.instance.doSocial(u.getKey(), u.getKey(), e, txn, true);
		txn.getTxn().commit();
		return u;
	}

	 
	public static ArrayList<Edition> makeEditions(int editionCount, long periodSize) {
		final Root root = new Root();
		root.id = 1L;
		DAO.instance.ofy().put(root);
		
		final Periodical p = new Periodical(Name.JOURNALISM, new Key<Root>(Root.class, 1L));
		Objectify o = DAO.instance.ofy();
		o.put(p);


		// create editions using a local function class
		// using arrays lets us mutate their contents from the local class method
		final int[] number = { 0 };
		final Date[] current = { new Date() };
		final class makeEd {
			final long duration;
			final Periodical p;
			
			public makeEd(long l, Periodical p) { duration = l; this.p = p;}
			// this is called repeatedly to generate new editions
			public Edition make() { 
				current[0] = new Date(current[0].getTime() + duration); 
				return new Edition(current[0], number[0]++, p.getKey()); 
			}
		}

		ArrayList<Edition> editions = new ArrayList<Edition>();
		for(int i1 = 0;i1 < editionCount;i1++) {
			editions.add(new makeEd(periodSize, p).make());
		}

		// generate keys
		DAO.instance.ofy().put(editions);		
		
		// make transition tasks
		if (!testing) {
			for (Edition e : editions) {
				Queue queue = QueueFactory.getDefaultQueue();
				queue.add(url("/tasks/transition").param("fromEdition", e.id)
						.etaMillis(e.end.getTime()).method(TaskOptions.Method.GET));
			}
		}
		
		p.setcurrentEditionKey(editions.get(0).getKey());
		o.put(p);

		return editions;
	}

}
