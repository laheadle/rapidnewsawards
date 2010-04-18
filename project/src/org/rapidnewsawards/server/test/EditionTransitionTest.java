package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;
import org.rapidnewsawards.server.Config;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.server.PerishableFactory;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.User;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;

// Here we are testing the case of a periodical whose final edition is current.
public abstract class EditionTransitionTest extends RNATest {

	public static class Module extends AbstractModule {
		@Override 
		protected void configure() {}

		public ArrayList<Perishable> mockPs = new ArrayList<Perishable>();
		int currentEdition = 1;
		int numEditions = 3;

		@Provides
		PerishableFactory fact() {
			abstract class PF implements PerishableFactory {}
			PerishableFactory pF = new PF() {
				public Perishable create(Date end) {
					Perishable mockP = createMock(Perishable.class);
					if (currentEdition == numEditions + 1) {
						// called by findPeriodicalByName
						// we're going to add follows and eventPanel to this one
						expect(mockP.isExpired()).andReturn(false);
					}
					// second time editions are queried
					else if (currentEdition == 2) {
						// called by findPeriodicalByName
						// expire the one we modified
						expect(mockP.isExpired()).andReturn(true);							
					}
					else {
						// called by findPeriodicalByName
						// not expired: we're going to test this one for follows and eventPanel copied in
						expect(mockP.isExpired()).andReturn(false);
					}
					replay(mockP);
					mockPs.add(mockP);
					currentEdition++;
					return mockP;
				}
			};
			return pF;
		}
	}

	public Module module;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		module = new Module();
		org.rapidnewsawards.server.Config.injector = Guice.createInjector(module);
		MakeDataServlet.makeData(module.numEditions, 60 * MakeDataServlet.ONE_SECOND, null);
	}

	@Override
	public void tearDown() throws Exception {
		org.rapidnewsawards.server.Config.init();
		module = null;
		super.tearDown();
	}


}


