package org.rapidnewsawards.server.test;

import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;

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


