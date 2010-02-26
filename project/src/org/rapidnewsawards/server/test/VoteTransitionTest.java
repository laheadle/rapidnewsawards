package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.Config;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.User;

public class VoteTransitionTest extends EditionTransitionTest {

	@Test
	public void testVotes() {
		Edition e1 = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);		
		User mg = DAO.instance.findUserByEditionAndUsername(e1, "megangarber");

		Link l = DAO.instance.findOrCreateLinkByURL("http://example.com");
		DAO.instance.voteFor(mg, l);
		assertTrue(DAO.instance.hasVoted(mg, l));

		Edition e2 = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
		mg = DAO.instance.findUserByEditionAndUsername(e2, "megangarber");

		assertFalse(DAO.instance.hasVoted(mg, l));

		for(Perishable p : module.mockPs)
			verify(p);
	}


}
