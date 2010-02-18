package org.rapidnewsawards.server.test;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Reader;

public class MakeDataTest extends RNATest {

	@Test
	public void testMakeEditions() {
		// make editions
		ArrayList<Edition> editions = MakeDataServlet.makeEditions(2, 60 * 60 * MakeDataServlet.ONE_SECOND);
		assertEquals(editions.size(), 2);
		Edition first = DAO.instance.getCurrentEdition("Journalism");
		assertNotNull(first.getKey());
		assertEquals(first.getKey(), editions.get(0).getKey());

		// make reader
		Reader r = new Reader(first, "Megan Garber", "megangarber");
		assertNotNull(r.parent);
		assertEquals(r.parent, first.getKey());
		assertNull(r.id);
		DAO.instance.ofy().put(r);
		assertNotNull(r.getKey());
		assertNotNull(r.getKey().getParent());

		Reader.VotesIndex vi = new Reader.VotesIndex(r);
		DAO.instance.ofy().put(vi);
		Reader.JudgesIndex ji = new Reader.JudgesIndex(r);
		DAO.instance.ofy().put(ji);

		//Reader r2 = MakeDataServlet.makeReader(first, "Megan Garber", "megangarber");
		//assertNotNull(r2.getKey().getParent());

		LinkedList<Reader> readers = DAO.instance.findReadersByEdition(first);
		assertEquals(readers.size(), 1);
		assertEquals("megangarber", readers.get(0).getUsername());
	}



}
