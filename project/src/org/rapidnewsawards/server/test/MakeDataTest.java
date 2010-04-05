package org.rapidnewsawards.server.test;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VotesIndex;

public class MakeDataTest extends RNATest {

	@Test
	public void testMakeEditions() {
		// make editions
		ArrayList<Edition> editions = MakeDataServlet.makeEditions(2, 60 * 60 * MakeDataServlet.ONE_SECOND);
		assertEquals(editions.size(), 2);
		assertEquals(DAO.instance.getNumEditions(Name.JOURNALISM), 2);
		Edition first = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		assertNotNull(first.getKey());
		assertEquals(first.getKey(), editions.get(0).getKey());

		// make user
		User u = new User(first, "Megan Garber", "megangarber", false);
		assertNotNull(u.parent);
		assertEquals(u.parent, first.getKey());
		assertNull(u.id);
		DAO.instance.ofy().put(u);
		assertNotNull(u.getKey());
		assertNotNull(u.getKey().getParent());

		VotesIndex vi = new VotesIndex(u);
		DAO.instance.ofy().put(vi);

		LinkedList<User> users = DAO.instance.findUsersByEdition(first);
		assertEquals(users.size(), 1);
		assertEquals("megangarber", users.get(0).getUsername());
	}



}
