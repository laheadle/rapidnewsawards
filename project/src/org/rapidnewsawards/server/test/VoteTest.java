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

public class VoteTest extends RNATest {
	
	@Test
	public void testVotes() throws MalformedURLException {

		Edition e1 = d.editions.getCurrentEdition();		
		User jqp = getUser(null);
		EditionUserAuthority eua = d.ofy().query(EditionUserAuthority.class)
		.ancestor(e1.getKey()).filter("user", jqp.getKey()).get();
		d.ofy().delete(eua);
		d.ofy().put(new EditionUserAuthority(1, 
				e1.getKey(), jqp.getKey()));

		Link l = d.users.createLink("http://example.com", "title", jqp.getKey());
		assertEquals(d.editions.getLatestUser_Vote_Links(e1).size(), 0);
		Response r = d.users.voteFor(jqp, e1, l, true);
		assertEquals(r, Response.SUCCESS);
		d.users.writeVote(jqp.getKey(), e1.getKey(), l.getKey(), true);
		assertTrue(d.users.hasVoted(jqp, e1, l));
		Vote v = d.editions.getLatestUser_Vote_Links(e1).get(0).vote;
		d.tallyVote(v.getKey());
		ScoreSpace space = d.editions.getScoreSpace(v.edition);
		assertEquals(space.totalScore, 1);
	}


}
