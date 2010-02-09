package rapidnews.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import rapidnews.shared.Edition;
import rapidnews.shared.Link;
import rapidnews.shared.Periodical;
import rapidnews.shared.Reader;
import rapidnews.shared.Reader.VotesIndex;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.OKey;
import com.googlecode.objectify.OQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.helper.DAOBase;


public class DAO extends DAOBase
{
    static {
        ObjectifyService.factory().register(Reader.class);
        ObjectifyService.factory().register(Reader.VotesIndex.class);
        ObjectifyService.factory().register(Link.class);
        ObjectifyService.factory().register(Periodical.class);
        ObjectifyService.factory().register(Edition.class);
        ObjectifyService.factory().setDatastoreTimeoutRetryCount(3);
    }

    public static DAO instance = new DAO();
	private static final Logger log = Logger.getLogger(DAO.class.getName());
    
    public <T> T get(OKey<T> key) throws EntityNotFoundException {
    	return ofy().get(key);
    }
    
    public Reader findReaderByUsername(String username, boolean fillRefs) throws EntityNotFoundException {
    	Reader r = findByFieldName(Reader.class, "username", username);
    	if (fillRefs) {
    		LinkedList<Link> votes = findVotesByReader(r, true);
    		r.setVotes(votes);
    	}
    	return r;
	}
    
	public LinkedList<Link> findVotesByReader(Reader r, boolean fillRefs) throws EntityNotFoundException {
		Objectify o = ofy();
		OQuery<VotesIndex> q = fact().createQuery(VotesIndex.class).ancestor(r);
		VotesIndex i = o.prepare(q).asSingle();
		if (i.votes == null)
			return new LinkedList<Link>();
		if (i.votes.size() > 0)
			return new LinkedList<Link>(o.get(i.votes).values());
		throw new AssertionError(); // not reached
	}

	/**
	 * Store a new vote in the DB by a reader for a link
	 * 
	 * @param r the reader voting
	 * @param l the link voted for
	 * @throws IllegalArgumentException
	 */
	public void voteFor(Reader r, Link l) throws IllegalArgumentException {
		if (hasVoted(r, l)) {
			throw new IllegalArgumentException("Already Voted");
		}
		OQuery<VotesIndex> q = fact().createQuery(VotesIndex.class).ancestor(r);
		Objectify o = fact().beginTransaction();
		VotesIndex i = o.prepare(q).asSingle();
		i.voteFor(l);
		o.put(i);
		o.getTxn().commit();
	}
	
/*	public <T> OKey<T> getOKey(Class<T> clazz, T) {
		return new OKey<T>(clazz, id);
	}
*/

    public boolean hasVoted(Reader r, Link l) {
		OQuery<VotesIndex> q = fact().createQuery(VotesIndex.class).ancestor(r).filter("votes =", l.getOKey());
		int count = ofy().prepareKeysOnly(q).count();
		assert(count <= 1);
		return count == 1;
	}

	// clients should call convenience methods above
    private <T> T findByFieldName(Class<T> clazz, String fieldName, Object value) {
    	OQuery<T> q = fact().createQuery(clazz).filter(fieldName, value);
    	return ofy().prepare(q).asSingle();
    }

    private <T> T findBy2FieldNames(Class<T> clazz, String fieldName, Object value, String fieldName2, Object value2) {
    	OQuery<T> q = fact().createQuery(clazz).filter(fieldName, value).filter(fieldName2, value2);
    	return ofy().prepare(q).asSingle();    	
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

	public Periodical findPeriodicalByName(String name, boolean fillRefs) throws EntityNotFoundException {
		final Periodical p = findByFieldName(Periodical.class, "name", name);

		if (p == null)
			return  null;

		if (!fillRefs) 
			return p;

		// initialize editions array
		final ArrayList<Edition> editions = findEditionsByPeriodical(p, true);
		assert(editions != null && editions.size() > 0);
		p.setEditions(editions);
		Edition current = get(p.getCurrentEditionKey());
		p.setcurrentEditionKey(current.getOKey());
		p.setCurrentEdition(current);

		try { 
			while (current.isExpired()) {
				// set current to the next edition after current
				int i = editions.indexOf(current);
				current = editions.get(i + 1);
				p.setcurrentEditionKey(current.getOKey());
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


	private ArrayList<Edition> findEditionsByPeriodical(Periodical p, boolean fillRefs) {
		OQuery<Edition> q = fact().createQuery(Edition.class).filter("periodicalKey", p.getOKey());
		ArrayList<Edition> editions = new ArrayList<Edition>(ofy().prepare(q).asList());
		
		for (Edition e : editions) {
			// TODO xxx fill in readers
		}
		return editions;
	}

	public Edition getCurrentEdition(String periodicalName) {
		final Periodical p;
		try {
			p = DAO.instance.findPeriodicalByName(periodicalName, true);
		} catch (EntityNotFoundException e2) {
	        log.warning("No Periodical found");
	        return null;
		}
		return p.getCurrentEdition();
	}
    

}