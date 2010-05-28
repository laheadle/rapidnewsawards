package org.rapidnewsawards.server;

import java.net.MalformedURLException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.RecentStories;
import org.rapidnewsawards.shared.Root;
import org.rapidnewsawards.shared.ScoredLink;
import org.rapidnewsawards.shared.SocialInfo;
import org.rapidnewsawards.shared.Follow;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.SocialEvent;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.StoryInfo;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.UserInfo;
import org.rapidnewsawards.shared.User_Authority;
import org.rapidnewsawards.shared.User_Link;
import org.rapidnewsawards.shared.Vote;
import org.rapidnewsawards.shared.Vote_Link;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.helper.DAOBase;

import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import static com.google.appengine.api.labs.taskqueue.TaskOptions.Builder.*;

public class DAO extends DAOBase
{
    static {
        ObjectifyService.factory().register(User.class);
        ObjectifyService.factory().register(Root.class);
        ObjectifyService.factory().register(Vote.class);
        ObjectifyService.factory().register(SocialEvent.class);
        ObjectifyService.factory().register(Follow.class);
        ObjectifyService.factory().register(Link.class);
        ObjectifyService.factory().register(ScoredLink.class);        
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
    }

    public static DAO instance = new DAO();
	public User user = null;
	public static final Logger log = Logger.getLogger(DAO.class.getName());
        
    public User findUserByLogin(String email, String domain) {
    	return ofy().query(User.class).filter("email", email).filter("domain", domain).get();
	}
    
    public User findRNAUser() {
    	Objectify o = ofy();
    	
    	Query<User> q = o.query(User.class).filter("isRNA", true);
    	if (q.countAll() != 1) {
    		log.severe("bad rnaEditor count: " + q.countAll());
    		return null;
    	}

    	return q.get();
	}
    
	public LinkedList<Link> findVotesByUser(User u) {
		Objectify o = ofy();
		LinkedList<Link> links = new LinkedList<Link>();
		for(Link l : o.query(Link.class).ancestor(u)) {
			links.add(l);
		}
		return links;
	}

	private class LockedPeriodical {
		public Objectify transaction;
		public Periodical periodical;
		
		public LockedPeriodical(Objectify o, Periodical p) {
			this.transaction = o;
			this.periodical = p;
		}
	}

	/*
	 * Do a follow, unfollow, or cancel pending follow
	 * @param e this should be the next edition after current
	 */
	public Return doSocial(Key<User> from, Key<User> to, Edition e, boolean on) {
		LockedPeriodical lp = lockPeriodical();

		// TODO handle the case where this is the last edition
		
		if (lp == null) {
			log.warning("failed to lock for social");
			return Return.FAILED;
		}
		
		if (!lp.periodical.getCurrentEditionKey().equals(e.getPreviousKey())) {
			log.warning("Attempted to socialize in old edition");
			lp.transaction.getTxn().commit();
			return Return.NO_LONGER_CURRENT;			
		}

		if (lp.periodical.inSocialTransition) {
			log.warning("Attempted to socialize during transition");
			lp.transaction.getTxn().commit();
			return Return.TRANSITION_IN_PROGRESS;			
		}

		Return r = Return.SUCCESS;
		Objectify o = ofy();

		final Follow following = getFollow(from, to, o);
		final SocialEvent aboutToSocial = getAboutToSocial(from, to, e, o);
		
		if (on) {
			if (aboutToSocial != null && !aboutToSocial.on) {
				// cancel (delete) the pending unfollow
				o.delete(aboutToSocial);
				r = Return.PENDING_UNFOLLOW_CANCELLED;				
			}			
			else if (aboutToSocial != null && aboutToSocial.on) {
				log.warning("Already about to follow: [" + from + ", " + to + "]");
				r = Return.ALREADY_ABOUT_TO_FOLLOW;				
			}
			else if (following != null) {
				log.warning("Already following: [" + from + ", " + to + "]");
				r = Return.ALREADY_FOLLOWING;
			}
			else {
				// this follow won't take effect until a transition
				final SocialEvent follow = new SocialEvent(from, to, e.getKey(), new Date(), on);
				r = Return.ABOUT_TO_FOLLOW;
				o.put(follow);
			}
		}
		else if (following != null) {
			// this unfollow won't take effect until a transition
			final SocialEvent unfollow = new SocialEvent(from, to, e.getKey(), new Date(), on);
			o.put(unfollow);
			r = Return.ABOUT_TO_UNFOLLOW;			
		}
		else if (aboutToSocial != null) {
			// cancel (delete) the pending follow
			o.delete(aboutToSocial);
			r = Return.PENDING_FOLLOW_CANCELLED;
		}
		else {
			log.warning("Can't unfollow unless following: " + from + ", " + to + ", " + e);
			r = Return.NOT_FOLLOWING;
		}
		
		lp.transaction.getTxn().commit();
		return r;
	}


	public Follow getFollow(Key<User> from, Key<User> to, Objectify o) {
		if (o == null)
			o = instance.ofy();

		return o.query(Follow.class).ancestor(from).filter("judge", to).get();
	}

	public SocialEvent getAboutToSocial(Key<User> from, Key<User> to, Edition e, Objectify o) {
		if (o == null)
			o = instance.ofy();

		return o.query(SocialEvent.class).ancestor(from).filter("judge", to).filter("edition", e.getKey()).get();
	}
	
	public boolean isFollowingOrAboutToFollow(Key<User> from, Key<User> to) {
		Edition e = getEdition(Name.JOURNALISM, -2, null);
		SocialEvent about = getAboutToSocial(from, to, e, null);
		
		if (about != null) {
			return about.on;
		}
		else {
			Follow f = getFollow(from, to, null);
			return f != null;
		}
	}

	private LockedPeriodical lockPeriodical() {
		Objectify oTxn = fact().beginTransaction();
		Periodical p = null;

		for(int i = 0;i < RETRY_TIMES;i++)
			try {
				Root root = ofy().find(Root.class, 1);
				p = oTxn.query(Periodical.class).ancestor(root).get();
				return new LockedPeriodical(oTxn, p);
			}
		catch(Error e) {
			log.warning("lock failed, i = " + i);
		}
		return null;
	}
	
	/**
	 * Store a new vote in the DB by a user for a link
	 * 
	 * @param r the user voting
	 * @param e the edition during which the vote occurs
	 * @param l the link voted for
	 * @throws IllegalArgumentException
	 */
	public Return voteFor(User u, Edition e, Link l, Boolean on) throws IllegalArgumentException {
		// TODO only judges can vote, ditto for ed follows
		
		// obtain lock
		LockedPeriodical lp = lockPeriodical();

		if (lp == null || e == null) {
			log.warning("vote failed: " + u + " -> " + l.url);
			return Return.FAILED;
		}

		if (!lp.periodical.getCurrentEditionKey().equals(e.getKey())) {
			log.warning("Attempted to vote in old edition");
			lp.transaction.getTxn().rollback();
			return Return.NO_LONGER_CURRENT;
		}

		if (lp.periodical.inSocialTransition) {
			log.warning("Attempted to vote during transition");
			lp.transaction.getTxn().rollback();
			return Return.TRANSITION_IN_PROGRESS;
		}

		if (on) {
			if (hasVoted(u, e, l)) {
				lp.transaction.getTxn().rollback();
				return Return.ALREADY_VOTED;
			}

			int authority = ofy().query(Follow.class).filter("judge", u.getKey()).countAll();

			ofy().put(new Vote(u.getKey(), e.getKey(), l.getKey(), new Date(), authority));

			log.info(u + " " + authority + " -> " + l.url);

			// release lock
			lp.transaction.getTxn().commit();

			return Return.SUCCESS;
		}
		else {
			Vote v = ofy().query(Vote.class).filter("edition", e.getKey()).filter("link", l.getKey()).get();
			if (v == null) {
				lp.transaction.getTxn().rollback();
				return Return.HAS_NOT_VOTED;
			}
			else {
				ofy().delete(v);
				return Return.SUCCESS;
			}
		}
	}
	
    public boolean hasVoted(User u, Edition e, Link l) {
    	Objectify o = ofy();
    	int count =  o.query(Vote.class).ancestor(u).filter("edition", e.getKey()).filter("link", l.getKey()).countAll();
    	if(count > 1) {
    		log.severe("too many eventPanel for user " + u);
    	}
		return count == 1;
	}

	// clients should call convenience methods above
    public <T> T findByFieldName(Class<T> clazz, Name fieldName, Object value, Objectify o) {
    	if (o == null)
    		o = ofy();
    	return o.query(clazz).filter(fieldName.name, value).get();
    }
	
	/*
	 * makes pending social actions current.  part of the transition machinery.
	 */
	public void socialTransition(Edition to) {
		Objectify o = ofy();

		// obtain lock
		LockedPeriodical lp = lockPeriodical();
		if (lp == null) {
			throw new IllegalStateException("failed to lock");
		}

		log.info("Social transition into " + to);

		for (SocialEvent s : o.query(SocialEvent.class).filter("edition", to.getKey())) {
			Follow old = ofy().query(Follow.class).ancestor(s.editor).filter("judge", s.judge).get();

			if (s.on == false) {
				if (old == null) {
					log.warning("Permitted unfollow when not following" + s);
				}
				else {
					o.delete(old);
					log.info("Unfollowed: " + old);
				}
			}
			else {
				if (old != null) {
					log.warning("Permitted follow when already following" + s);
				}
				else {
					// put new follow into effect
					Follow f = new Follow(s.editor, s.judge, s.getKey());
					o.put(f);
				}
			}
		}
		
		lp.periodical.inSocialTransition = false;
		lp.transaction.put(lp.periodical);
		lp.transaction.getTxn().commit();
	}

	public boolean isExpired(Edition e) {
		Perishable expiry = Config.injector.getInstance(PerishableFactory.class).create(e.end);
		return expiry.isExpired();
	}

	private ArrayList<Edition> findEditionsByPeriodical(Periodical p, Objectify o) {
		ArrayList<Edition> editions = new ArrayList<Edition>();
		if (o == null)
			o = ofy();
		
		for (Edition e : ofy().query(Edition.class).filter("periodical", p).fetch()) {
			editions.add(e);
		}
				
		if (editions.size() == 0) {
			log.warning("PERIODICAL: No editions");
			return editions;
		}

		Collections.sort(editions);
		
		return editions;
	}


	
	/*
	 * Just get the requested edition
	 * @param number the edition number requested, or -1 for current, or -2 for next
	 */
	public Edition getEdition(Name periodicalName, int number, Objectify o) {
		if (o == null)
			o = ofy();
		
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, o);

		if (p == null)
			return  null;

		if (number == -1) {
			return ofy().find(p.getCurrentEditionKey());
		}

		if (number == -2) {
			return ofy().find(Edition.getNextKey(p.getCurrentEditionKey().getName()));			
		}
		
		// TODO this only works because we assume one periodical
		return ofy().find(Edition.class, ""+number);
	}

	public Edition getCurrentEdition(Name periodicalName) {
		return getEdition(periodicalName, -1, null);
	}
	
	public Edition getNextEdition(Name periodicalName) {
		return getEdition(periodicalName, -2, null);
	}
	
	// TODO cache this
	public int getNumEditions(Name periodicalName) {
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, null);

		if (p == null) {
			log.severe("Can't find periodical: " + periodicalName);
			return  0;
		}

		return getNumEditions(p);
	}
	
	public int getNumEditions(Periodical p) {		
		int result = ofy().query(Edition.class).filter("periodical", p.getKey()).countAll();
		return result;
	}
	
	public RecentVotes getRecentVotes(int edition, Name name) {
		Edition e = getEdition(name, edition, null);
		// TODO handle bad edition number
		RecentVotes s = new RecentVotes();
		s.edition = e;
		s.numEditions = getNumEditions(name);
		if (e == null)
			return s;
		s.votes = getLatestUser_Links(e);
		return s;
	}

	/* Runs three queries: first get keys, then use the keys to get 2 sets of entities
	 * 
	 */
	public LinkedList<User_Link> getLatestUser_Links(Edition e) {
		LinkedList<User_Link> result = new LinkedList<User_Link>();
		
		ArrayList<Key<User>> users = new ArrayList<Key<User>>();
		ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();
		
		Query<Vote> q = ofy().query(Vote.class).filter("edition", e.getKey()).order("-time");
		
		for (Vote v : q) {
			users.add(v.voter);
			links.add(v.link);
		}
		Map<Key<User>, User> umap = ofy().get(users);
		Map<Key<Link>, Link> lmap = ofy().get(links);
		
		for(int i = 0;i < users.size();i++) {
			result.add(new User_Link(umap.get(users.get(i)), lmap.get(links.get(i))));
		}

		return result;
	}

	public RecentSocials getRecentSocials(Edition edition, Edition nextEdition, Name name) {
		RecentSocials s = new RecentSocials();
		s.edition = edition;
		s.numEditions = getNumEditions(name);
		s.socials = getLatestEditor_Judges(nextEdition);
		return s;
	}

	


	public RecentStories getTopStories(int editionNum, Name name) {
		// TODO error checking
		
		Edition e = getEdition(name, editionNum, null);
		LinkedList<ScoredLink> scored = getScoredLinks(e);
		LinkedList<Key<Link>> linkKeys = new LinkedList<Key<Link>>();
		
		for(ScoredLink sl : scored) {
			linkKeys.add(sl.link);
		}
		
		Map<Key<Link>, Link> linkMap = ofy().get(linkKeys);

		// for the submitter of each vote
		LinkedList<Key<User>> userKeys = new LinkedList<Key<User>>();
		
		for(Link l : linkMap.values()) {
			userKeys.add(l.submitter);
		}
		
		Map<Key<User>, User> userMap = ofy().get(userKeys);

		LinkedList<StoryInfo> stories = new LinkedList<StoryInfo>();
		
		for(ScoredLink sl : scored) {
			StoryInfo si = new StoryInfo();
			si.link = linkMap.get(sl.link);
			si.score = sl.score;
			si.submitter = userMap.get(si.link.submitter);
			stories.add(si);
		}

		RecentStories result = new RecentStories();
		result.edition = e;
		result.numEditions = getNumEditions(name);
		result.stories = stories;
		
		return result;
	}

	
	/* Runs three queries: first get keys, then use the keys to get 2 sets of entities
	 * 
	 */
	private LinkedList<SocialInfo> getLatestEditor_Judges(Edition e) {
		LinkedList<SocialInfo> result = new LinkedList<SocialInfo>();
		
		ArrayList<Key<User>> editors = new ArrayList<Key<User>>();
		ArrayList<Key<User>> judges = new ArrayList<Key<User>>();
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		// think - does this show everything we want?
		Query<SocialEvent> q = ofy().query(SocialEvent.class).filter("edition", e).order("-time");
		
		for (SocialEvent event : q) {
			editors.add(event.editor);
			judges.add(event.judge);
			bools.add(event.on);
		}
		Map<Key<User>, User> umap = ofy().get(editors);
		Map<Key<User>, User> lmap = ofy().get(judges);
		
		for(int i = 0;i < editors.size();i++) {
			result.add(new SocialInfo(umap.get(editors.get(i)), lmap.get(judges.get(i)), bools.get(i)));
		}

		return result;

	}

	public LinkedList<User_Authority> getVoters(Link l, Edition e) {
		LinkedList<User_Authority> result = new LinkedList<User_Authority>();

		Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
		ArrayList<Key<User>> voters = new ArrayList<Key<User>>();

		for (Vote v : ofy().query(Vote.class).filter("link", l.getKey()).filter("edition", e.getKey())) {
			authorities.put(v.voter, v.authority);
			voters.add(v.voter);
		}

		if (voters.size() == 0) {
			log.warning("requested voters for empty link " + l);
			return result;
		}
		
		Map<Key<User>, User> vmap = ofy().get(voters);

		for(int i = 0;i < voters.size();i++) {
			result.add(new User_Authority(vmap.get(voters.get(i)), authorities.get(voters.get(i))));
		}
		
		Collections.sort(result);
		return result;
	}
	
	public UserInfo getUserInfo(Name periodical, Key<User> user) {
		UserInfo ui = new UserInfo();
		try {
			ui.user = ofy().get(user);
			ui.votes = getLatestVote_Links(user);
			return ui;
		} catch (NotFoundException e1) {
			return null;
		}
	}

	private LinkedList<Vote_Link> getLatestVote_Links(Key<User> user) {
		LinkedList<Vote_Link> result = new LinkedList<Vote_Link>();
		
		ArrayList<Vote> votes = new ArrayList<Vote>();
		ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();
		
		Query<Vote> q = ofy().query(Vote.class).ancestor(user).order("-time");
		
		for (Vote v : q) {
			votes.add(v);
			links.add(v.link);
		}
		Map<Key<Link>, Link> lmap = ofy().get(links);
		
		for(int i = 0;i < votes.size();i++) {
			result.add(new Vote_Link(votes.get(i), lmap.get(links.get(i))));
		}

		return result;
	}

	public RelatedUserInfo getRelatedUserInfo(Name periodical, User from, Key<User> to) {
		UserInfo ui = getUserInfo(periodical, to);
		RelatedUserInfo rui = new RelatedUserInfo();
		rui.userInfo = ui;
		rui.following = from != null? isFollowingOrAboutToFollow(from.getKey(), to) : false;
		return rui;
	}

	private static int RETRY_TIMES = 20; 

	public boolean transitionEdition(Name periodicalName) {
		
		LockedPeriodical locked = lockPeriodical();

		if (locked == null) {
			log.warning("publish failed");
			return false;
		}
		
		_transitionEdition(locked);
		return true;
	}
	
	private void _transitionEdition(LockedPeriodical lp) {
		
		final Periodical p = lp.periodical;

		if (p == null) {
			log.severe("No Periodical");
			return;
		}

		if (!p.live) {
			log.warning("tried to publish edition of a dead periodical");
		}

		Edition current = ofy().find(p.getCurrentEditionKey());
		
		if (current == null) {
			log.severe("no edition matching" + p.getCurrentEditionKey());
			return;	
		}
		
		int nextNum = current.number + 1;
		int n = getNumEditions(p);
		
		if (nextNum == n) {
			p.live = false;
		}
		else if (nextNum > n) {
			log.severe("bug in edition numbers");
			return;
		}
		else {
			// change current edition
			p.setcurrentEditionKey(new Key<Edition>(Edition.class, ""+nextNum));
		}
		
		p.inSocialTransition = true;
		
		ofy().put(p);
		lp.transaction.getTxn().commit();
		
		log.info(p.name + ": current Edition:" + nextNum);
	}
	
	public void finalizeTally(Key<Edition> e) {
		tally(e);
	}
	
	public void tally(Key<Edition> e) {
		
		Map<Key<Link>, ScoredLink> links = new HashMap<Key<Link>, ScoredLink>();
		
		for (Vote v : ofy().query(Vote.class).filter("edition", e).fetch()) {
			if (links.containsKey(v.link)) {
				ScoredLink sl = links.get(v.link);
				sl.score += v.authority;
				// links.put(v.link, sl);
			}
			else {
				links.put(v.link, new ScoredLink(e, v.link, v.authority));
			}
		}
		
		clearTally(e);
		if (links.size() > 0) {
			ofy().put(links.values());
		}
	}

	private void clearTally(Key<Edition> e) {
		LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
		for (ScoredLink sl : ofy().query(ScoredLink.class).filter("edition", e).fetch()) {
			result.add(sl);
		}	
		ofy().delete(result);
	}
	
	public LinkedList<ScoredLink> getScoredLinks(Edition e) {
		LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
		for (ScoredLink sl : ofy().query(ScoredLink.class).filter("edition", e.getKey()).order("-score").fetch()) {
			result.add(sl);
		}
		return result;
	}

	public Link createLink(String url, String title, Key<User> submitter) {
    	if (submitter == null) {
    		log.warning("tried to create link without submitter: " + url);
    		return null;
    	}
    	else {
    		String domain;
			try {
				domain = new java.net.URL(url).getHost();
			} catch (MalformedURLException e) {
				log.warning("bad url " + url);
				return null;
			}
    		Link l = new Link(url, title, domain, submitter);
    		ofy().put(l);
    		return l;
    	}	
	}

}