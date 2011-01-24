package org.rapidnewsawards.server;

import java.net.MalformedURLException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.AllEditions;
import org.rapidnewsawards.shared.Donation;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.EditionUserAuthority;
import org.rapidnewsawards.shared.TopJudges;
import org.rapidnewsawards.shared.TopStories;
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
import org.rapidnewsawards.shared.User_Vote_Link;
import org.rapidnewsawards.shared.Vote;
import org.rapidnewsawards.shared.VoteResult;
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
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.JsonElement;

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
        ObjectifyService.factory().register(Donation.class);
        ObjectifyService.factory().register(ScoredLink.class);        
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
        ObjectifyService.factory().register(EditionUserAuthority.class);
    }

    public static DAO instance = new DAO();
	public User user = null;
	public static final Logger log = Logger.getLogger(DAO.class.getName());
        
    public User findUserByLogin(String email, String domain) {
    	if (email == null || domain == null)
    		return null;
    	email = email.toLowerCase();
    	domain = domain.toLowerCase();
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
			else if (!isEditor(from)) {
				r = Return.NOT_AN_EDITOR;
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


	private boolean isEditor(Key<User> from) {
		User u = ofy().find(from);
		if (u == null) {
			log.severe("bad input");
			return false;
		}		
		return u.isEditor;
	}

	public Follow getFollow(Key<User> from, Key<User> to, Objectify o) {
		if (o == null)
			o = instance.ofy();

		return o.query(Follow.class).ancestor(from).filter("judge", to).get();
	}

	public SocialEvent getAboutToSocial(Key<User> from, Key<User> to, Edition e, Objectify o) {
		if (o == null)
			o = instance.ofy();

		if (e == null)
			return null;
		
		return o.query(SocialEvent.class).filter("editor", from).filter("judge", to).filter("edition", e.getKey()).get();
	}
	
	public boolean isFollowingOrAboutToFollow(Key<User> from, Key<User> to) {
		Edition e = getEdition(Name.AGGREGATOR_NAME, -2, null);
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

			ofy().put(new Vote(u.getKey(), e.getKey(), l.getKey(), new Date(), u.authority));
			TallyTask.scheduleImmediately();
			
			log.info(u + " " + u.authority + " -> " + l.url);

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
				TallyTask.scheduleImmediately();
				// release lock
				lp.transaction.getTxn().commit();
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

		for (SocialEvent s : o.query(SocialEvent.class).filter("edition", to.getKey()).filter("editor !=", User.getRNAEditor())) {
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
		Perishable expiry = new Calendar(e.end); //Config.injector.getInstance(PerishableFactory.class).create(e.end);
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


	public AllEditions getAllEditions() {
		LinkedList<Edition> ll = new LinkedList<Edition>();
		for (Edition e : ofy().query(Edition.class)) {
			ll.add(e);
		}
		Edition c = getCurrentEdition(Name.AGGREGATOR_NAME);
		AllEditions ae = new AllEditions(ll, c);
		return ae;
	}
	
	/*
	 * Just get the requested edition
	 * @param number the edition number requested, or -1 for current, or -2 for next
	 */
	public Edition getEdition(Name periodicalName, int number, Objectify o) {
		if (periodicalName == null)
			periodicalName = Name.AGGREGATOR_NAME;
		
		if (o == null)
			o = ofy();
		
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name, o);

		if (p == null)
			return  null;

		if (number == -1) {
			if (!p.live)
				return null;
			
			return ofy().find(p.getCurrentEditionKey());
		}

		if (number == -2) {
			if (!p.live) {
				log.warning("Next edition requested for dead periodical");
				return null;
			}
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
		return p.numEditions;
	}
	
	public RecentVotes getRecentVotes(int edition, Name name) {
		Edition e = getEdition(name, edition, null);
		// TODO handle bad edition number
		RecentVotes s = new RecentVotes();
		s.edition = e;
		s.numEditions = getNumEditions(name);
		if (e == null)
			return s;
		s.votes = getLatestUser_Vote_Links(e);
		return s;
	}

	/* Runs three queries: first get keys, then use the keys to get 2 sets of entities
	 * 
	 */
	public LinkedList<User_Vote_Link> getLatestUser_Vote_Links(Edition e) {
		LinkedList<User_Vote_Link> result = new LinkedList<User_Vote_Link>();
		
		ArrayList<Key<User>> users = new ArrayList<Key<User>>();
		ArrayList<Key<Link>> links = new ArrayList<Key<Link>>();
		
		Query<Vote> q = ofy().query(Vote.class).filter("edition", e.getKey()).order("-time");

		LinkedList<Vote> votes = new LinkedList<Vote>();
		for (Vote v : q) {
			votes.add(v);
		}
		
		for (Vote v : votes) {
			users.add(v.voter);
			links.add(v.link);
		}
		
		Map<Key<User>, User> umap = ofy().get(users);
		Map<Key<Link>, Link> lmap = ofy().get(links);
		
		int i = 0;
		for(Vote v : votes) { // iterate again in the same order
			result.add(new User_Vote_Link(umap.get(users.get(i)), v, lmap.get(links.get(i))));
			i++;
		}

		return result;
	}


	public TopStories getTopStories(int editionNum, Name name) {
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
			si.revenue = sl.revenue;
			stories.add(si);
		}

		TopStories result = new TopStories();
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

		if (e == null)
			return result;
		
		ArrayList<Key<User>> editors = new ArrayList<Key<User>>();
		ArrayList<Key<User>> judges = new ArrayList<Key<User>>();
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		// think - does this show everything we want?
		// todo ignore welcomes on eds and donors
		Query<SocialEvent> q = ofy().query(SocialEvent.class).filter("edition", e.getKey()).order("-time");
		
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

	
	public void updateAuthorities() {
		Edition e = getCurrentEdition(Name.AGGREGATOR_NAME);
		LinkedList<User> users = new LinkedList<User>();
		LinkedList<EditionUserAuthority> eaus = new LinkedList<EditionUserAuthority>();
		for (User u : ofy().query(User.class).filter("isEditor", false)) {
			int tmp = ofy().query(Follow.class).filter("judge", u.getKey()).countAll();
			u.authority = tmp;
			users.add(u);
			EditionUserAuthority eua = 
				new EditionUserAuthority(u.authority, e.getKey(), u.getKey());
			eaus.add(eua);
		}

		ofy().put(users);
		ofy().put(eaus);
	}

	
	public TopJudges getTopJudges(Integer edition) {	
		Edition e = 
			DAO.instance.getEdition(Name.AGGREGATOR_NAME, 
									edition == null? -1 : edition, 
									null);
		TopJudges tj = new TopJudges();
		tj.edition = e;
		tj.numEditions = getNumEditions(Name.AGGREGATOR_NAME);
		tj.list = getJudges(e);
		return tj;
	}

	// fixme refactor with getvoters
	public LinkedList<User_Authority> getJudges(Edition e) {
		LinkedList<User_Authority> result = new LinkedList<User_Authority>();

		Map<Key<User>, Integer> authorities = new HashMap<Key<User>, Integer>();
		ArrayList<Key<User>> judges = new ArrayList<Key<User>>();

		for (EditionUserAuthority eua : 
			ofy().query(EditionUserAuthority.class).filter("edition", e.getKey())) {
			authorities.put(eua.user, eua.authority);
			judges.add(eua.user);
		}

		if (judges.size() == 0) {
			log.warning("requested judges for empty edition " + e);
			return result;
		}
		
		Map<Key<User>, User> vmap = ofy().get(judges);

		for(int i = 0;i < judges.size();i++) {
			result.add(new User_Authority(vmap.get(judges.get(i)), 
										  authorities.get(judges.get(i))));
		}
		
		Collections.sort(result);
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

	public void finishPeriodical(Name journalism) {

		LockedPeriodical lp = lockPeriodical();

		if (lp == null) {
			log.warning("failed");
			return;
		}
		
		final Periodical p = lp.periodical;

		if (p == null) {
			log.severe("No Periodical");
			return;
		}

		if (!p.live) {
			log.warning("tried to finish a dead periodical");
		}

		p.live = false;
		p.setcurrentEditionKey(null);
		ofy().put(p);
		
		lp.transaction.getTxn().commit();
	}

	public void setEditionRevenue() {

		LockedPeriodical lp = lockPeriodical();

		if (lp == null) {
			log.warning("failed");
			return;
		}
		
		final Periodical p = lp.periodical;

		if (p == null) {
			log.severe("No Periodical");
			return;
		}

		Edition e;

		if (!p.live) {
			log.warning("spending all remaining revenue");
			
			e = getLastEdition(p);
			
			if (e == null) {
				log.severe("no final edition");
				return;	
			}
			e.revenue = p.balance;
			p.balance = 0;
		}
		else {
			e = getPreviousEdition(lp);
			
			if (e == null) {
				log.severe("no previous edition");
				return;	
			}

			int n = getNumEditions(p);
			e.revenue = p.balance / (n - e.number);
			p.balance -= e.revenue;
		}

		ofy().put(e);
		lp.transaction.put(p);
		
		lp.transaction.getTxn().commit();

		log.info(e + ": revenue " + Periodical.moneyPrint(e.revenue));
		log.info("balance: " + Periodical.moneyPrint(p.balance));
	}

	private Edition getLastEdition(final Periodical p) {
		Edition e;
		e = ofy().find(Edition.getPreviousKey(""+getNumEditions(p)));
		return e;
	}

	private Edition getPreviousEdition(LockedPeriodical lp) {
		Key<Edition> current = lp.periodical.getCurrentEditionKey();
		if (current == null) {
			// last edition
			current = new Key<Edition>(Edition.class, ""+(getNumEditions(lp.periodical) - 1));
		}
		return ofy().find(Edition.getPreviousKey(current.getName()));
	}

	public User welcomeUser(String nickname, Integer donation) {
		log.info("welcome: " + user + ": " + Periodical.moneyPrint(donation));

		LockedPeriodical lp = lockPeriodical();

		user.nickname = nickname;
		user.isInitialized = true;
		
		ofy().put(user);
		
		Donation don = new Donation(user.getKey(), donation);
		
		ofy().put(don);

		Edition next = getNextEdition(Name.AGGREGATOR_NAME);
		
		if (next == null) {
			log.warning("join failed");
			return null;
		}
		else {
			SocialEvent join = new SocialEvent(User.getRNAEditor(), user.getKey(), next.getKey(), new Date(), true);
			ofy().put(join);
		}
		
		lp.periodical.balance += donation;
		lp.transaction.put(lp.periodical);		
		lp.transaction.getTxn().commit();

		log.info("balance: " + Periodical.moneyPrint(lp.periodical.balance));

		return user;
	}

	public static String home = "/Rapid_News_Awards.html?gwt.codesvr=127.0.0.1:9997";

	public VoteResult voteFor(String link, String fullLink, Edition edition, Boolean on) {
		VoteResult vr = new VoteResult();
        UserService userService = UserServiceFactory.getUserService();
		
		if (user == null) {
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
    		vr.returnVal = voteFor(user, edition == null? getCurrentEdition(Name.AGGREGATOR_NAME) : edition, l, on);
    		vr.authUrl = userService.createLogoutURL(home);    		
    	}
		return vr;
	}

	public VoteResult submitStory(String url, String title, Edition edition) {
		VoteResult vr = new VoteResult();
		
		if (user == null) {
			vr.returnVal = Return.NOT_LOGGED_IN;
			vr.authUrl = null; // userService.createLoginURL(fullLink);
			return vr;
		}

		// TODO broken on some complex hrefs
    	Link l = createLink(url, title, user.getKey());

    	if (l == null) {
    		return null;
    	}
    	
    	else {
    		vr.returnVal = voteFor(user, edition == null? getCurrentEdition(Name.AGGREGATOR_NAME) : edition, l, true);
    		vr.authUrl = null; // userService.createLogoutURL(home);    		
    	}
		return vr;
	}

	public RecentSocials getRecentSocials(Integer edition) {	
		Edition current = null;
		Edition next = null;
	
		if (edition == null || edition == -1) {
			current = DAO.instance.getEdition(Name.AGGREGATOR_NAME, -1, null);
			next = DAO.instance.getEdition(Name.AGGREGATOR_NAME, -2, null);
		}
		else {
			// next after edition
			current = DAO.instance.getEdition(Name.AGGREGATOR_NAME, edition, null);
			next = DAO.instance.getEdition(Name.AGGREGATOR_NAME, edition + 1, null);
		}
			
		RecentSocials s = new RecentSocials();
		s.edition = current;
		s.numEditions = getNumEditions(Name.AGGREGATOR_NAME);
		s.list = getLatestEditor_Judges(next);
		return s;
	}
	
	private void _transitionEdition(LockedPeriodical lp) {
		
		final Periodical p = lp.periodical;

		if (p == null) {
			log.severe("No Periodical");
			return;
		}

		if (!p.live) {
			log.warning("tried to publish edition of a dead periodical");
			return;
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
			log.severe("bug in edition numbers: " + nextNum);
			return;
		}
		else {
			// change current edition
			p.setcurrentEditionKey(new Key<Edition>(Edition.class, ""+nextNum));
		}
		
		p.inSocialTransition = true;
		
		ofy().put(p);
		lp.transaction.getTxn().commit();
		
		log.info(p.name + ": New current Edition:" + nextNum);
	}
	
	public Periodical tally(Edition e) {
		
		LockedPeriodical lp = lockPeriodical();

		if (lp == null) {
			throw new IllegalStateException("lock failed for tally");		
		}

		Map<Key<Link>, ScoredLink> links = new HashMap<Key<Link>, ScoredLink>();
		
		for (Vote v : ofy().query(Vote.class).filter("edition", e)
					.filter("authority >", 0)) {
			if (links.containsKey(v.link)) {
				ScoredLink sl = links.get(v.link);
				sl.score += v.authority;
			}
			else {
				links.put(v.link, new ScoredLink(e.getKey(), v.link, v.authority, 0));
			}
		}
		
		clearTally(e.getKey());
		if (links.size() > 0) {
			e.numFundedLinks = links.size();
			ofy().put(links.values());
		}				
		
		lp.transaction.getTxn().commit();
		return lp.periodical;
	}

	int revenue(int score, int totalScore, int editionFunds) {
		return (int) (score / (double) totalScore * editionFunds);
	}
	
	public void fund(Edition e) {
		
		if (e == null) {
			log.warning("cannot fund null edition");
			throw new IllegalArgumentException("Edition is null");			
		}
		
		LockedPeriodical lp = lockPeriodical();

		if (lp == null) {
			log.warning("failed to lock for tally");
			throw new IllegalStateException("lock failed for fund");
		}
		
		Map<Key<Link>, ScoredLink> links = new HashMap<Key<Link>, ScoredLink>();
		
		if (e == null) {
			log.severe("No edition");
			return;
		}
		
		int totalScore = 0;
		int numLinks = 0;
		
		Query<Vote> q = ofy().query(Vote.class).filter("edition", e.getKey())
			.filter("authority >", 0);
		
		for (Vote v : q) {
			numLinks++;
			totalScore += v.authority;
		}
		
		if (totalScore == 0) {
			lp.transaction.getTxn().commit();
			return;
		}
		
		int totalSpend = 0;
		for (Vote v : q) {
			if (links.containsKey(v.link)) {
				ScoredLink sl = links.get(v.link);
				sl.score += v.authority;
				int revenue = revenue(sl.score, totalScore, e.revenue);
				totalSpend += (revenue - sl.revenue);
				sl.revenue = revenue;
				links.put(v.link, sl);
			}
			else {
				int revenue = revenue(v.authority, totalScore, e.revenue);
				totalSpend += revenue;
				links.put(v.link, new ScoredLink(e.getKey(), v.link, v.authority, revenue));
			}
		}
		
		clearTally(e.getKey());

		if (links.size() > 0) {
			ofy().put(links.values());
			e.totalSpend = totalSpend;
			e.numFundedLinks = links.size();
			ofy().put(e);
		}

		lp.transaction.getTxn().commit();

	}


	private void clearTally(Key<Edition> e) {
		LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
		for (ScoredLink sl : ofy().query(ScoredLink.class).filter("edition", e)) {
			result.add(sl);
		}	
		ofy().delete(result);
	}
	
	public LinkedList<ScoredLink> getScoredLinks(Edition e) {
		LinkedList<ScoredLink> result = new LinkedList<ScoredLink>();
		if (e == null)
			return result;
		
		for (ScoredLink sl : ofy().query(ScoredLink.class).filter("edition", e.getKey()).order("-score")) {
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

	public Return doSocial(User to, Boolean on) {		
		if (user == null) {
			log.warning("attempt to follow with null user");
			return Return.ILLEGAL_OPERATION;
		}
		
		// read-only transaction 
		Edition e = getEdition(Name.AGGREGATOR_NAME, -2, null);
		if (e == null) {
			log.warning(user + "Attempted to socialize during final edition");
			return Return.FORBIDDEN_DURING_FINAL;			
		}
		Return result = doSocial(user.getKey(), to.getKey(), e, on);
		return result;
	}

	public void doTransition(Name aggregatorName, Integer editionNum, Objectify o) {
		Edition from = getEdition(aggregatorName, editionNum, o);

		Edition current = getCurrentEdition(Name.AGGREGATOR_NAME);
		Edition next = getNextEdition(Name.AGGREGATOR_NAME);
		
		if (from == null) {
			log.severe("Edition " + editionNum + " does not exist");
			return;
		}
		if (!from.equals(current)) {
			log.severe("edition 1 not current (2 is): " + from + ", " + current);
			return;
		}
		
		if (next == null) {
			finishPeriodical(Name.AGGREGATOR_NAME);
			log.info("End of periodical; last edition is" + current);
		}
		else {
			transitionEdition(Name.AGGREGATOR_NAME);
			socialTransition(next);
			updateAuthorities();
		}
		setEditionRevenue();
		fund(current);		
	}


}