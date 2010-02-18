package org.rapidnewsawards.shared;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Reader implements IsSerializable {

    @Parent 
    public Key<Edition> parent; 

    public String name;
		
	@Id 	
	public Long id;

	public String username;

	@Transient
	public LinkedList<Link> votes;
	
	@Entity
	public static class JudgesIndex {
	    @Id Long id; 
	    @Parent Key<Reader> parent; 
	    public LinkedList<Key<Reader>> judges;

	    public JudgesIndex() {}

		public JudgesIndex(Reader parent) {
	    	this.parent = parent.getKey();
		}

	    public void follow(Reader j) {
	    	ensureState();
	    	judges.add(j.getKey());
	    }

	    public void ensureState() {
	    	if (judges == null)
	    		judges = new LinkedList<Key<Reader>>();	    	
	    }
	    
	}
	
	@Entity
	public static class VotesIndex {
	    @Id Long id; 
	    @Parent Key<Reader> parent; 
	    public LinkedList<Key<Link>> votes;
	    
	    public void voteFor(Link l) {
	    	ensureState();
	    	votes.add(l.getKey());
	    }
	    
	    public VotesIndex(Reader parent, LinkedList<Key<Link>> votes) {
	    	this.parent = parent.getKey();
	    	this.votes = votes;
	    }

	    public VotesIndex(Reader parent) {
	    	this(parent, null);
	    }

	    public void ensureState() {
	    	if (votes == null)
	    		votes = new LinkedList<Key<Link>>();	    	
	    }
	    
	    public VotesIndex() {}

	}

	public Reader() {
		this.votes = new LinkedList<Link>();
	}
	
	public Reader(Edition e, String name, String username, LinkedList<Link> votes) {
		this.parent = e.getKey();
		this.name = name;
		this.username = username;
		this.votes = votes;
	}

	public Reader(Edition e, String name, String username) {
		this(e, name, username, new LinkedList<Link>());
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

	public Key<Reader> getKey() {
		return new Key<Reader>(this.parent, Reader.class, id);
	}

	public LinkedList<Link> getVotes() {
		return votes;
	}

	public void setVotes(LinkedList<Link> votes) {
		this.votes = votes; 
	}

}
