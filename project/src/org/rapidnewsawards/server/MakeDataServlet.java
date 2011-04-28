package org.rapidnewsawards.server;

import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.url;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
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
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.Root;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VoteResult;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static PrintWriter out;
	public static boolean doFollow = false;
	public static boolean doTransition = false;	
	public static DAO d = DAO.instance;
	private static Integer numLinks = 0;
	
	// don't set up tasks if testing (set by test case)
	public static boolean testing = false;
	
	public final static long ONE_SECOND = 1000; 
	public final static long ONE_MINUTE = 60 * 1000; 	
	public final static long ONE_HOUR = 60 * ONE_MINUTE; 		
	public final static long FIVE_MINUTES = 5 * ONE_MINUTE; 

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		out = response.getWriter();
		Cell<Integer> numUsers = new Cell<Integer>(null);
		int numEditions = 5;
		try {
			try {
				doFollow = new Boolean(request.getParameter("doFollow"));
			}
			catch(Exception e) {}

			int minutes = 5;
			try {
				minutes = new Integer(request.getParameter("periodSize"));
			}
			catch(Exception e) {}
			try {
				doTransition = new Boolean(request.getParameter("doTransition"));
			}
			catch(Exception e) {}
			try {
				numLinks = new Integer(request.getParameter("numLinks"));
			}
			catch(Exception e) {}

			makeData(numEditions, minutes * ONE_MINUTE, numUsers);

			if (doTransition) {
				DAO.instance.transition.doTransition(Name.AGGREGATOR_NAME, 0, null);
			}
			
			if (numLinks > 0) {
				makeLinks();
			}
		} catch (ParseException e) {
			e.printStackTrace(out);
		}
		out.println("created " + numUsers.value + " users");
		out.println("created " + numEditions + " editions");
	}

	private void makeLinks() {
		Edition current = d.editions.getEdition(Name.AGGREGATOR_NAME, 1, null);
		User jq = d.users.findUserByLogin("johnqpublic@gmail.com", "gmail.com");
		for (int i = 0;i < numLinks;i++) {
			VoteResult vr = d.editions.submitStory("http://www.example" + i + ".com", 
					"example story", current, jq);
			if (!vr.returnVal.s.equals(Return.SUCCESS.s)) {
				DAO.log.warning(vr.returnVal.toString());
			}
		}
		TallyTask.scheduleImmediately();
	}

	public static void welcome(User u, String nickname, int donation) {
		User olduser = d.user;
		d.user = u;
		d.users.welcomeUser(nickname, donation * 100);		
		d.user = olduser;
	}
	public static void makeData (int editionCount, long periodSize, Cell<Integer> numUsers) throws ParseException {		
		// add users to first edition
		makeEditions(editionCount, periodSize);

		makeRNAEditor();
		makeEditor("jthomas100@gmail.com");
		makeEditor("joshuanyoung@gmail.com");
		makeEditor("ohthatmeg@gmail.com");
		User jq = makeJudge("johnqpublic@gmail.com");
		User lyn = makeEditor("laheadle@gmail.com");		
		makeEditor("steveouting@gmail.com");
		welcome(lyn, "lyn", 5000);
		welcome(jq, "john q public", 5000);

		if (numUsers != null)
			numUsers.value = new Integer(6);
		
		if (doFollow) {
			d.user = lyn;
			d.social.doSocial(jq, true);
		}
	}


	private static void makeRNAEditor() {
		User rna = new User("__rnaEditor@gmail.com", "gmail.com", true);
		rna.id = 1L;
		DAO.instance.ofy().put(rna);
	}


	public static User makeUser(String email, boolean isEditor) {
		Objectify txn = DAO.instance.fact().beginTransaction();
		User u = new User(email, "gmail.com", isEditor);
		txn.put(u);
		//DAO.instance.doSocial(u.getKey(), u.getKey(), e, txn, true);
		txn.getTxn().commit();
		return u;		
	}

	public static User makeEditor(String email) {
		return makeUser(email, true);
	}

	public static User makeJudge(String email) {
		return makeUser(email, false);
	}

	 
	public static ArrayList<Edition> makeEditions(int editionCount, long periodSize) throws ParseException {
		final Root root = new Root();
		root.id = 1L;
		DAO.instance.ofy().put(root);
		
		final Periodical p = new Periodical(Name.AGGREGATOR_NAME, new Key<Root>(Root.class, 1L));
		Objectify o = DAO.instance.ofy();
		p.numEditions = editionCount;
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
				Edition e = new Edition(current[0], number[0]++, p.getKey()); 
				e.revenue = p.balance / p.numEditions;
				return e;
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
				if (e.number == 0 && doTransition) {
					continue;
				}
				queue.add(url("/tasks/transition").param("fromEdition", e.id)
						.etaMillis(e.end.getTime()).method(TaskOptions.Method.GET));
			}
			
		}
		
		p.setcurrentEditionKey(editions.get(0).getKey());
		o.put(p);

		return editions;
	}

}
