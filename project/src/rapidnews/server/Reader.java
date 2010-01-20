package rapidnews.server;
import java.util.LinkedList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Link;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Reader {

	@Persistent 
	private String name;
	
	@Persistent (mappedBy = "reader")
	private LinkedList<Vote> votes; // chronological order
	
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	private String username;

	public Reader(String name, String username) {
		this.name = name;
		this.username = username;
		this.votes = new LinkedList<Vote>();
	}

	public String getName() {
		return name;
	}

	public Key getKey() {
		return key;
	}

	public rapidnews.shared.Reader getDTO() {
		rapidnews.shared.Reader result = new rapidnews.shared.Reader(this.name, this.username);
		for(Vote v : this.votes) {
			result.vote(v.getDTO());
		}
		return result;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void voteFor(Link l) {
		votes.add(new Vote(this, l));
	}

	public static Reader findByUsername(String username) {
		PersistenceManager pm = PMF.getPersistenceManager();
		Query query = pm.newQuery(Reader.class);
		query.setFilter("username == un");
		query.declareParameters("String un");
		List<Reader> results = (List<Reader>) query.execute(username);
		return results.get(0); // xxx
	}
}
