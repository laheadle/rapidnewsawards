package org.rapidnewsawards.server.test;

import org.junit.Before;
import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class DAOTest extends RNATest {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MakeDataServlet.makeData(2, 30 * 60 * MakeDataServlet.ONE_SECOND, null);
	}

	void verifyEdition() {
		Edition e = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		assertNotNull(e);
	}

	@Test
	public void testEditions() {
		verifyEdition();
	}

	// TODO disallow voting in expired editions


	@Test
	public void testUser() {
		User mg = getUser(null);
		assertNotNull(mg);
		assertEquals(mg.getUsername(), "megangarber");
	}
	
	@Test
	public void testVote() {
		User mg = getUser(null);
		Edition e = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		Link l = DAO.instance.findOrCreateLinkByURL("http://example.com");
		Link l3 = DAO.instance.findOrCreateLinkByURL("http://example2.com");
		DAO.instance.voteFor(mg, e, l);
		assertTrue(DAO.instance.hasVoted(mg, e, l));
		DAO.instance.voteFor(mg, e, l3);
		assertTrue(DAO.instance.hasVoted(mg, e, l3));
		assertTrue(DAO.instance.hasVoted(mg, e, l));
		Link l2 = DAO.instance.findOrCreateLinkByURL("http://bad.com");
		assertFalse(DAO.instance.hasVoted(mg, e, l2));
	}


}
