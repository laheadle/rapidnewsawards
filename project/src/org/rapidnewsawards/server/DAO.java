package org.rapidnewsawards.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Editor_Judge;
import org.rapidnewsawards.shared.Follow;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.User_Link;
import org.rapidnewsawards.shared.Vote;
import org.rapidnewsawards.shared.Periodical.EditionsIndex;

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
        ObjectifyService.factory().register(Follow.class);
        ObjectifyService.factory().register(Link.class);
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
        ObjectifyService.factory().setDatastoreTimeoutRetryCount(3);
    }

    public static DAO instance = new DAO();
	private static final Logger log = Logger.getLogger(DAO.class.getName());
        
    public User findUserByEditionAndUsername(Edition e, String username) {
    	Objectify o = ofy();
    	
    	User u = o.query(User.class).ancestor(e).filter("username", username).filter("isRNA", false).get();

    	if (u == null)
    		return null;
    	
    	return u;
	}
    
    public User findRNAUserByEdition(Edition e) {
    	Objectify o = ofy();
    	
    	Query<User> q = o.query(User.class).ancestor(e).filter("isRNA", true);
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


	public void follow(User from, User to, Objectify o, boolean upcoming) {
		if (o == null) {
			o = fact().beginTransaction();
			follow(from, to, o, upcoming);
			o.getTxn().commit();
			return;
		}
		
		if (isFollowing(from, to, o, upcoming)) {
			log.warning("Already following(" + upcoming + ")" + ": [" + from + ", " + to + "]");
			return;
		}
		
		Follow jt = new Follow(from.getKey(), to.getKey(), new Date(), upcoming);
		o.put(jt);
	}


	public boolean isFollowing(User from, User to, Objectify o, boolean upcoming) {
		if (o == null)
			o = instance.ofy();		

		Query<Follow> q = o.query(Follow.class).ancestor(from).filter("judge", to.getKey()).filter("upcoming", upcoming);
		int count = q.countAll();
		assert(count <= 1);
		return count == 1;
	}
	

	/**
	 * Store a new vote in the DB by a user for a link
	 * 
	 * @param r the user voting
	 * @param l the link voted for
	 * @throws IllegalArgumentException
	 */
	public void voteFor(User u, Link l) throws IllegalArgumentException {
		if (hasVoted(u, l)) {
			throw new IllegalArgumentException("Already Voted");
		}
		Objectify oTxn = fact().beginTransaction();
		oTxn.put(new Vote(u.getKey(), l.getKey(), new Date()));
		oTxn.getTxn().commit();
	}
	
    public boolean hasVoted(User u, Link l) {
    	Objectify o = ofy();
    	int count =  o.query(Vote.class).ancestor(u).filter("link", l.getKey()).countAll();
    	if(count > 1) {
    		log.severe("too many eventPanel for user " + u);
    	}
		return count == 1;
	}

	// clients should call convenience methods above
    private <T> T findByFieldName(Class<T> clazz, Name fieldName, Object value) {
    	return ofy().query(clazz).filter(fieldName.name, value).get();
    }

    private <T> T findBy2FieldNames(Class<T> clazz, Name fieldName, Object value, Name fieldName2, Object value2) {
    	return ofy().query(clazz).filter(fieldName.name, value).filter(fieldName2.name, value2).get();    	
    }

    // TODO this is not transactional - could result in duplicates; need parent
	public Link findOrCreateLinkByURL(String url) {
		Objectify o = ofy();
		
    	Link l = findByFieldName(Link.class, Name.URL, url);
    	if (l != null)
    		return l;		
    	else {
    		l = new Link(url);
    		o.put(l);
    		return l;
    	}
	}

	private class TransitionEdition {
		private Edition from;

		HashMap<Key<User>, Key<User>> userKeyTransitions;
		
		public TransitionEdition(Edition from) {
			this.from = from;
			userKeyTransitions = new HashMap<Key<User>, Key<User>>();
		}
		
		private void forward(Key<User> fromKey, Key<User> toKey) {
			userKeyTransitions.put(fromKey, toKey);
		}
		
		private Key<User> getForwardingKey(Key<User> k) {
			return userKeyTransitions.get(k);
		}

		public void to (Edition to, Objectify o) {
			final LinkedList<User> users = findUsersByEdition(from);

			// set up table mapping previous user keys to next user keys
			for(User u : users) {
				Key<User> fromUserKey = u.getKey();
				// generate new Key
				u.parent = to.getKey(); 
				forward(fromUserKey, u.getKey());
				u.parent = from.getKey();
			}

			// create new User relations
			
			// social graph, eventPanel
			for(User u : users) {
				// copy previous social graph [upcoming and now] to next social graph [now]
				for(Follow previous : o.query(Follow.class).ancestor(u)) {
					final Follow jtNew = new Follow(getForwardingKey(previous.editor), getForwardingKey(previous.judge), previous.time, false);
					o.put(jtNew);
				}
			}

			
			// parent, eventPanel
			for(User u : users) {
				u.parent = to.getKey();
				o.put(u);
			}
		}
	}
	
	// TODO convert this to use transactions
	public Periodical findPeriodicalByName(Name periodicalName) {
		Objectify o = ofy();
		final Periodical p = findByFieldName(Periodical.class, Name.NAME, periodicalName.name);

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

		log.info(periodicalName.name + ": current Edition:" + current);		
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

	public LinkedList<User> findUsersByEdition(Edition e) {
		LinkedList<User> users = new LinkedList<User>();
		Objectify o = ofy();
		
		for (User u : o.query(User.class).ancestor(e.getKey()).filter("isRNA", false)) {
				users.add(u);
		}

		if (users.size() == 0)
			log.warning(e + ": No users");
		
		return users;
	}

	// fills in the references
	public Edition getEdition(Integer edition, Name periodicalName) {
		final Periodical p = DAO.instance.findPeriodicalByName(periodicalName);

		if (p == null) {
	        log.severe("No Periodical found (null)");
	        return null;
		}

		if (edition == null)
			return p.getCurrentEdition();

		// a specific edition was requested
		
		if (edition > getNumEditions(periodicalName) || edition < 1) {
	        log.severe("Requested non-existent edition #" + edition);
			return null;
		}

		if (p.getCurrentEdition() != null && edition > p.getCurrentEdition().number) {
	        log.warning("Requested future edition #" + edition);			
		}
		
		Edition result = p.getEdition(edition);
		return result;
	}

	public Edition getCurrentEdition(Name periodicalName) {
		return getEdition(null, periodicalName);
	}
	
	// TODO cache this
	public int getNumEditions(Name periodicalName) {
		final Periodical p = DAO.instance.findPeriodicalByName(periodicalName);
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
		
		Query<Vote> q = ofy().query(Vote.class).ancestor(e).order("-time");
		
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

	public RecentSocials getRecentSocials(Integer edition, Name name) {
		Edition e = getEdition(edition, name);
		RecentSocials s = new RecentSocials();
		s.edition = e;
		s.numEditions = getNumEditions(name);
		s.socials = getLatestEditor_Judges(e);
		return s;
	}

	/* Runs three queries: first get keys, then use the keys to get 2 sets of entities
	 * 
	 */
	private LinkedList<Editor_Judge> getLatestEditor_Judges(Edition e) {
		LinkedList<Editor_Judge> result = new LinkedList<Editor_Judge>();
		
		ArrayList<Key<User>> editors = new ArrayList<Key<User>>();
		ArrayList<Key<User>> judges = new ArrayList<Key<User>>();
		
		Query<Follow> q = ofy().query(Follow.class).ancestor(e).filter("upcoming", false).order("-time");
		
		for (Follow f : q) {
			editors.add(f.editor);
			judges.add(f.judge);
		}
		Map<Key<User>, User> umap = ofy().get(editors);
		Map<Key<User>, User> lmap = ofy().get(judges);
		
		for(int i = 0;i < editors.size();i++) {
			result.add(new Editor_Judge(umap.get(editors.get(i)), lmap.get(judges.get(i))));
		}

		return result;

	}


}