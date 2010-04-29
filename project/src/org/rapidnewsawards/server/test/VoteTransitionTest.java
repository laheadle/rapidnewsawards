package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class VoteTransitionTest extends EditionTransitionTest {

	@Test
	public void testVotes() {
		Edition e1 = DAO.instance.getCurrentEdition(Name.JOURNALISM);		
		User mg = DAO.instance.findUserByUsername("megangarber");

		Link l = DAO.instance.findOrCreateLinkByURL("http://example.com");
		assertEquals(DAO.instance.getLatestUser_Links(e1).size(), 0);
		DAO.instance.voteFor(mg, e1, l);
		assertTrue(DAO.instance.hasVoted(mg, e1, l));
		assertEquals("User_Link Exists", DAO.instance.getLatestUser_Links(e1).size(), 1);
		Edition e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		assertFalse(DAO.instance.hasVoted(mg, e2, l));

		for(Perishable p : module.mockPs)
			verify(p);
	}


}
