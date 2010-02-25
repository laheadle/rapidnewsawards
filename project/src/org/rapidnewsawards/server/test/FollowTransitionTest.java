package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.shared.Config;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Perishable;
import org.rapidnewsawards.shared.User;

public class FollowTransitionTest extends EditionTransitionTest {

		
	@Test
	public void testFollows() {
		Edition e1 = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
		
		User mg = DAO.instance.findUserByEditionAndUsername(e1, "megangarber");
		User jny2 = DAO.instance.findUserByEditionAndUsername(e1, "jny2");

		DAO.instance.follow(mg, jny2);

		Edition e2 = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
		mg = DAO.instance.findUserByEditionAndUsername(e2, "megangarber");
		jny2 = DAO.instance.findUserByEditionAndUsername(e2, "jny2");

		assertEquals(DAO.instance.isFollowing(mg, jny2, null), true);
		assertEquals(DAO.instance.isFollowing(jny2, jny2, null), true);

		for(Perishable p : module.mockPs)
			verify(p);
	}


}
