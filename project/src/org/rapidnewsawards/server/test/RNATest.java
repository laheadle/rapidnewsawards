package org.rapidnewsawards.server.test;

import junit.framework.TestCase;

import org.rapidnewsawards.core.User;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;


public abstract class RNATest extends TestCase {

	protected static DAO d = DAO.instance;

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
	private LocalTaskQueueTestConfig taskQueueConfig;

	protected User getUser(String name) {
		if (name == null)
			name = "johnqpublic";
		
		return d.users.findUserByLogin(name + "@gmail.com", "gmail.com"); 
	}

	public void doTransition() {
		d.transition.transitionEdition();
	}


	@Override
	public void setUp() throws Exception {
        super.setUp();
        helper.setUp();
        MakeDataServlet.testing = true;
        taskQueueConfig = new LocalTaskQueueTestConfig();

		MakeDataServlet.makeData(3, 30 * 60 * MakeDataServlet.ONE_SECOND, null);
	}

	@Override
	public void tearDown() throws Exception {
        helper.tearDown();
        MakeDataServlet.testing = false;
        super.tearDown();
	}
	
	
}
