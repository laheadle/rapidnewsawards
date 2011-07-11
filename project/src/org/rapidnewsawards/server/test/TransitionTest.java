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
import org.rapidnewsawards.server.RNAException;

public class TransitionTest extends RNATest {
	@Test
	public void testFinalEditionCurrent() throws RNAException {
		for (int i = 0;i < numEditions - 1;i++) {
			DAO.instance.transition.transitionEdition();
		}
		Edition e = DAO.instance.editions.getCurrentEdition();
		assertNotNull(e);
		assertEquals(e.number, numEditions - 1);
	}

	@Test
	public void testTransition() throws RNAException {
		d.transition.transitionEdition();
		assertFalse(d.getPeriodical().isFinished());
		assertTrue(d.getPeriodical().inTransition);
		assertFalse(d.getPeriodical().userlocked);
	}

	@Test(expected = RNAException.class)
	public void testBadCurrentTransition() throws Exception {
		d.transition.transitionEdition();
	}
	
	@Test(expected = RNAException.class)
	public void testDeadTransition() throws Exception {
		Periodical p = d.getPeriodical();
		p.setFinished();
		d.ofy().put(p);
		d.transition.transitionEdition();
	}
	
	@Test(expected = RNAException.class)
	public void testNoCurrentTransition() throws Exception {
		Periodical p = d.getPeriodical();
		p.setcurrentEditionKey(null);
		d.ofy().put(p);
		d.transition.transitionEdition();
	}
	
	// TODO This should throw an unchecked exception
	@Test(expected = RNAException.class)
	public void testNoNextTransition() throws Exception {
		Periodical p = d.getPeriodical();
		p.setcurrentEditionKey(Edition.createKey(2));
		d.ofy().put(p);
		d.transition.transitionEdition();
	}
	
	@Test
	public void testSetBalance() throws RNAException {
		d.transition.transitionEdition();
		d.transition.setPeriodicalBalance();
		d.editions.setSpaceBalance(1, 100);
		d.transition.finishTransition();
		ScoreSpace s = d.editions.getScoreSpace(d.ofy(), Edition.createKey(0));
		assertEquals(s.balance, 0);
	}
}


