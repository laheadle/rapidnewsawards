package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.messages.Return;
import org.rapidnewsawards.server.DAO;

public class FollowTransitionTest extends RNATest {

		
	@Test
	public void testFollows() {
		Edition e2 = DAO.instance.editions.getEdition(Name.AGGREGATOR_NAME, 1, null);
				
		User mg = getUser("ohthatmeg");
		User jny2 = getUser("Joshuanyoung");

		Return r = DAO.instance.social.doSocial(mg.getKey(), jny2.getKey(), e2, true);
		assertEquals(r.s, Return.ABOUT_TO_FOLLOW.s);
		
		assertNull(DAO.instance.social.getFollow(mg.getKey(), jny2.getKey(), null));
		assertNotNull("About To Follow", DAO.instance.social.getAboutToSocial(mg.getKey(), jny2.getKey(), e2, null));

		doTransition();
		
		e2 = DAO.instance.editions.getCurrentEdition(Name.AGGREGATOR_NAME);

		assertNotNull(DAO.instance.social.getFollow(mg.getKey(), jny2.getKey(), null));

	}


}
