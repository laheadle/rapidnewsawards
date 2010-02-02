package rapidnews.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import rapidnews.shared.Edition;
import rapidnews.shared.Link;
import rapidnews.shared.Periodical;
import rapidnews.shared.Reader;
import rapidnews.shared.Vote;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.OKey;
import com.googlecode.objectify.OQuery;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.helper.DAOBase;


public class DAO extends DAOBase
{
    static {
        ObjectifyService.factory().register(Reader.class);
        ObjectifyService.factory().register(Vote.class);
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
    		LinkedList<Vote> votes = findVotesByReader(r, true);
    		r.setVotes(votes);
    	}
    	return r;
	}
    
	public LinkedList<Vote> findVotesByReader(Reader r, boolean fillRefs) throws EntityNotFoundException {
		OQuery<Vote> q = fact().createQuery(Vote.class).filter("readerKey", r.getOKey());
		LinkedList<Vote> votes = new LinkedList<Vote>(ofy().prepare(q).asList());
		for (Vote v: votes) {
			Link l = findLinkByVote(v);
			v.setLink(l);
		}
		return votes;
	}

	public Link findLinkByVote(Vote v) throws EntityNotFoundException {
		return ofy().get(v.linkKey);
	}

	public void voteFor(Reader r, Link l) throws IllegalArgumentException {
		//Objectify ofy = fact().beginTransaction();
		if (hasVoted(r, l)) {
			throw new IllegalArgumentException("Already Voted");
		}
		ofy().put(new Vote(r, l));
		//ofy.getTxn().commit();
	}
	
/*	public <T> OKey<T> getOKey(Class<T> clazz, T) {
		return new OKey<T>(clazz, id);
	}
*/

    public boolean hasVoted(Reader r, Link l) {
    	return findBy2FieldNames(Vote.class, "readerKey", r.getOKey(), "linkKey", l.getOKey()) != null;
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
		final ArrayList<Edition> editions;
		
		// initialize editions array
		if (fillRefs) {
    		editions = findEditionsByPeriodical(p, true);
    		p.setEditions(editions);
    	}
		else {
			editions = null;
		}
    	
    	Edition current = get(p.getCurrentEditionKey());

    	if (current.isExpired()) {
    		// set current to the next edition after current
    		int i = editions.indexOf(current);
    		// TODO XXX what if last one?
    		current = editions.get(i + 1);
    		p.setcurrentEditionKey(current.getOKey());
    		ofy().put(p);
    	}
    	
    	assert(get(p.getCurrentEditionKey()).equals(current));    	
		p.setCurrentEdition(current);
 
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