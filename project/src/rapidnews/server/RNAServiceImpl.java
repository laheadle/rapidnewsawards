package rapidnews.server;

import rapidnews.client.RNAService;
import rapidnews.shared.Edition;
import rapidnews.shared.Periodical;
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

	public Edition sendEdition() {
		
		Periodical p;
		try {
			p = DAO.instance.findPeriodicalByName("Journalism", true);
		} catch (EntityNotFoundException e2) {
	        log.warning("No Periodical found");
	        return null;
		}
		
		Edition e = p.getCurrentEdition();
		
		Reader r;
		try {
			r = DAO.instance.findReaderByUsername("megangarber", true);
			e.addReader(r);
		} catch (EntityNotFoundException e1) {
	        log.warning("No reader found");
	        return null;
		}
		return e;
	}
}
