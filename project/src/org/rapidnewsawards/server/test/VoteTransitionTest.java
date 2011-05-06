package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import java.net.MalformedURLException;
import java.util.Iterator;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditionUserAuthority;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.ScoreRoot;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;
import org.rapidnewsawards.messages.Response;
import org.rapidnewsawards.server.DAO;

import com.googlecode.objectify.Key;

public class VoteTransitionTest extends RNATest {
	
	@Test
	public void testVotes() throws MalformedURLException {

		Edition e1 = d.editions.getCurrentEdition();		
		User mg = getUser(null);
		d.ofy().put(new EditionUserAuthority(0, 
				d.editions.getCurrentEdition().getKey(), mg.getKey()));
		d.ofy().put(new EditionUserAuthority(0, 
				d.editions.getEdition(DAO.Editions.NEXT).getKey(), mg.getKey()));

		Link l = d.users.createLink("http://example.com", "title", mg.getKey());
		assertEquals(d.editions.getLatestUser_Vote_Links(e1).size(), 0);
		Response r = d.users.voteFor(mg, e1, l, true);
		assertEquals(r, Response.SUCCESS);
		d.users.writeVote(mg.getKey(), e1.getKey(), l.getKey(), true);
		assertTrue(d.users.hasVoted(mg, e1, l));
		Vote v = d.editions.getLatestUser_Vote_Links(e1).get(0).vote;
		d.tallyVote(v.getKey());
		ScoreSpace space = d.editions.getScoreSpace(v.edition);
		assertEquals(space.totalScore, 0);
	}

	public void testTally() throws MalformedURLException, InterruptedException {
				
		Edition e = d.editions.getCurrentEdition();

		User mg = getUser("ohthatmeg");
		User jny2 = getUser("joshuanyoung");
		User steveouting = getUser("steveouting");

		// megan follows josh 
		d.social.doSocial(mg.getKey(), jny2.getKey(), e.getKey(), true);

		// megan and josh follow steve
		d.social.doSocial(mg.getKey(), steveouting.getKey(), e.getKey(), true);
		d.social.doSocial(jny2.getKey(), steveouting.getKey(), e.getKey(), true);

		doTransition();
		Thread.sleep(1000);
		e = d.editions.getCurrentEdition();
		
		Link l1 = d.users.createLink("http://example.com", "title", mg.getKey());
		Link l2 = d.users.createLink("http://example2.com", "title", mg.getKey());
		Link l3 = d.users.createLink("http://example3.com", "title",  mg.getKey());
		
		d.users.voteFor(jny2, e, l1, true);

		d.users.voteFor(steveouting, e, l2, true);

		d.users.voteFor(jny2, e, l3, true);
		d.users.voteFor(steveouting, e, l3, true);

		doTransition();
		
		Iterator<ScoredLink> iter = d.editions.getScoredLinks(e, 1).iterator();
/*		
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
		*/
	}


}
