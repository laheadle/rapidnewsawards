package org.rapidnewsawards.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.EditionUserAuthority;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.RNAException;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.ScoredLink;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;

import com.googlecode.objectify.Key;


public class FundingTest extends RNATest {

	@Test
	public void testFunding() throws MalformedURLException, RNAException {
		Key<Edition> e1 = Edition.createKey(0);		
		d.editions.setSpaceBalance(0, 1000);
		User jqp = getUser(null);
		EditionUserAuthority eua = d.ofy().query(EditionUserAuthority.class)
		.ancestor(e1).filter("user", jqp.getKey()).get();
		d.ofy().delete(eua);
		d.ofy().put(new EditionUserAuthority(1, 
				e1, jqp.getKey()));

		Link l = d.users.createLink("http://example.com", "title", jqp.getKey());
		assertEquals(d.editions.getLatestUser_Vote_Links(e1).size(), 0);
		Response r = d.users.voteFor(jqp, e1, l, true);
		assertEquals(r, Response.SUCCESS);
		d.users.writeVote(jqp.getKey(), e1, l.getKey(), true);
		assertTrue(d.users.hasVoted(jqp, e1, l));
		Vote v = d.editions.getLatestUser_Vote_Links(e1).get(0).vote;
		d.tallyVote(v.getKey());
		ScoreSpace space = d.editions.getScoreSpace(v.edition);
		assertEquals(space.totalScore, 1);
		assertEquals(space.totalSpend, 1000);
		ScoredLink sl = d.ofy().query(ScoredLink.class)
		.ancestor(space.root).filter("link", v.link).get();
		assertEquals(sl.funding, 1000);		

	}


}
