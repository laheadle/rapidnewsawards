package rapidnews.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import rapidnews.shared.*;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface RNAServiceAsync {
	void sendEdition(AsyncCallback<Edition> callback);
}
