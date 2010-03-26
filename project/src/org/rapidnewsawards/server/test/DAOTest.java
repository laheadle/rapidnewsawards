package org.rapidnewsawards.server.test;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VotesIndex;

public class DAOTest extends RNATest {

	@Before
	public void setUp() throws Exception {
		super.setUp();
		MakeDataServlet.makeData(2, 30 * 60 * MakeDataServlet.ONE_SECOND, null);
	}

	void verifyEdition() {
		Edition e = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		assertNotNull(e);
		LinkedList<User> users = DAO.instance.findUsersByEdition(e);
		assertEquals(users.size(), 3);
		assertNotNull(e.getUsers());
		assertEquals(true, e.getUsers().size() > 0);
		assertEquals(true, e.getUsers().size() == users.size());	
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
		assertTrue("Self Follow", DAO.instance.isFollowing(mg, mg, null, false));
		VotesIndex vi = DAO.instance.findVotesIndexByUser(mg, null);
		assertNotNull(vi);
		vi.ensureState();
		assertNotNull(vi.votes);
		assertTrue(vi.votes.size() == 0);
	}

	@Test
	public void testfindUsers() {
		LinkedList<User> users = DAO.instance.findUsersByEdition(DAO.instance.getCurrentEdition(Name.JOURNALISM));
		assertTrue(users.size() > 1);
		assertNotNull(users.get(0).getVotes());
	}
	
	@Test
	public void testVote() {
		User mg = getUser(null);
		Link l = DAO.instance.findOrCreateLinkByURL("http://example.com");
		Link l3 = DAO.instance.findOrCreateLinkByURL("http://example2.com");
		DAO.instance.voteFor(mg, l);
		assertTrue(DAO.instance.hasVoted(mg, l));
		DAO.instance.voteFor(mg, l3);
		assertTrue(DAO.instance.hasVoted(mg, l3));
		assertTrue(DAO.instance.hasVoted(mg, l));
		Link l2 = DAO.instance.findOrCreateLinkByURL("http://bad.com");
		assertFalse(DAO.instance.hasVoted(mg, l2));
	}

	@Test
	public void testFollow() {
		User mg = getUser(null);
		User jny2 = getUser("jny2");
		// xxx make transaction
		DAO.instance.follow(mg, jny2, null, false);
		assertTrue(DAO.instance.isFollowing(mg, jny2, null, false));
		DAO.instance.follow(mg, jny2, null, true);
		assertTrue(DAO.instance.isFollowing(mg, jny2, null, true));
	}


}
