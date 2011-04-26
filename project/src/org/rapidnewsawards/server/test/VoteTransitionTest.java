package org.rapidnewsawards.server.test;

//import static org.easymock.EasyMock.verify;

import java.util.Iterator;

import org.junit.Test;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.ScoredLink;
import org.rapidnewsawards.shared.User;

public class VoteTransitionTest extends RNATest {
	
	@Test
	public void testVotes() {
		Edition e1 = d.editions.getCurrentEdition(Name.AGGREGATOR_NAME);		
		User mg = getUser(null);

		Link l = d.createLink("http://example.com", "title", mg.getKey());
		assertEquals(d.getLatestUser_Vote_Links(e1).size(), 0);
		d.voteFor(mg, e1, l, true);
		assertTrue(d.hasVoted(mg, e1, l));
		assertEquals("User_Vote_Link Exists", d.getLatestUser_Vote_Links(e1).size(), 1);
		
		doTransition();
		
		Edition e2 = d.editions.getCurrentEdition(Name.AGGREGATOR_NAME);
		
		assertFalse(d.hasVoted(mg, e2, l));
	}

	public void testTally() {
		d.editions.getCurrentEdition(Name.AGGREGATOR_NAME);		
		Edition e2 = d.editions.getEdition(Name.AGGREGATOR_NAME, 1, null);

		User mg = getUser("ohthatmeg");
		User jny2 = getUser("joshuanyoung");
		User steveouting = getUser("steveouting");

		// megan follows josh 
		d.social.doSocial(mg.getKey(), jny2.getKey(), e2, true);

		// megan and josh follow steve
		d.social.doSocial(mg.getKey(), steveouting.getKey(), e2, true);
		d.social.doSocial(jny2.getKey(), steveouting.getKey(), e2, true);

		doTransition();

		e2 = d.editions.getCurrentEdition(Name.AGGREGATOR_NAME);
		
		Link l1 = d.createLink("http://example.com", "title", mg.getKey());
		Link l2 = d.createLink("http://example2.com", "title", mg.getKey());
		Link l3 = d.createLink("http://example3.com", "title",  mg.getKey());
		
		d.voteFor(jny2, e2, l1, true);

		d.voteFor(steveouting, e2, l2, true);

		d.voteFor(jny2, e2, l3, true);
		d.voteFor(steveouting, e2, l3, true);

		doTransition();
		
		Iterator<ScoredLink> iter = d.editions.getScoredLinks(e2).iterator();
		
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
	}


}
