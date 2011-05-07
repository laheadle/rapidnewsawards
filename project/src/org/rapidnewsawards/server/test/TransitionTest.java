package org.rapidnewsawards.server.test;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
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
		assertTrue(d.getPeriodical().live);
		assertTrue(d.getPeriodical().inTransition);
		assertFalse(d.getPeriodical().userlocked);
	}

	@Test
	public void testSetBalance() {
		d.transition.doTransition(0);
		d.transition.setBalance();
		ScoreSpace s = d.editions.getScoreSpace(Edition.getKey(0));
		assertEquals(s.revenue, 0);
	}
}


