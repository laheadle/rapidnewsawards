package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("rna")
public interface RNAService extends RemoteService {
	RelatedUserInfo sendRelatedUser(long userId);
	Return doSocial(User to, boolean on);
	VoteResult voteFor(String link, String fullLink, Edition e, Boolean on);
	String sendLoginUrl(String url);
	String sendLogoutUrl(String url);
	User sendUserInfo();
	LinkedList<User_Authority> getVoters(Link link, Edition e);
	RecentVotes sendRecentVotes(Integer edition);
	RecentSocials sendRecentSocials(Integer edition);
	RecentStories sendTopStories(Integer editionNum);
}
