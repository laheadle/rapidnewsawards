package org.rapidnewsawards.server.test;

import java.util.ArrayList;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Reader;

public class MakeDataTest extends RNATest {

	@Test
	public void testMakeData() {
		ArrayList<Edition> editions = MakeDataServlet.makeEditions(2, MakeDataServlet.ONE_SECOND);
		assertEquals(editions.size(), 2);
		Edition first = editions.get(0);
		assertNotNull(first.getKey());
		Reader r = new Reader(first, "Megan Garber", "megangarber");
		assertNotNull(r.parent);
		assertEquals(r.parent, first.getKey());
		assertNull(r.id);
		DAO.instance.ofy().put(r);
		assertNotNull(r.getKey());
		assertNotNull(r.getKey().getParent());
		
/*		Reader.VotesIndex vi = new Reader.VotesIndex(r);
		DAO.instance.ofy().put(vi);
		Reader.JudgesIndex ji = new Reader.JudgesIndex(r);
		DAO.instance.ofy().put(ji);

		Reader r = MakeDataServlet.makeReader(first, "Megan Garber", "megangarber");
		assertNotNull(r.getKey().getParent());
		*/
	}
	


}
