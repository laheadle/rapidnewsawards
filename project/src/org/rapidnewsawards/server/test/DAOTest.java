package org.rapidnewsawards.server.test;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Config;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.JudgesIndex;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VotesIndex;

public class DAOTest extends RNATest {

	@Before
	public void setUp() throws Exception {
		super.setUp();
		MakeDataServlet.makeData(2, 30 * 60 * MakeDataServlet.ONE_SECOND);
	}

	void verifyEdition() {
		Edition e = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
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
		JudgesIndex ji = DAO.instance.findJudgesIndexByUser(mg, null);
		assertNotNull(ji);
		assertTrue(ji.judges.size() == 1);
		assertTrue("Self Following", ji.judges.get(0).equals(mg.getKey()));
		VotesIndex vi = DAO.instance.findVotesIndexByUser(mg, null);
		assertNotNull(vi);
		vi.ensureState();
		assertNotNull(vi.votes);
		assertTrue(vi.votes.size() == 0);
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
		DAO.instance.follow(mg, jny2);
		assertTrue(DAO.instance.isFollowing(mg, jny2, null));
	}


}
