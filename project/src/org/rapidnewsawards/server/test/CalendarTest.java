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
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Perishable;
import org.rapidnewsawards.shared.PerishableFactory;
import org.rapidnewsawards.shared.User;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;

public class CalendarTest extends RNATest {

	public static ArrayList<Perishable> mockPs = new ArrayList<Perishable>();
	static int currentEdition = 1;
	static int totalEditions = 2;

	public static class RNAModule extends AbstractModule {
		@Override 
		protected void configure() {}

		// Here we are testing the case of a periodical whose final edition is current.
		@Provides
		PerishableFactory fact() {
			abstract class PF implements PerishableFactory {}
			PerishableFactory pF = new PF() {
				public Perishable create(Date end) {
					Perishable mockP = createMock(Perishable.class);
					// Only test those editions created by Objectify
					// not the ones created by makeData
					if (currentEdition > totalEditions) {
						if (currentEdition < totalEditions * 2)
							// called by findPeriodicalByName
							expect(mockP.isExpired()).andReturn(true);
						else {
							// this is the last edition, and it is current
							// called by findPeriodicalByName
							expect(mockP.isExpired()).andReturn(false);
							// called by verifyState
							expect(mockP.isExpired()).andReturn(false);
						}
						replay(mockP);
						CalendarTest.mockPs.add(mockP);
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
		MakeDataServlet.makeData(totalEditions, 60 * MakeDataServlet.ONE_SECOND);
	}

	@Test
	public void testEditions() {
		Edition e = DAO.instance.getCurrentEdition("Journalism");
		for(Perishable p : mockPs)
			verify(p);
		assertNotNull(e);
		LinkedList<User> users = DAO.instance.findUsersByEdition(e);
		assertEquals(users.size(), 0);
		assertNotNull(e.getUsers());
		assertEquals(true, e.getUsers().size() == 0);
		assertEquals(true, e.getUsers().size() == users.size());
	}


}


