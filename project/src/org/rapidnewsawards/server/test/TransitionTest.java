package org.rapidnewsawards.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.ScoreSpace;
import org.rapidnewsawards.server.DAO;

public class TransitionTest extends RNATest {
	static int numEditions = 3;

	@Test
	public void testFinalEditionCurrent() {
		for (int i = 0;i < numEditions - 1;i++) {
			doTransition();
		}		
		Edition e = DAO.instance.editions.getCurrentEdition();
		assertNotNull(e);
		assertEquals(e.number, numEditions - 1);
	}

	@Test
	public void testTransition() {
		d.transition.doTransition(0);
		assertFalse(d.getPeriodical().isFinished());
		assertTrue(d.getPeriodical().inTransition);
		assertFalse(d.getPeriodical().userlocked);
	}

	@Test(expected = IllegalStateException.class)
	public void testBadCurrentTransition() throws Exception {
		d.transition.doTransition(1);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testDeadTransition() throws Exception {
		Periodical p = d.getPeriodical();
		p.setFinished();
		d.ofy().put(p);
		d.transition.doTransition(0);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testNoCurrentTransition() throws Exception {
		Periodical p = d.getPeriodical();
		p.setcurrentEditionKey(null);
		d.ofy().put(p);
		d.transition.doTransition(0);
	}
	
	@Test
	public void testSetBalance() {
		d.transition.doTransition(0);
		d.transition.setBalance();
		ScoreSpace s = d.editions.getScoreSpace(Edition.getKey(0));
		assertEquals(s.revenue, 0);
	}
}


