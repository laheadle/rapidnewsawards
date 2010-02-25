package org.rapidnewsawards.server.test;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Config;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.User;

public class MakeDataTest extends RNATest {

	@Test
	public void testMakeEditions() {
		// make editions
		ArrayList<Edition> editions = MakeDataServlet.makeEditions(2, 60 * 60 * MakeDataServlet.ONE_SECOND);
		assertEquals(editions.size(), 2);
		Edition first = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
		assertNotNull(first.getKey());
		assertEquals(first.getKey(), editions.get(0).getKey());

		// make user
		User r = new User(first, "Megan Garber", "megangarber");
		assertNotNull(r.parent);
		assertEquals(r.parent, first.getKey());
		assertNull(r.id);
		DAO.instance.ofy().put(r);
		assertNotNull(r.getKey());
		assertNotNull(r.getKey().getParent());

		User.VotesIndex vi = new User.VotesIndex(r);
		DAO.instance.ofy().put(vi);
		User.JudgesIndex ji = new User.JudgesIndex(r);
		DAO.instance.ofy().put(ji);

		LinkedList<User> users = DAO.instance.findUsersByEdition(first);
		assertEquals(users.size(), 1);
		assertEquals("megangarber", users.get(0).getUsername());
	}



}
