package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.RNAException;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.Root;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.VoteResult;

import com.googlecode.objectify.Objectify;

public class MakeDataServlet extends HttpServlet {
	private static final int FIRST_EDITION = 0;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static PrintWriter out;
	public static boolean doFollow = false;
	public static boolean doTransition = false;	
	public static DAO d = DAO.instance;
	private static int numLinks = 0;
	
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
				
				try { 
					DAO.instance.transition.doTransition(0);
				} catch (ConcurrentModificationException cme) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					DAO.instance.transition.doTransition(0);
				}
			}
			
			if (numLinks > 0) {
				makeLinks();
			}
		} catch (ParseException e) {
			e.printStackTrace(out);
		} catch (RNAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println("created " + numUsers.value + " users");
		out.println("created " + numEditions + " editions");
	}

	private void makeLinks() throws RNAException {
		Edition current = d.editions.getEdition(1);
		User jq = d.users.findUserByLogin("johnqpublic@gmail.com", "gmail.com");
		for (int i = 0;i < numLinks;i++) {
			VoteResult vr = d.editions.submitStory("http://www.example" + i + ".com", 
					"example story", current.getKey(), jq);
			if (!vr.returnVal.s.equals(Response.SUCCESS.s)) {
				DAO.log.warning(vr.returnVal.toString());
			}
		}
	}

	public static void welcome(User u, String nickname, int donation) throws RNAException {
		User olduser = d.user;
		d.user = u;
		d.users.welcomeUser(nickname, donation * 100);		
		d.user = olduser;
	}
	public static void makeData (int editionCount, long periodSize, Cell<Integer> numUsers) 
	throws ParseException, RNAException {		
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
				Response r = d.social.doSocial(jq.getKey(), true);
				assert(r.equals(Response.ABOUT_TO_FOLLOW));
		}
	}


	private static void makeRNAEditor() {
		User rna = new User("__rnaEditor@gmail.com", "gmail.com", true);
		rna.id = 1L;
		DAO.instance.ofy().put(rna);
	}


	public static User makeUser(String email, boolean isEditor) {
		Objectify o = DAO.instance.ofy();
		User u = new User(email, "gmail.com", isEditor);
		o.put(u);
		return u;		
	}

	public static User makeEditor(String email) {
		return makeUser(email, true);
	}

	public static User makeJudge(String email) {
		return makeUser(email, false);
	}

	 
	public static ArrayList<Edition> makeEditions(int editionCount, long periodSize) 
	throws ParseException {
		Objectify o = DAO.instance.ofy();
		final Root root = new Root();
		root.id = 1L;
		o.put(root);
		
		final Periodical p = new Periodical(Name.AGGREGATOR_NAME);
		p.numEditions = editionCount;
		assert(o.put(p) != null);

		assert(d.getPeriodical() != null);
		
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
				return e;
			}
		}

		ArrayList<Edition> editions = new ArrayList<Edition>();
		for(int i1 = 0;i1 < editionCount;i1++) {
			editions.add(new makeEd(periodSize, p).make());
		}

		// generate keys
		DAO.instance.ofy().put(editions);		

		List<ScoreSpace> spaces = new ArrayList<ScoreSpace>();
		for (Edition e : editions) {
			ScoreRoot parent = new ScoreRoot();
			parent.id = e.getKey().getName();
			boolean pinserted = o.put(parent) != null;
			assert(pinserted);

			ScoreSpace s = new ScoreSpace(parent.id);
			spaces.add(s);
		}
		
		boolean inserted = DAO.instance.ofy().put(spaces).keySet().size() ==
			spaces.size() && spaces.size() == editions.size();
		assert (inserted);

		// make transition tasks
		if (!testing) {
			for (Edition e : editions) {
				if (e.number == 0 && doTransition) {
					continue;
				}
				TransitionTask.scheduleTransition(e);
			}
			
		}
		
		p.setcurrentEditionKey(Edition.createKey(FIRST_EDITION));
		o.put(p);

		return editions;
	}

}
