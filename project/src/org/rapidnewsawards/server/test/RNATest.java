package org.rapidnewsawards.server.test;

import java.io.File;

import junit.framework.TestCase;

import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.shared.Config;
import org.rapidnewsawards.shared.User;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

public abstract class RNATest extends TestCase {

	protected User getUser(String username) {
		if (username == null)
			username = "megangarber";

		return DAO.instance.findUserByEditionAndUsername(DAO.instance.getCurrentEdition(Config.Name.JOURNALISM), username);
	}

	@Override
	public void setUp() throws Exception {
        super.setUp();
        ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
        ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")){});

        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        
	}

	@Override
	public void tearDown() throws Exception {
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService = (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        
		ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);

        super.tearDown();
	}
	
	
}
