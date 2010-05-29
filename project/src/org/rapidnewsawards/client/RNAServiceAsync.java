package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>RNAService</code>.
 */
public interface RNAServiceAsync {
	void sendRecentVotes(Integer edition, AsyncCallback<RecentVotes> callback);

	void sendRecentSocials(Integer editionNum,
			AsyncCallback<RecentSocials> asyncCallback);

	void sendRelatedUser(long userId, AsyncCallback<RelatedUserInfo> callback);
	void grabTitle(String urlStr, AsyncCallback<String> callback);
	void doSocial(User to, boolean on, AsyncCallback<Return> asyncCallback);
	void voteFor(String link, String fullLink, Edition e, Boolean checked, AsyncCallback<VoteResult> asyncCallback);
	
	void sendTopStories(Integer editionNum,
			AsyncCallback<RecentStories> asyncCallback);

	void getVoters(Link link, Edition edition, AsyncCallback<LinkedList<User_Authority>> asyncCallback);

	void sendLoginUrl(String url, AsyncCallback<String> asyncCallback);

	void sendUserInfo(AsyncCallback<User> asyncCallback);

	void sendLogoutUrl(String url, AsyncCallback<String> asyncCallback);

	void submitStory(String url, String title, Edition e, AsyncCallback<VoteResult> asyncCallback);
}
