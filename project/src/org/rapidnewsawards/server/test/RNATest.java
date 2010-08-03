package org.rapidnewsawards.server.test;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.ApiProxy;


public abstract class RNATest extends TestCase {

	protected static DAO d = DAO.instance;

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	protected User getUser(String name) {
		if (name == null)
			name = "ohthatmeg";
		
		return d.findUserByLogin(name + "@gmail.com", "gmail.com"); 
	}

	public void doTransition() {
		Edition current = d.getEdition(Name.JOURNALISM, -1, null);
		Edition next = d.getEdition(Name.JOURNALISM, -2, null);
		d.transitionEdition(Name.JOURNALISM);
		d.socialTransition(next);
		d.setEditionRevenue();
		d.fund(current.getKey());
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
