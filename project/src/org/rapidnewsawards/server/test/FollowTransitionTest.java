package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Response;
import org.rapidnewsawards.server.DAO;

public class FollowTransitionTest extends RNATest {

		
	@Test
	public void testFollows() throws InterruptedException {
		Edition e0 = DAO.instance.editions.getEdition(0);
		Edition e1 = DAO.instance.editions.getEdition(1);
				
		User mg = getUser("ohthatmeg");
		User jny2 = getUser("Joshuanyoung");

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jny2.getKey(), e0.getKey(), true);
		/*
		assertEquals(r.s, Response.ABOUT_TO_FOLLOW.s);
		
		assertNotNull(DAO.instance.social.getFollow(
				mg.getKey(), jny2.getKey(), e1.getKey(), d.ofy()));
		
		assertNotNull("About To Follow", 
				DAO.instance.social.getAboutToSocial(
						mg.getKey(), jny2.getKey(), e1.getKey(), d.ofy()));

		doTransition();
		Thread.sleep(1000);
		assertNotNull(DAO.instance.social.getFollow(
				mg.getKey(), jny2.getKey(), e1.getKey(), d.ofy()));
*/
	}


}
