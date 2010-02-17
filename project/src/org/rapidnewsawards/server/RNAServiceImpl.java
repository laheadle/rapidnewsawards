package org.rapidnewsawards.server;


import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.logging.Logger;

import org.rapidnewsawards.client.RNAService;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.Reader;

/**
 * The server side implementation of the RPC service.
 */

// for session:
//this.getThreadLocalRequest().getSession();

@SuppressWarnings("serial")
public class RNAServiceImpl extends RemoteServiceServlet implements
RNAService {
	private static final Logger log = Logger.getLogger(RNAServiceImpl.class.getName());

	public Edition sendEdition() {		
		return DAO.instance.getCurrentEdition("Journalism");
	}
}
