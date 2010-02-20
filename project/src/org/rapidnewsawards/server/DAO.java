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
		VotesIndex i = o.query(VotesIndex.class).ancestor(r).get();
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

		Objectify oTxn = fact().beginTransaction();
		VotesIndex i = oTxn.query(VotesIndex.class).ancestor(r).get();
		i.voteFor(l);
		oTxn.put(i);
		oTxn.getTxn().commit();
	}
	
/*	public <T> Key<T> getKey(Class<T> clazz, T) {
		return new Key<T>(clazz, id);
	}
*/

    public boolean hasVoted(User r, Link l) {
    	Objectify o = ofy();
    	int count =  o.query(VotesIndex.class).ancestor(r).filter("votes =", l).countAll();
    	assert(count <= 1);
		return count == 1;
	}

	// clients should call convenience methods above
    private <T> T findByFieldName(Class<T> clazz, String fieldName, Object value) {
    	return ofy().query(clazz).filter(fieldName, value).get();
    }

    private <T> T findBy2FieldNames(Class<T> clazz, String fieldName, Object value, String fieldName2, Object value2) {
    	return ofy().query(clazz).filter(fieldName, value).filter(fieldName2, value2).get();    	
    }

    // TODO this is not transactional - could result in duplicates; need parent
	public Link findOrCreateLinkByURL(String url) {
		Objectify o = ofy();
		
    	Link l = findByFieldName(Link.class, "url", url);
    	if (l != null)
    		return l;		
    	else {
    		l = new Link(url);
    		o.put(l);
    		return l;
    	}
	}

	// TODO convert this to use transactions
	public Periodical findPeriodicalByName(String name) throws EntityNotFoundException {
		Objectify o = ofy();
		final Periodical p = findByFieldName(Periodical.class, "name", name);

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
				current = editions.get(i + 1);
				p.setcurrentEditionKey(current.getKey());
				p.setCurrentEdition(current);
				o.put(p);
				assert(ofy().get(p.getCurrentEditionKey()).equals(current));    	
			}
		}
		catch (IndexOutOfBoundsException e) {
			// periodical has ended
			// TODO should this put a periodical with no current edition?
			p.setcurrentEditionKey(null);
			p.setCurrentEdition(null);
		}

		p.verifyState();
		return p;
	}


	private Edition getEdition(Key<Edition> key) {
		Objectify o = ofy();
		Edition e = o.find(key);

		if (e == null) {
			log.warning("GetEdition: No edition");
			return null;
		}

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
		
		for (Edition e : editions) {
			fillRefs(e);
		}
		
		return editions;
	}

	public LinkedList<User> findUsersByEdition(Edition e) {
		LinkedList<User> users = new LinkedList<User>();
		Objectify o = ofy();
		
		for (User r : o.query(User.class).ancestor(e.getKey())) {
			users.add(r);
		}

		if (users.size() == 0)
			log.warning(e + ": No users");
		
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