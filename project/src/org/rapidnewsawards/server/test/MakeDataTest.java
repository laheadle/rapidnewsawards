package org.rapidnewsawards.server.test;

import java.util.ArrayList;
import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class MakeDataTest extends RNATest {

	@Test
	public void testMakeEditions() {
		// make editions
		ArrayList<Edition> editions = MakeDataServlet.makeEditions(3, 60 * 60 * MakeDataServlet.ONE_SECOND);
		assertEquals(editions.size(), 3);
		assertEquals(DAO.instance.getNumEditions(Name.JOURNALISM), 3);
		Edition first = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		assertNotNull(first.getKey());
		assertEquals(first.getKey(), editions.get(0).getKey());

		// make user
		User u = new User("Megan Garber", "megangarber", false);
		assertNull(u.id);
		DAO.instance.ofy().put(u);
		assertNotNull(u.getKey());
	}



}
