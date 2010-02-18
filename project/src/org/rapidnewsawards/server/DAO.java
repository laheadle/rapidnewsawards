package org.rapidnewsawards.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Periodical;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.Periodical.EditionsIndex;
import org.rapidnewsawards.shared.User.JudgesIndex;
import org.rapidnewsawards.shared.User.VotesIndex;


import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.helper.DAOBase;


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
    
    public <T> T get(Key<T> key) throws EntityNotFoundException {
    	return ofy().get(key);
    }
    
    public User findUserByUsername(String username) throws EntityNotFoundException {
    	User r = findByFieldName(User.class, "username", username);

    	if (r == null)
    		return null;
    	
    	fillRefs(r);
    	return r;
	}
    
	private void fillRefs(User r) {
		LinkedList<Link> votes = findVotesByUser(r);
		r.setVotes(votes);		
	}

	public LinkedList<Link> findVotesByUser(User r) {
		Objectify o = ofy();
		VotesIndex i = ofy().query(VotesIndex.class).ancestor(r).get();
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
	public void voteFor(User r, Link l) throws IllegalArgumentException {
		if (hasVoted(r, l)) {
			throw new IllegalArgumentException("Already Voted");
		}
		Query<VotesIndex> q = ofy().query(VotesIndex.class).ancestor(r);
		Objectify o = fact().beginTransaction();
		VotesIndex i = q.get();
		i.voteFor(l);
		o.put(i);
		o.getTxn().commit();
	}
	
/*	public <T> Key<T> getKey(Class<T> clazz, T) {
		return new Key<T>(clazz, id);
	}
*/

    public boolean hasVoted(User r, Link l) {
		Query<VotesIndex> q = ofy().query(VotesIndex.class).ancestor(r).filter("votes =", l.getKey());
		int count = q.countAll();
		assert(count <= 1);
		return count == 1;
	}

	// clients should call convenience methods above
    private <T> T findByFieldName(Class<T> clazz, String fieldName, Object value) {
    	Query<T> q = ofy().query(clazz).filter(fieldName, value);
    	return q.get();
    }

    private <T> T findBy2FieldNames(Class<T> clazz, String fieldName, Object value, String fieldName2, Object value2) {
    	Query<T> q = ofy().query(clazz).filter(fieldName, value).filter(fieldName2, value2);
    	return q.get();    	
    }

	public Link findOrCreateLinkByURL(String url) {
    	Link l = findByFieldName(Link.class, "url", url);
    	if (l != null)
    		return l;		
    	else {
    		l = new Link(url);
    		ofy().put(l);
    		return l;
    	}
	}

	public Periodical findPeriodicalByName(String name) throws EntityNotFoundException {
		final Periodical p = findByFieldName(Periodical.class, "name", name);

		if (p == null)
			return  null;

		// initialize editions array
		final ArrayList<Edition> editions = findEditionsByPeriodical(p);
		assert(editions != null && editions.size() > 0);
		p.setEditions(editions);
		
		// initialize current edition
		Edition current = getEdition(p.getCurrentEditionKey());
		p.setcurrentEditionKey(current.getKey());
		p.setCurrentEdition(current);

		try { 
			while (current.isExpired()) {
				// set current to the next edition after current
				int i = editions.indexOf(current);
				current = editions.get(i + 1);
				p.setcurrentEditionKey(current.getKey());
				p.setCurrentEdition(current);
				ofy().put(p);
				assert(get(p.getCurrentEditionKey()).equals(current));    	
			}
		}
		catch (IndexOutOfBoundsException e) {
			// periodical has ended
			p.setcurrentEditionKey(null);
			p.setCurrentEdition(null);
		}

		p.verifyState();
		return p;
	}


	private Edition getEdition(Key<Edition> key) {
		Edition e = ofy().find(key);
		fillRefs(e);
		return e;
	}

	void fillRefs(Edition e) {
		LinkedList<User> users = findUsersByEdition(e);
		e.setUsers(users);
		e.ensureState();
	}
	
	
	private ArrayList<Edition> findEditionsByPeriodical(Periodical p) {
		ArrayList<Edition> editions = new ArrayList<Edition>();
		Query<EditionsIndex> q = ofy().query(EditionsIndex.class).ancestor(p);
		EditionsIndex i = q.get();
		if (i.editions == null) {
			log.warning("PERIODICAL: No editions");
			return editions;
		}
		if (i.editions.size() > 0)
			editions = new ArrayList<Edition>(ofy().get(i.editions).values());
		else
			throw new AssertionError(); // not reached
		
		for (Edition e : editions) {
			fillRefs(e);
		}
		
		return editions;
	}

	public LinkedList<User> findUsersByEdition(Edition e) {
		LinkedList<User> users = new LinkedList<User>();
		for (User r : ofy().query(User.class).ancestor(e.getKey())) {
			users.add(r);
		}
		return users;
	}

	public Edition getCurrentEdition(String periodicalName) {
		final Periodical p;
		try {
			p = DAO.instance.findPeriodicalByName(periodicalName);
		} catch (EntityNotFoundException e2) {
	        log.warning("No Periodical found");
	        return null;
		}
		return p.getCurrentEdition();
	}


}