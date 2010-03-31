package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>RNAService</code>.
 */
public interface RNAServiceAsync {
	void sendState(Integer edition, AsyncCallback<State> callback);
}
