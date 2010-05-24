package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;

public class FollowTransitionTest extends RNATest {

		
	@Test
	public void testFollows() {
		Edition e2 = DAO.instance.getEdition(Name.JOURNALISM, 1, null);
				
		User mg = getUser("megangarber");
		User jny2 = getUser("jny2");

		Return r = DAO.instance.doSocial(mg.getKey(), jny2.getKey(), e2, true);
		assertEquals(r.s, Return.ABOUT_TO_FOLLOW.s);
		
		assertNull(DAO.instance.getFollow(mg.getKey(), jny2.getKey(), null));
		assertNotNull("About To Follow", DAO.instance.getAboutToSocial(mg.getKey(), jny2.getKey(), e2, null));

		doTransition();
		
		e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);

		assertNotNull(DAO.instance.getFollow(mg.getKey(), jny2.getKey(), null));

	}


}
