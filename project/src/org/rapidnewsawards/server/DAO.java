package org.rapidnewsawards.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.RecentStories;
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
import org.rapidnewsawards.shared.User_Link;
import org.rapidnewsawards.shared.Vote;
import org.rapidnewsawards.shared.Vote_Link;
import org.rapidnewsawards.shared.Periodical.EditionsIndex;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.helper.DAOBase;

public class DAO extends DAOBase
{
    static {
        ObjectifyService.factory().register(User.class);
        ObjectifyService.factory().register(Vote.class);
        ObjectifyService.factory().register(EditionsIndex.class);
        ObjectifyService.factory().register(SocialEvent.class);
        ObjectifyService.factory().register(Follow.class);
        ObjectifyService.factory().register(Link.class);
        ObjectifyService.factory().register(ScoredLink.class);        
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
        ObjectifyService.factory().setDatastoreTimeoutRetryCount(3);
    }

    public static DAO instance = new DAO();
	private static final Logger log = Logger.getLogger(DAO.class.getName());
        
    public User findUserByUsername(String username) {
    	Objectify o = ofy();
    	
    	User u = o.query(User.class).filter("username", username).filter("isRNA", false).get();

    	if (u == null)
    		return null;
    	
    	return u;
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


	/*
	 * Do a follow, unfollow, or cancel pending follow
	 * @param e this should be a future edition
	 */
	public Return doSocial(Key<User> from, Key<User> to, Edition e, Objectify o, boolean on) {
		Return r = Return.SUCCESS;
		if (o == null) {
			o = fact().beginTransaction();
			r = doSocial(from, to, e, o, on);
			o.getTxn().commit();
			return r;
		}
		
		final Follow following = getFollow(from, to, o);
		final SocialEvent aboutToSocial = getAboutToSocial(from, to, e, o);
		
		if (on) {
			// this follow won't take effect until a transition
			if (following != null) {
				log.warning("Already following: [" + from + ", " + to + "]");
				r = Return.ALREADY_FOLLOWING;
			}
			else if (aboutToSocial != null && aboutToSocial.on) {
				log.warning("Already about to follow: [" + from + ", " + to + "]");
				r = Return.ALREADY_ABOUT_TO_FOLLOW;				
			}
			else if (aboutToSocial != null) {
				// cancel (delete) the pending unfollow
				o.delete(aboutToSocial);
				r = Return.PENDING_UNFOLLOW_CANCELLED;				
			}			
			else {
				// this unfollow won't take effect until a transition
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
		Edition e = getRawEdition(Name.JOURNALISM, -2, null);
		SocialEvent about = getAboutToSocial(from, to, e, null);
		
		if (about != null) {
			return about.on;
		}
		else {
			Follow f = getFollow(from, to, null);
			return f != null;
		}
	}
	
	/**
	 * Store a new vote in the DB by a user for a link
	 * 
	 * @param r the user voting
	 * @param e the edition during which the vote occurs
	 * @param l the link voted for
	 * @throws IllegalArgumentException
	 */
	public void voteFor(User u, Edition e, Link l) throws IllegalArgumentException {
		if (hasVoted(u, e, l)) {
			throw new IllegalArgumentException("Already Voted");
		}
		
		// TODO this will race/fail unless we put a fence around the transition stuff
		int authority = ofy().query(Follow.class).filter("judge", u.getKey()).countAll();
		
		Objectify oTxn = fact().beginTransaction();
		oTxn.put(new Vote(u.getKey(), e.getKey(), l.getKey(), new Date(), authority));
		
		log.info(u.username + " " + authority + " -> " + l.url);
		oTxn.getTxn().commit();
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
    private <T> T findByFieldName(Class<T> clazz, Name fieldName, Object value, Objectify o) {
    	if (o == null)
    		o = ofy();
    	return o.query(clazz).filter(fieldName.name, value).get();
    }

    // TODO this is not transactional - could result in duplicates; need parent
	public Link findOrCreateLinkByURL(String url, Key<User> submitter) {
		Objectify o = ofy();
		
    	Link l = findByFieldName(Link.class, Name.URL, url, null);
    	if (l != null)
    		return l;		
    	else if (submitter == null) {
    		log.warning("tried to create link without submitter: " + url);
    		return null;
    	}
    	else {
    		l = new Link(url, null, submitter);
    		o.put(l);
    		return l;
    	}
	}

	private class TransitionEdition {
		private Edition from;

		public TransitionEdition(Edition current) {
			from = current;
		}

		public void to (Edition to, Objectify o) {
			log.info("Transitioning");
			for (SocialEvent s : o.query(SocialEvent.class).filter("edition", to.getKey())) {
				if (s.on == false) {
					Follow old = ofy().query(Follow.class).ancestor(s.editor).filter("judge", s.judge).get();
					if (old == null) {
						log.severe("Permitted unfollow when not following" + s);
					}
					else {
						o.delete(old);
						log.info("Unfollowed: " + old);
					}
				}
				else {
					// put new follow into effect
					Follow f = new Follow(s.editor, s.judge, s.getKey());
					o.put(f);
				}
			}
		}
	}
	
	// TODO convert this to use transactions
	public Periodical findPeriodicalByName(Name periodicalName) {
		Objectify o = ofy();
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, null);

		if (p == null)
			return  null;

		// initialize editions array
		final ArrayList<Edition> editions = findEditionsByPeriodical(p);
		if (editions == null || editions.size() == 0) {
			p.setEditions(null);
			log.warning(periodicalName.name + ": no editions");
			return p;
		}
		p.setEditions(editions);
		
		// initialize current edition
		Edition current = null;
		for (Edition e : editions) {
			if (e.getKey().equals(p.getCurrentEditionKey())) {
				current = e;
				p.setcurrentEditionKey(current.getKey());
				p.setCurrentEdition(current);
				break;
			}
		}

		if (current == null) {
			log.severe("no edition matching" + p.getCurrentEditionKey());
			return null;			
		}
		
		try { 
			while (isExpired(current)) {
				// set current to the next edition after current
				int i = editions.indexOf(current);
				Edition next = editions.get(i + 1);
				new TransitionEdition(current).to(next, o);
				current = next;
				p.setcurrentEditionKey(current.getKey());
				p.setCurrentEdition(current);
				o.put(p);
			}
		}
		catch (IndexOutOfBoundsException e) {
			// periodical has ended
			// TODO should this put a periodical with null current edition?
			p.setcurrentEditionKey(null);
			p.setCurrentEdition(null);
		}

		log.fine(periodicalName.name + ": current Edition:" + current);		
		return p;
	}

	public boolean isExpired(Edition e) {
		Perishable expiry = Config.injector.getInstance(PerishableFactory.class).create(e.end);
		return expiry.isExpired();
	}

	// does not fill in refs for editions found!
	private ArrayList<Edition> findEditionsByPeriodical(Periodical p) {
		ArrayList<Edition> editions = new ArrayList<Edition>();
		Objectify o = ofy();
		
		EditionsIndex i = o.query(EditionsIndex.class).ancestor(p).get();

		if (i == null) {
			log.warning("PERIODICAL: No editions index");
			return editions;
		}
		
		if (i.editions == null) {
			log.warning("PERIODICAL: No editions");
			return editions;
		}

		if (i.editions.size() > 0)
			editions = new ArrayList<Edition>(o.get(i.editions).values());
		else
			throw new AssertionError(); // not reached
				
		Collections.sort(editions);
		
		return editions;
	}


	public Edition getEdition(Integer edition, Name periodicalName) {
		final Periodical p = DAO.instance.findPeriodicalByName(periodicalName);

		if (p == null) {
	        log.severe("No Periodical found (null)");
	        return null;
		}

		if (edition == null)
			return p.getCurrentEdition();

		// a specific edition was requested
		
		if (edition > getNumEditions(periodicalName) - 1 || edition < 0) {
	        log.severe("Requested non-existent edition #" + edition);
			return null;
		}

		if (p.getCurrentEdition() != null && edition > p.getCurrentEdition().number) {
	        log.warning("Requested future edition #" + edition);			
		}
		
		Edition result = p.getEdition(edition);
		return result;
	}
	
	/*
	 * Just get the requested edition without invoking the transition machinery
	 * @param number the edition number requested, or -1 for current, or -2 for next
	 */
	public Edition getRawEdition(Name periodicalName, int number, Objectify o) {
		if (o == null)
			o = ofy();
		
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, o);

		if (p == null)
			return  null;

		ArrayList<Edition> editions = findEditionsByPeriodical(p);

		if (number == -1 || number == -2) {
			boolean done = false;
			// return current edition
			for (Edition e : editions) {
				if (done)
					return e;
				if (p.getCurrentEditionKey().equals(e.getKey())) {
					if (number == -1)
						return e;
					else
						done = true;
				}
			}
			log.info("no current edition");
			return null;			
		}
		if (number < -1 || number > editions.size() - 1) {
			log.warning("bad number: " + number);
			return null;
		}
		return editions.get(number);
	}

	public Edition getCurrentEdition(Name periodicalName) {
		return getEdition(null, periodicalName);
	}
	
	// TODO cache this
	public int getNumEditions(Name periodicalName) {
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, null);

		if (p == null)
			return  0;

		EditionsIndex i = ofy().query(EditionsIndex.class).ancestor(p).get();
		if (i == null) {
			log.severe("No Editions Index");
			return 0;
		}
		return i.editions.size();
	}
	
	public RecentVotes getRecentVotes(Integer edition, Name name) {
		Edition e = getEdition(edition, name);
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

	public RecentSocials getRecentSocials(Edition currentEdition, Edition nextEdition, Name name) {
		RecentSocials s = new RecentSocials();
		s.edition = currentEdition;
		s.numEditions = getNumEditions(name);
		s.socials = getLatestEditor_Judges(nextEdition);
		return s;
	}

	


	public RecentStories getTopStories(Integer editionNum, Name name) {
		// TODO error checking
		
		Edition e = getEdition(editionNum, name);
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
		
		for (SocialEvent f : q) {
			editors.add(f.editor);
			judges.add(f.judge);
			bools.add(f.on);
		}
		Map<Key<User>, User> umap = ofy().get(editors);
		Map<Key<User>, User> lmap = ofy().get(judges);
		
		for(int i = 0;i < editors.size();i++) {
			result.add(new SocialInfo(umap.get(editors.get(i)), lmap.get(judges.get(i)), bools.get(i)));
		}

		return result;

	}

	public UserInfo getUserInfo(Name periodical, Key<User> user) {
		UserInfo ui = new UserInfo();
		try {
			ui.user = ofy().get(user);
			ui.votes = getLatestVote_Links(user);
			return ui;
		} catch (EntityNotFoundException e1) {
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
		rui.following = isFollowingOrAboutToFollow(from.getKey(), to);
		return rui;
	}
	
	public void tally(Edition e) {
		
		Map<Key<Link>, ScoredLink> links = new HashMap<Key<Link>, ScoredLink>();
		
		for (Vote v : ofy().query(Vote.class).filter("edition", e.getKey()).fetch()) {
			if (links.containsKey(v.link)) {
				ScoredLink sl = links.get(v.link);
				sl.score += v.authority;
				// links.put(v.link, sl);
			}
			else {
				links.put(v.link, new ScoredLink(e.getKey(), v.link, v.authority));
			}
		}
		
		clearTally(e);		
		ofy().put(links.values());
	}

	private void clearTally(Edition e) {
		LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
		for (ScoredLink sl : ofy().query(ScoredLink.class).filter("edition", e.getKey()).fetch()) {
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

}