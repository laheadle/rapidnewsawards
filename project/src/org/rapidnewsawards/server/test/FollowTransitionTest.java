package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.Config;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class FollowTransitionTest extends EditionTransitionTest {

		
	@Test
	public void testFollows() {
		Edition e1 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		User mg = DAO.instance.findUserByEditionAndUsername(e1, "megangarber");
		User jny2 = DAO.instance.findUserByEditionAndUsername(e1, "jny2");

		DAO.instance.follow(mg, jny2);

		Edition e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		User mg2 = DAO.instance.findUserByEditionAndUsername(e2, "megangarber");
		User jny22 = DAO.instance.findUserByEditionAndUsername(e2, "jny2");

		assertEquals(DAO.instance.isFollowing(mg2, jny22, null), true);
		assertEquals(DAO.instance.isFollowing(jny22, jny22, null), true);

		for(Perishable p : module.mockPs)
			verify(p);
	}


}
