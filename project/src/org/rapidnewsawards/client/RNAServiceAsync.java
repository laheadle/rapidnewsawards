package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>RNAService</code>.
 */
public interface RNAServiceAsync {
	void sendRecentVotes(Integer edition, AsyncCallback<RecentVotes> callback);

	void sendRecentSocials(Integer editionNum,
			AsyncCallback<RecentSocials> asyncCallback);
}
