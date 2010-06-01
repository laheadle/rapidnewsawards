package org.rapidnewsawards.server;


import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.log.Log;
import org.rapidnewsawards.client.RNA;
import org.rapidnewsawards.client.RNAService;
import org.rapidnewsawards.shared.Donation;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentStories;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.UserInfo;
import org.rapidnewsawards.shared.User_Authority;
import org.rapidnewsawards.shared.VoteResult;

/**
 * The server side implementation of the RPC service.
 */

// for session:
//this.getThreadLocalRequest().getSession();

@SuppressWarnings("serial")
public class RNAServiceImpl extends RemoteServiceServlet implements
RNAService {
	private static final Logger log = Logger.getLogger(DAO.class.getName());
	private static DAO d = DAO.instance;
	
	// TODO: on client side, null means current edition; on server, -1 does.
	private int ed(Integer edition) { return edition == null ? -1 : edition; }
	
	@Override
	public RecentVotes sendRecentVotes(Integer edition) {
		// TODO don't return future editions
		return DAO.instance.getRecentVotes(ed(edition), Name.JOURNALISM);
	}

	@Override
	public RecentSocials sendRecentSocials(Integer edition) {
		Edition current = null;
		Edition next = null;

		if (edition == null) {
			current = DAO.instance.getEdition(Name.JOURNALISM, -1, null);
			next = DAO.instance.getEdition(Name.JOURNALISM, -2, null);
		}
		else {
			// next after edition
			current = DAO.instance.getEdition(Name.JOURNALISM, edition, null);
			next = DAO.instance.getEdition(Name.JOURNALISM, edition + 1, null);
		}
		
		if (current == null || next == null) {
			return null;
		}

		return DAO.instance.getRecentSocials(current, next, Name.JOURNALISM);
	}

	@Override
	public Return doSocial(User to, boolean on) {
		
		if (d.user == null) {
			log.warning("attempt to follow with null user");
			return Return.ILLEGAL_OPERATION;
		}
		
		// read-only transaction 
		Edition e = d.getEdition(Name.JOURNALISM, -2, null);
		Return result = d.doSocial(d.user.getKey(), to.getKey(), e, on);
		return result;
	}
	
	@Override
	public RelatedUserInfo sendRelatedUser(long userId) {
		return d.getRelatedUserInfo(Name.JOURNALISM, d.user, new Key<User>(User.class, userId));
	}

	@Override
	public RecentStories sendTopStories(Integer editionNum) {
		return d.getTopStories(ed(editionNum), Name.JOURNALISM);
	}

	public static String home = "/Rapid_News_Awards.html?gwt.codesvr=127.0.0.1:9997";

	@Override
	public VoteResult voteFor(String link, String fullLink, Edition edition, Boolean on) {
		VoteResult vr = new VoteResult();
        UserService userService = UserServiceFactory.getUserService();
		
		if (d.user == null) {
			vr.returnVal = Return.NOT_LOGGED_IN;
			vr.authUrl = userService.createLoginURL(fullLink);
			return vr;
		}

		// TODO broken on some complex hrefs
    	Link l = DAO.instance.findByFieldName(Link.class, Name.URL, link, null);
    	if (l == null) {
    		return null;
    	}
    	else {
    		vr.returnVal = d.voteFor(d.user, edition == null? d.getCurrentEdition(Name.JOURNALISM) : edition, l, on);
    		vr.authUrl = userService.createLogoutURL(home);    		
    	}
		return vr;
	}

	@Override
	public LinkedList<User_Authority> getVoters(Link link, Edition e) {
		return d.getVoters(link, e);
	}

	@Override
	public String sendLoginUrl(String url) {
        UserService userService = UserServiceFactory.getUserService();
        return userService.createLoginURL(url);
	}

	@Override
	public User sendUserInfo() {
		return d.user;
	}

	@Override
	public String sendLogoutUrl(String url) {
        UserService userService = UserServiceFactory.getUserService();
        return userService.createLogoutURL(url);
	}

	@Override
	public VoteResult submitStory(String url, String title, Edition edition) {
		VoteResult vr = new VoteResult();
        UserService userService = UserServiceFactory.getUserService();
		
		if (d.user == null) {
			vr.returnVal = Return.NOT_LOGGED_IN;
			vr.authUrl = null; // userService.createLoginURL(fullLink);
			return vr;
		}

		// TODO broken on some complex hrefs
    	Link l = DAO.instance.createLink(url, title, d.user.getKey());

    	if (l == null) {
    		return null;
    	}
    	
    	else {
    		vr.returnVal = d.voteFor(d.user, edition == null? d.getCurrentEdition(Name.JOURNALISM) : edition, l, true);
    		vr.authUrl = null; // userService.createLogoutURL(home);    		
    	}
		return vr;
	}

	@Override
	public String grabTitle(String urlStr) {
		return TitleGrabber.getTitle(urlStr);
	}

	@Override
	public String welcomeUser(String nickname, Integer donation) {
		if (d.user == null)
			return "failed";

		return d.welcomeUser(nickname, donation);
	}

}
