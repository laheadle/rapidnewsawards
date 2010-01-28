package rapidnews.server;

import rapidnews.client.RNAService;
import rapidnews.shared.Reader;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import java.util.logging.Logger;

/**
 * The server side implementation of the RPC service.
 */

// for session:
//this.getThreadLocalRequest().getSession();

@SuppressWarnings("serial")
public class RNAServiceImpl extends RemoteServiceServlet implements
RNAService {


	private static final Logger log = Logger.getLogger(RNAServiceImpl.class.getName());

	public rapidnews.shared.Edition sendEdition() {
		rapidnews.shared.Edition e = new rapidnews.shared.Edition();
		Reader r;
		try {
			r = DAO.instance.findReaderByUsername("megangarber", true);
			e.addReader(r);
		} catch (EntityNotFoundException e1) {
	        log.warning("No reader found");
		}
		return e;
	}
}
