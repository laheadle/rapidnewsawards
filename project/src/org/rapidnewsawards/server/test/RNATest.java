package org.rapidnewsawards.server.test;

import junit.framework.TestCase;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;


public abstract class RNATest extends TestCase {

	protected static DAO d = DAO.instance;

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	protected User getUser(String name) {
		if (name == null)
			name = "ohthatmeg";
		
		return d.users.findUserByLogin(name + "@gmail.com", "gmail.com"); 
	}

	public void doTransition() {
		Edition current = d.editions.getEdition(Name.AGGREGATOR_NAME, -1, null);
		Edition next = d.editions.getEdition(Name.AGGREGATOR_NAME, -2, null);
		d.transition.transitionEdition(Name.AGGREGATOR_NAME);
		d.transition.socialTransition(next);
		d.transition.setEditionRevenue();
		d.editions.fund(current);
	}


	@Override
	public void setUp() throws Exception {
        super.setUp();
        helper.setUp();
        MakeDataServlet.testing = true;
		MakeDataServlet.makeData(3, 30 * 60 * MakeDataServlet.ONE_SECOND, null);
	}

	@Override
	public void tearDown() throws Exception {
        helper.tearDown();
        MakeDataServlet.testing = false;
        super.tearDown();
	}
	
	
}
