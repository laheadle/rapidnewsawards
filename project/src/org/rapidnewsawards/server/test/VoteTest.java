package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.JudgeInfluence;
import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Response;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;
import org.rapidnewsawards.server.RNAException;

import com.googlecode.objectify.Key;

public class VoteTest extends RNATest {
	
	@Test
	public void testVotes() throws MalformedURLException, RNAException {

		Key<Edition> e1 = d.editions.getCurrentEdition().getKey();		
		User jqp = getUser(null);
		JudgeInfluence inf = d.ofy().query(JudgeInfluence.class)
		.ancestor(e1).filter("user", jqp.getKey()).get();
		d.ofy().delete(inf);
		d.ofy().put(new JudgeInfluence(1, 
				ScoreSpace.keyFromEditionKey(e1), jqp.getKey()));

		Link l = d.users.getOrCreateLink("http://example.com", "title", jqp.getKey());
		assertEquals(d.editions.getLatestUser_Vote_Links(e1).size(), 0);
		Response r = d.users.voteFor(jqp, e1, l, true);
		assertEquals(r, Response.SUCCESS);
		d.users.writeVote(jqp.getKey(), e1, l.getKey(), true);
		assertTrue(d.users.hasVoted(jqp, e1, l));
		Vote v = d.editions.getLatestUser_Vote_Links(e1).get(0).vote;
		ScoreSpace space = d.editions.getScoreSpace(d.ofy(), v.edition);
		space.balance = 100;
		d.ofy().put(space);
		d.tallyVote(v.getKey(), true);
		space = d.editions.getScoreSpace(d.ofy(), v.edition);
		assertEquals(space.totalScore, 1);
		assertEquals(space.numFundedLinks, 1);
		assertEquals(space.totalSpend, 100);
		assertEquals(space.balance, 100);
		d.addJudgeScore(v.getKey(), 1, true);
		JudgeInfluence ji = d.users.getJudgeInfluence(d.ofy(), v.voter, v.edition);
		assertEquals(ji.score, 1);
		//d.addEditorFunding(v.getKey(), 100);
		//EditorInfluence ei = d.users.getEditorInfluence(d.ofy(), v.voter, v.edition);
		//assertEquals(ei.funded, 100);				
	}

}
