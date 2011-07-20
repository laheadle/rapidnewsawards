package org.rapidnewsawards.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditorInfluence;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.User;

import com.googlecode.objectify.Objectify;

public class MakeDataProductionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static DAO d = DAO.instance;
	private HttpServletRequest req;
	private PrintWriter out;
	private Periodical periodical;
	private ArrayList<Edition> editions;
	private ArrayList<User> editors;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		try {
			req = request;
			out = response.getWriter();
			editors = new ArrayList<User>();
			Date[] schedule = makeSchedule();
			makeEditions(schedule);
			makeRNAEditor();
			makeEditors();
			DAO.instance.clearCache();
		}
		catch (ParseException e) {
			throw new AssertionError();
		}
		catch (IOException e1) {
			throw new AssertionError();
		}
		
		
		Object _user = request.getAttribute("user");
		if (_user != null) {
			out.println("Logged in user: " + (User) _user);
		}
	}

	public User makeEditor(String email) {
		return makeUser(email, true);
	}

	public User makeUser(String email, boolean isEditor) {
		User u = new User(email, User.GMAIL, isEditor);
		d.ofy().put(u);
		
		if (u.isEditor != isEditor) {
			throw new IllegalStateException("bad editor status");
		}
		if (isEditor) {
			editors.add(u);
		}
		return u;		
	}

	private void makeEditors() {
		makeEditor("ftrain@gmail.com");
		makeEditor("joshuanyoung@gmail.com");
		makeEditor("ohthatmeg@gmail.com");
		makeEditor("steveouting@gmail.com");
		makeEditor("jthomas100@gmail.com");
		makeEditor("heychanders@gmail.com");
		makeEditor("nicholas.diakopoulos@gmail.com");
		makeEditor("jason.fry@gmail.com");
		makeEditor("tom.glaisyer@gmail.com");
		makeEditor("jmcquaid1@gmail.com>");
		makeEditor("scottros@gmail.com");
		
		// EditorInflucne
		for (Edition e : editions) {
			for (User ed : editors) {
				d.ofy().put(new EditorInfluence(ScoreSpace.keyFromEditionKey(e.getKey()), ed.getKey()));
			}
		}
	}

	private void makeEditions(Date[] schedule) {
		editions = new ArrayList<Edition>();
		for (int i = 0;i < schedule.length;i++) {
			editions.add(new Edition(schedule[i], i, periodical.getKey())); 
		}
		// generate keys
		DAO.instance.ofy().put(editions);		

		List<ScoreSpace> spaces = new ArrayList<ScoreSpace>();
		for (Edition e : editions) {
			ScoreRoot parent = new ScoreRoot();
			parent.id = e.getKey().getName();
			boolean pinserted = DAO.instance.ofy().put(parent) != null;
			assert(pinserted);

			ScoreSpace s = new ScoreSpace(parent.id);
			spaces.add(s);
		}
		
		boolean inserted = DAO.instance.ofy().put(spaces).keySet().size() ==
			spaces.size() && spaces.size() == editions.size();
		assert (inserted);

		periodical.setcurrentEditionKey(Edition.createKey(DAO.Editions.INITIAL));
		DAO.instance.ofy().put(periodical);
		
		TransitionTask.scheduleTransition(editions.get(0));
	}

	public Date[] makeSchedule() throws ParseException {
		int editionCount = 12;
		periodical = MakeDataServlet.makePeriodical(editionCount);
		
		SimpleDateFormat df = new SimpleDateFormat("EEE M/d h:mm a 'ET' yyyy");
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"));

		Date[] ends = {
				df.parse("Wed 7/27 12:00 PM ET 2011"),
				df.parse("Fri 7/29 12:00 PM ET 2011"),
				df.parse("Mon 8/1 12:00 PM ET 2011"),
				df.parse("Wed 8/3 12:00 PM ET 2011"),
				df.parse("Fri 8/5 12:00 PM ET 2011"),
				df.parse("Mon 8/8 12:00 PM ET 2011"),
				df.parse("Wed 8/10 12:00 PM ET 2011"),
				df.parse("Fri 8/12 12:00 PM ET 2011"),
				df.parse("Mon 8/15 12:00 PM ET 2011"),
				df.parse("Wed 8/17 12:00 PM ET 2011"),
				df.parse("Fri 8/19 12:00 PM ET 2011"),
				df.parse("Mon 8/22 12:00 PM ET 2011")
		};
		
		if (ends.length != editionCount) { throw new IllegalStateException(); }
		out.println(String.format("making %d editions", editionCount));
		return ends;
	}
	
	private static void makeRNAEditor() {
		User rna = new User(User.RNA_EDITOR_EMAIL, User.GMAIL, true);
		Objectify txn = DAO.instance.fact().beginTransaction();
		txn.put(rna);
		txn.getTxn().commit();
	}


}
