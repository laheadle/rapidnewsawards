package rapidnews.server;

import rapidnews.client.RNAService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */

// for session:
//this.getThreadLocalRequest().getSession();

@SuppressWarnings("serial")
public class RNAServiceImpl extends RemoteServiceServlet implements
RNAService {

	public rapidnews.shared.Edition sendEdition() {
		rapidnews.shared.Edition e = new rapidnews.shared.Edition();
		Reader r = Reader.findByUsername("megangarber");
		e.addReader(r.getDTO());
		return e;
	}
}
