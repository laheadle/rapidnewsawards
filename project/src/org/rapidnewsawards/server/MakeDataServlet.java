package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
	public static PrintWriter out;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		out = response.getWriter();
		Cell<Integer> numUsers = new Cell<Integer>(null);
		int numEditions = 5;
		try {
			makeData(numEditions, 8 * FIVE_MINUTES, numUsers);
		} catch (ParseException e) {
			e.printStackTrace(out);
		}
		out.println("created " + numUsers.value + " users");
		out.println("created " + numEditions + " editions");
	}

	public static boolean testing = false;
	
	public final static long ONE_SECOND = 1000; 
	public final static long ONE_MINUTE = 60 * 1000; 	
	public final static long ONE_HOUR = 60 * ONE_MINUTE; 		
	public final static long FIVE_MINUTES = 5 * ONE_MINUTE; 

	public static void makeData (int editionCount, long periodSize, Cell<Integer> numUsers) throws ParseException {		
		// add users to first edition
		makeEditions(editionCount, periodSize);

		makeRNAEditor();
		makeEditor("ohthatmeg@gmail.com");
		makeEditor("jthomas100@gmail.com");
		makeEditor("joshuanyoung@gmail.com");
		makeEditor("laheadle@gmail.com");		
		makeEditor("steveouting@gmail.com");	
		
		if (numUsers != null)
			numUsers.value = new Integer(5);
	}


	private static void makeRNAEditor() {
		User rna = new User("__rnaEditor@gmail.com", "gmail.com", true);
		rna.id = 1L;
		DAO.instance.ofy().put(rna);
	}


	public static User makeEditor(String email) {
		Objectify txn = DAO.instance.fact().beginTransaction();
		User u = new User(email, "gmail.com", true);
		txn.put(u);
		//DAO.instance.doSocial(u.getKey(), u.getKey(), e, txn, true);
		txn.getTxn().commit();
		return u;
	}

	 
	public static ArrayList<Edition> makeEditions(int editionCount, long periodSize) throws ParseException {
		final Root root = new Root();
		root.id = 1L;
		DAO.instance.ofy().put(root);
		
		final Periodical p = new Periodical(Name.AGGREGATOR_NAME, new Key<Root>(Root.class, 1L));
		Objectify o = DAO.instance.ofy();
		o.put(p);


		// create editions using a local function class
		// using arrays lets us mutate their contents from the local class method
		final int[] number = { 0 };
		
		Date start = new Date(); //new SimpleDateFormat ("yyyy-MM-dd hh:mma z").parse("2010-06-15 10:07am ET");
		//out.println("start: " + start.toString());
	    
		final Date[] current = { start };
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
			Queue queue = QueueFactory.getDefaultQueue();
			for (Edition e : editions) {
				queue.add(url("/tasks/transition").param("fromEdition", e.id)
						.etaMillis(e.end.getTime()).method(TaskOptions.Method.GET));
			}
			
			// adds itself again every few minutes
			queue.add(url("/tasks/tally").etaMillis(new Date().getTime() + 2 * ONE_MINUTE).method(TaskOptions.Method.GET));
		}
		
		p.setcurrentEditionKey(editions.get(0).getKey());
		o.put(p);

		return editions;
	}

}
