package org.rapidnewsawards.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.Config.Name;
import org.rapidnewsawards.shared.Periodical.EditionsIndex;
import org.rapidnewsawards.shared.VotesIndex;


import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.helper.DAOBase;
import org.rapidnewsawards.shared.JudgesIndex;

public class DAO extends DAOBase
{
    static {
        ObjectifyService.factory().register(User.class);
        ObjectifyService.factory().register(VotesIndex.class);
        ObjectifyService.factory().register(EditionsIndex.class);
        ObjectifyService.factory().register(JudgesIndex.class);
        ObjectifyService.factory().register(Link.class);
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
        ObjectifyService.factory().setDatastoreTimeoutRetryCount(3);
    }

    public static DAO instance = new DAO();
	private static final Logger log = Logger.getLogger(DAO.class.getName());
        
    public User findUserByEditionAndUsername(Edition e, String username) {
    	Objectify o = ofy();
    	
    	User u = o.query(User.class).ancestor(e).filter("username", username).get();

    	if (u == null)
    		return null;
    	
    	fillRefs(u);
    	
    	return u;
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

	public void follow(User from, User to) {
		Objectify o = instance.fact().beginTransaction();

		if (isFollowing(from, to, o)) {
			throw new IllegalArgumentException("Already Following");
		}
		
		JudgesIndex i = o.query(JudgesIndex.class).ancestor(from).get();
		i.ensureState();
		i.follow(to);
		o.put(i);
		
		o.getTxn().commit();
	}
    

	public boolean isFollowing(User from, User to, Objectify o) {
		if (o == null)
			o = instance.ofy();		

		Query<JudgesIndex> q = o.query(JudgesIndex.class).ancestor(from).filter("judges =", to.getKey());
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

	public JudgesIndex findJudgesIndexByUser(User u, Objectify o) {
		if (o == null)
			o = ofy();
		return o.query(JudgesIndex.class).ancestor(u).get();
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
			to.setUsers(users);

			for(User u : users) {
				Key<User> fromUserKey = u.getKey();
				// generate new Key
				u.parent = to.getKey(); 
				forward(fromUserKey, u.getKey());
				u.parent = from.getKey();
			}

			for(User u : users) {			
				final JudgesIndex ji = findJudgesIndexByUser(u, o);
				ji.parent = getForwardingKey(u.getKey());
				LinkedList<Key<User>> toKeys = new LinkedList<Key<User>>();
				for (Key<User> k : ji.judges) {
					toKeys.add(getForwardingKey(k));
				}
				ji.judges = toKeys;
				o.put(ji);
				o.put(new VotesIndex(getForwardingKey(u.getKey())));
			}

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
		assert(editions != null && editions.size() > 0);
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
			while (current.isExpired()) {
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

		return p;
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
		for (Edition e : editions)
			e.ensureState();
		
		return editions;
	}

	public LinkedList<User> findUsersByEdition(Edition e) {
		LinkedList<User> users = new LinkedList<User>();
		Objectify o = ofy();
		
		for (User u : o.query(User.class).ancestor(e.getKey())) {
			users.add(u);
		}

		if (users.size() == 0)
			log.warning(e + ": No users");
		
		return users;
	}

	public Edition getCurrentEdition(Name periodicalName) {
		final Periodical p = DAO.instance.findPeriodicalByName(periodicalName);

		if (p == null) {
	        log.severe("No Periodical found (null)");
	        return null;
		}

		return p.getCurrentEdition();
	}

	public VotesIndex findVotesIndexByUser(User mg, Objectify o) {
		if (o == null)
			o = ofy();
		return o.query(VotesIndex.class).ancestor(mg).get();
	}


}