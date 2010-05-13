package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import java.util.Iterator;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.ScoredLink;
import org.rapidnewsawards.shared.User;

public class VoteTransitionTest extends EditionTransitionTest {

	@Test
	public void testVotes() {
		Edition e1 = DAO.instance.getCurrentEdition(Name.JOURNALISM);		
		User mg = DAO.instance.findUserByUsername("megangarber");

		Link l = DAO.instance.findOrCreateLinkByURL("http://example.com", mg.getKey());
		assertEquals(DAO.instance.getLatestUser_Links(e1).size(), 0);
		DAO.instance.voteFor(mg, e1, l);
		assertTrue(DAO.instance.hasVoted(mg, e1, l));
		assertEquals("User_Link Exists", DAO.instance.getLatestUser_Links(e1).size(), 1);
		Edition e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		assertFalse(DAO.instance.hasVoted(mg, e2, l));

		for(Perishable p : module.mockPs)
			verify(p);
	}


	@Test
	public void testTally() {
		DAO.instance.getCurrentEdition(Name.JOURNALISM);		
		Edition e2 = DAO.instance.getRawEdition(Name.JOURNALISM, 1, null);

		User mg = DAO.instance.findUserByUsername("megangarber");
		User jny2 = DAO.instance.findUserByUsername("jny2");
		User steveouting = DAO.instance.findUserByUsername("steveouting");

		// megan follows josh 
		DAO.instance.doSocial(mg.getKey(), jny2.getKey(), e2, null, true);

		// megan and josh follow steve
		DAO.instance.doSocial(mg.getKey(), steveouting.getKey(), e2, null, true);
		DAO.instance.doSocial(jny2.getKey(), steveouting.getKey(), e2, null, true);
		
		e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		Link l1 = DAO.instance.findOrCreateLinkByURL("http://example.com", mg.getKey());
		Link l2 = DAO.instance.findOrCreateLinkByURL("http://example2.com", mg.getKey());
		Link l3 = DAO.instance.findOrCreateLinkByURL("http://example3.com", mg.getKey());
		
		DAO.instance.voteFor(jny2, e2, l1);

		DAO.instance.voteFor(steveouting, e2, l2);

		DAO.instance.voteFor(jny2, e2, l3);
		DAO.instance.voteFor(steveouting, e2, l3);

		DAO.instance.tally(e2);
		
		Iterator<ScoredLink> iter = DAO.instance.getScoredLinks(e2).iterator();
		
		assertTrue(iter.hasNext());
		ScoredLink sl1 = iter.next();
		assertEquals(sl1.score, 3);
		assertEquals(sl1.link, l3.getKey());
		
		assertTrue(iter.hasNext());
		ScoredLink sl2 = iter.next();
		assertEquals(sl2.score, 2);
		assertEquals(sl2.link, l2.getKey());

		assertTrue(iter.hasNext());
		ScoredLink sl3 = iter.next();
		assertEquals(sl3.score, 1);
		assertEquals(sl3.link, l1.getKey());
		
		for(Perishable p : module.mockPs)
			verify(p);
	}


}
