package org.rapidnewsawards.server;


import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.log.Log;
import org.rapidnewsawards.client.RNAService;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentStories;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.UserInfo;

/**
 * The server side implementation of the RPC service.
 */

// for session:
//this.getThreadLocalRequest().getSession();

@SuppressWarnings("serial")
public class RNAServiceImpl extends RemoteServiceServlet implements
RNAService, Filter {
	private static final Logger log = Logger.getLogger(DAO.class.getName());
	private static DAO d = DAO.instance;
	
	// TODO: on client side, null means current edition; on server, -1 does.
	private int ed(Integer edition) { return edition == null ? -1 : edition; }
	
	public RecentVotes sendRecentVotes(Integer edition) {
		return DAO.instance.getRecentVotes(ed(edition), Name.JOURNALISM);
	}

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

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		UserService userService = UserServiceFactory.getUserService();
		com.google.appengine.api.users.User appUser = userService.getCurrentUser();
		if (appUser == null) {
			d.user = null;
		}
		else {
			User u = DAO.instance.findUserByLogin(appUser.getEmail(), appUser.getAuthDomain());
			if (u == null) {
				// first time logging in; create new user
				u = new User();
				u.email = appUser.getEmail();
				u.domain = appUser.getAuthDomain();
				DAO.instance.ofy().put(u);
				d.user = u;
			}
		}
		chain.doFilter(request, response);
	}

	public void Login(String username, String password) {}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
