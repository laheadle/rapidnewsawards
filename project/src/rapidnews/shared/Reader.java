package rapidnews.shared;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.OKey;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Reader implements IsSerializable {

	public String name;
		
	@Id 	
	public Long id;

	public String username;

	@Transient
	public LinkedList<Link> votes;
	
	
	public static class JudgesIndex {
	    @Id Long id; 
	    @Parent OKey<Reader> parent; 
	    public LinkedList<OKey<Reader>> judges;

	    public JudgesIndex() {}

		public JudgesIndex(Reader parent) {
	    	this.parent = parent.getOKey();
		}

	    public void follow(Reader j) {
	    	ensureState();
	    	judges.add(j.getOKey());
	    }

	    public void ensureState() {
	    	if (judges == null)
	    		judges = new LinkedList<OKey<Reader>>();	    	
	    }
	    
	}
	
	
	public static class VotesIndex {
	    @Id Long id; 
	    @Parent OKey<Reader> parent; 
	    public LinkedList<OKey<Link>> votes;
	    
	    public void voteFor(Link l) {
	    	ensureState();
	    	votes.add(l.getOKey());
	    }
	    
	    public VotesIndex(Reader parent, LinkedList<OKey<Link>> votes) {
	    	this.parent = parent.getOKey();
	    	this.votes = votes;
	    }

	    public VotesIndex(Reader parent) {
	    	this(parent, null);
	    }

	    public void ensureState() {
	    	if (votes == null)
	    		votes = new LinkedList<OKey<Link>>();	    	
	    }
	    
	    public VotesIndex() {}

	}

	public Reader() {
		this.votes = new LinkedList<Link>();
	}
	
	public Reader(String name, String username, LinkedList<Link> votes) {
		this.name = name;
		this.username = username;
		this.votes = votes;
	}

	public Reader(String name, String username) {
		this(name, username, new LinkedList<Link>());
	}	

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public OKey<Reader> getOKey() {
		return new OKey<Reader>(Reader.class, id);
	}

	public LinkedList<Link> getVotes() {
		return votes;
	}

	public void setVotes(LinkedList<Link> votes) {
		this.votes = votes; 
	}

}
