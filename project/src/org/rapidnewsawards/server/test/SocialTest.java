package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditionUserAuthority;
import org.rapidnewsawards.core.Follow;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.RNAException;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.SocialEvent;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.server.DAO;

public class SocialTest extends RNATest {
	

	private Edition e0;
	private Edition e1;
	private User mg;
	private User jqp;

	@Override
	public void setUp() throws Exception {
        super.setUp();
    	e0 = DAO.instance.editions.getEdition(0);
		e1 = DAO.instance.editions.getEdition(1);
    	mg = getUser("ohthatmeg");
    	jqp = getUser(null);
	}

	@Test
	public void transitionInProgressResponse() throws RNAException  {
		Periodical p = d.getPeriodical();
		p.inTransition = true;
		d.ofy().put(p);

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), Edition.createKey(0), true);
		
		assertEquals(r, Response.TRANSITION_IN_PROGRESS);
	}


	@Test
	public void testFollowResponse() throws RNAException  {				
		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), true);
		
		assertEquals(r.s, Response.ABOUT_TO_FOLLOW.s);
	}


	@Test
	public void testUnFollowResponse() throws RNAException  {
		SocialEvent se = new SocialEvent(mg.getKey(), jqp.getKey(), 
				e0.getKey(), new Date(), true);

		d.ofy().put(se);
		d.ofy().put(new Follow(mg.getKey(), jqp.getKey(), e0.getKey(), se.getKey()));

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), false);
		
		assertEquals(r.s, Response.ABOUT_TO_UNFOLLOW.s);
	}


	@Test
	public void testCancelPendingFollowResponse() throws RNAException  {
		SocialEvent se = new SocialEvent(mg.getKey(), jqp.getKey(), 
				e1.getKey(), new Date(), true);

		d.ofy().put(se);
		d.ofy().put(new Follow(mg.getKey(), jqp.getKey(), e1.getKey(), se.getKey()));

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), false);
		
		assertEquals(r.s, Response.PENDING_FOLLOW_CANCELLED.s);
	}


	@Test
	public void testCancelPendingUnFollowResponse() throws RNAException  {
		SocialEvent se = new SocialEvent(mg.getKey(), jqp.getKey(), 
				e1.getKey(), new Date(), false);

		d.ofy().put(se);
		d.ofy().put(new Follow(mg.getKey(), jqp.getKey(), e0.getKey(), se.getKey()));

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), true);
		
		assertEquals(r.s, Response.PENDING_UNFOLLOW_CANCELLED.s);
	}


	@Test
	public void testNotAnEditorResponse() throws RNAException  {
		Response r = DAO.instance.social.doSocial(
				jqp.getKey(), mg.getKey(), e0.getKey(), true);
		
		assertEquals(r.s, Response.NOT_AN_EDITOR.s);
	}


	@Test
	public void testBadEditionResponse() throws RNAException  {
		Response doSocial = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), Edition.createKey(276), true);
		Response r = doSocial;
		
		assertEquals(r, Response.EDITION_NOT_CURRENT);
	}

	@Test
	public void testAlreadyAboutToFollowResponse() throws RNAException  {
		SocialEvent se = new SocialEvent(mg.getKey(), jqp.getKey(), 
				e1.getKey(), new Date(), true);

		d.ofy().put(se);
		d.ofy().put(new Follow(mg.getKey(), jqp.getKey(), e1.getKey(), se.getKey()));

		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), true);
		
		assertEquals(r.s, Response.ALREADY_ABOUT_TO_FOLLOW.s);
	}

	@Test
	public void testAlreadyAboutToUnfollowResponse() throws RNAException  {
		SocialEvent se = new SocialEvent(mg.getKey(), jqp.getKey(), 
				e1.getKey(), new Date(), false);

		d.ofy().put(se);
		d.ofy().put(new Follow(mg.getKey(), jqp.getKey(), e0.getKey(), se.getKey()));

		Response doSocial = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), false);
		Response r = doSocial;
		
		assertEquals(r, Response.ALREADY_ABOUT_TO_UNFOLLOW);
	}

	@Test
	public void testNotFollowingResponse() throws RNAException  {
		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), e0.getKey(), false);
		
		assertEquals(r, Response.NOT_FOLLOWING);
	}

	@Test
	public void testNoLongerCurrentResponse() throws RNAException  {
		Response r = DAO.instance.social.doSocial(
				mg.getKey(), jqp.getKey(), Edition.createKey(1), true);

		assertEquals(r, Response.EDITION_NOT_CURRENT);
	}

		
	@Test
	public void testWriteFollow() throws InterruptedException {
		DAO.instance.social.writeSocialEvent(
				mg.getKey(), jqp.getKey(), Edition.createKey(1), true);
		Thread.sleep(200);
		assertEquals(
				d.ofy().query(Follow.class).ancestor(mg.getKey()).count(),
				2);
		assertEquals(
				d.ofy().query(Follow.class).filter("judge", jqp.getKey()).count(),
				2);
	}
	
	@Test
	public void testChangePendingAuthority() {
		d.social.changePendingAuthority(jqp.getKey(), e1.getKey(), 1);
		assertEquals(
				d.ofy().query(EditionUserAuthority.class)
				.ancestor(e1.getKey())
				.filter("user", jqp.getKey())
				.get()
				.authority,
				1);
	}

}