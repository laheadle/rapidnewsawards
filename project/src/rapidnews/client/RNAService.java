package rapidnews.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import rapidnews.shared.*;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("rna")
public interface RNAService extends RemoteService {
	Edition sendEdition();
}
