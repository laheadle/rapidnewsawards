package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.Date;

import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Perishable;
import org.rapidnewsawards.shared.PerishableFactory;

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

		// the first third are created by makeData
		// the second and third thirds are created by objectify queries
		int numEditionsInstantiated = numEditions * 3;

		@Provides
		PerishableFactory fact() {
			abstract class PF implements PerishableFactory {}
			PerishableFactory pF = new PF() {
				public Perishable create(Date end) {
					Perishable mockP = createMock(Perishable.class);
					// Only test those editions created by Objectify
					// not the ones created by makeData
					if (currentEdition > numEditions) {
						if (currentEdition == numEditions + 1) {
							// called by findPeriodicalByName
							// we're going to add follows and votes to this one
							expect(mockP.isExpired()).andReturn(false);
						}
						// second time editions are queried
						else if (currentEdition == numEditions * 2 + 1) {
							// called by findPeriodicalByName
							// expire the one we modified
							expect(mockP.isExpired()).andReturn(true);							
						}
						else if (currentEdition == numEditions * 2 + 2 ) {
							// called by findPeriodicalByName
							// not expired: we're going to test this one for follows and votes copied in
							expect(mockP.isExpired()).andReturn(false);
						}
						replay(mockP);
						mockPs.add(mockP);
					}
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
		org.rapidnewsawards.shared.Config.injector = Guice.createInjector(module);
		MakeDataServlet.makeData(module.numEditions, 60 * MakeDataServlet.ONE_SECOND);
	}

	@Override
	public void tearDown() throws Exception {
		org.rapidnewsawards.shared.Config.init();
		module = null;
        super.tearDown();
	}


}


