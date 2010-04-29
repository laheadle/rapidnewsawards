package org.rapidnewsawards.server;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

import java.util.logging.Logger;

import org.rapidnewsawards.client.RNAService;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.RecentSocials;
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
RNAService {

	public RecentVotes sendRecentVotes(Integer edition) {
		return DAO.instance.getRecentVotes(edition, Name.JOURNALISM);
	}

	public RecentSocials sendRecentSocials(Integer edition) {
		Edition current = null;
		Edition next = null;

		if (edition == null) {
			current = DAO.instance.getRawEdition(Name.JOURNALISM, -1, null);
			next = DAO.instance.getRawEdition(Name.JOURNALISM, -2, null);
		}
		else {
			// next after edition
			current = DAO.instance.getRawEdition(Name.JOURNALISM, edition, null);
			next = DAO.instance.getRawEdition(Name.JOURNALISM, edition + 1, null);
		}
		
		if (current == null || next == null) {
			return null;
		}

		return DAO.instance.getRecentSocials(current, next, Name.JOURNALISM);
	}

	public Return doSocial(User from, User to, boolean on) {
		// read-only transaction 
		// TODO lock?
		final DAO d = DAO.instance;
		Edition e = d.getRawEdition(Name.JOURNALISM, -2, null);
		from = d.findUserByUsername("jny2");
		Return result = d.doSocial(from.getKey(), to.getKey(), e, null, on);
		return result;
	}
	
	@Override
	public RelatedUserInfo sendRelatedUser(User from, long userId) {
		if (from == null) {
			// TODO logins
			from = DAO.instance.findUserByUsername("jny2");
		}
		return DAO.instance.getRelatedUserInfo(Name.JOURNALISM, from, new Key<User>(User.class, userId));
	}
}
