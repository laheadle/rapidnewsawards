package org.rapidnewsawards.server.test;

import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.server.DAO;

// Here we are testing the case of a periodical whose final edition is current.
public class FinalEditionCurrentTest extends RNATest {
	static int numEditions = 3;

	@Test
	public void testEditions() {
		for (int i = 0;i < numEditions - 1;i++) {
			doTransition();
		}		
		Edition e = DAO.instance.editions.getCurrentEdition(Name.AGGREGATOR_NAME);
		assertNotNull(e);
		assertEquals(e.number, numEditions - 1);
	}


}


