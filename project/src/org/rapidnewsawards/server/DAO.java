package org.rapidnewsawards.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Follow;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.State;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.VotesIndex;
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
        ObjectifyService.factory().register(VotesIndex.class);
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
    	
    	fillRefs(u);
    	
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
    
	private void fillRefs(User u) {
		LinkedList<Link> votes = findVotesByUser(u);
		u.setVotes(votes);		
	}

	public LinkedList<Link> findVotesByUser(User u) {
		Objectify o = ofy();
		VotesIndex i = o.query(VotesIndex.class).ancestor(u).get();
		if (i.votes == null)
			return new LinkedList<Link>();
		if (i.votes.size() > 0)
			return new LinkedList<Link>(o.get(i.votes).values());
		throw new AssertionError(); // not reached
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
		VotesIndex i = oTxn.query(VotesIndex.class).ancestor(u).get();
		i.ensureState();
		i.voteFor(l);
		oTxn.put(i);
		oTxn.getTxn().commit();
	}
	
/*	public <T> Key<T> getKey(Class<T> clazz, T) {
		return new Key<T>(clazz, id);
	}
*/

    public boolean hasVoted(User u, Link l) {
    	Objectify o = ofy();
    	int count =  o.query(VotesIndex.class).ancestor(u).filter("votes =", l).countAll();
    	assert(count <= 1);
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
			final LinkedList<User> users = from.getUsers();

			// set up table mapping previous user keys to next user keys
			for(User u : users) {
				Key<User> fromUserKey = u.getKey();
				// generate new Key
				u.parent = to.getKey(); 
				forward(fromUserKey, u.getKey());
				u.parent = from.getKey();
			}

			// create new User relations
			
			// social graph, votes
			for(User u : users) {
				// copy previous social graph [upcoming and now] to next social graph [now]
				for(Follow previous : o.query(Follow.class).ancestor(u)) {
					final Follow jtNew = new Follow(getForwardingKey(previous.editor), getForwardingKey(previous.judge), previous.time, false);
					o.put(jtNew);
				}
				o.put(new VotesIndex(getForwardingKey(u.getKey())));
			}

			
			// parent, votes
			for(User u : users) {
				u.parent = to.getKey();
				u.setVotes(new LinkedList<Link>()); 
				o.put(u);
			}
			to.setUsers(users);
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
				fillRefs(current);
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

	void fillRefs(Edition e) {
		LinkedList<User> users = findUsersByEdition(e);
		e.setUsers(users);
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
				fillRefs(u);
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
		fillRefs(result);
		return result;
	}

	public Edition getCurrentEdition(Name periodicalName) {
		return getEdition(null, periodicalName);
	}
	
	public VotesIndex findVotesIndexByUser(User mg, Objectify o) {
		if (o == null)
			o = ofy();
		return o.query(VotesIndex.class).ancestor(mg).get();
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
	
	public State getState(Integer edition, Name name) {
		Edition e = getEdition(edition, name);
		State s = new State();
		s.edition = e;
		s.numEditions = getNumEditions(name);
		return s;
	}


}