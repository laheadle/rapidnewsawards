package rapidnews.server.test;

import static org.junit.Assert.*;

import java.io.File;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapidnews.server.DAO;
import rapidnews.server.MakeDataServlet;
import rapidnews.shared.Edition;
import rapidnews.shared.Link;
import rapidnews.shared.Reader;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

public class DAOTest extends TestCase {

	@Before
	public void setUp() throws Exception {
        super.setUp();
        ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
        ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")){});

        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        
        MakeDataServlet.makeData(2, MakeDataServlet.ONE_SECOND);
	}

	@After
	public void tearDown() throws Exception {
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService = (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        
		ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);

        super.tearDown();
	}


	@Test
	public void testEditions() {
		Edition e = DAO.instance.getCurrentEdition("Journalism");
		assertNotNull(e);
	}
	

	@Test
	public void testFindReaderByUsername () {
		try {
			Reader mg = DAO.instance.findReaderByUsername("megangarber", true);
			assertNotNull(mg);
			assertEquals(mg.getUsername(), "megangarber");
		} catch (EntityNotFoundException e) {
			fail("error in filling ref");
		}
	}

	
	@Test
	public void testVote() {
		try {
			Reader mg = DAO.instance.findReaderByUsername("megangarber", true);
			Link l = DAO.instance.findOrCreateLinkByURL("http://example.com");
			Link l3 = DAO.instance.findOrCreateLinkByURL("http://example2.com");
			DAO.instance.voteFor(mg, l);
			assertTrue(DAO.instance.hasVoted(mg, l));
			DAO.instance.voteFor(mg, l3);
			assertTrue(DAO.instance.hasVoted(mg, l3));
			assertTrue(DAO.instance.hasVoted(mg, l));
			Link l2 = DAO.instance.findOrCreateLinkByURL("http://bad.com");
			assertFalse(DAO.instance.hasVoted(mg, l2));
		} catch (EntityNotFoundException e) {
			fail("error in filling ref");
		}
	}

	
}
