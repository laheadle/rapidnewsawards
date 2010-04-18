package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("rna")
public interface RNAService extends RemoteService {
	RecentVotes sendRecentVotes(Integer edition);
	RecentSocials sendRecentSocials(Integer edition);
}
