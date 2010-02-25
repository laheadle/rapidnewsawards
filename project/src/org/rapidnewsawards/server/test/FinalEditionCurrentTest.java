package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Config;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Perishable;
import org.rapidnewsawards.shared.PerishableFactory;
import org.rapidnewsawards.shared.User;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;

// Here we are testing the case of a periodical whose final edition is current.
public class FinalEditionCurrentTest extends RNATest {

	public static ArrayList<Perishable> mockPs = new ArrayList<Perishable>();
	static int currentEdition = 1;
	static int numEditions = 3;
	
	// the first half are created by makeData
	// the second half are created by objectify queries
	static int numEditionsInstantiated = numEditions * 2;
	
	public static class RNAModule extends AbstractModule {
		@Override 
		protected void configure() {}

		@Provides
		PerishableFactory fact() {
			abstract class PF implements PerishableFactory {}
			PerishableFactory pF = new PF() {
				public Perishable create(Date end) {
					Perishable mockP = createMock(Perishable.class);
					// Only test those editions created by Objectify
					// not the ones created by makeData
					if (currentEdition > numEditions) {
						if (currentEdition < numEditionsInstantiated)
							// called by findPeriodicalByName
							expect(mockP.isExpired()).andReturn(true);
						else {
							// this is the last edition, and it is current
							// called by findPeriodicalByName
							expect(mockP.isExpired()).andReturn(false);
						}
						replay(mockP);
						FinalEditionCurrentTest.mockPs.add(mockP);
					}
					currentEdition++;
					return mockP;
				}
			};
			return pF;
		}
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		org.rapidnewsawards.shared.Config.injector = Guice.createInjector(new RNAModule());
		MakeDataServlet.makeData(numEditions, 60 * MakeDataServlet.ONE_SECOND);
	}

	@Test
	public void testEditions() {
		Edition e = DAO.instance.getCurrentEdition(Config.Name.JOURNALISM);
		for(Perishable p : mockPs)
			verify(p);
		assertNotNull(e);
		// This tests that the number of users copied over to the final edition is the same as in the first edition
		LinkedList<User> users = DAO.instance.findUsersByEdition(e);
		assertEquals(users.size(), 3);
		assertNotNull(e.getUsers());
		assertEquals(true, e.getUsers().size() == 3);
		assertEquals(true, e.getUsers().size() == users.size());
	}


}


