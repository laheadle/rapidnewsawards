package rapidnews.server.test;

import static org.junit.Assert.*;

import java.io.File;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapidnews.server.DAO;
import rapidnews.server.MakeDataServlet;
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
        
        MakeDataServlet.makeData();
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
	public void testFindReaderByUsername () {
		try {
			Reader mg = DAO.instance.findReaderByUsername("megangarber", true);
			assertNotNull(mg);
			assertEquals(mg.getUsername(), "megangarber");
		} catch (EntityNotFoundException e) {
			fail("error in filling ref");
		}
	}

}
