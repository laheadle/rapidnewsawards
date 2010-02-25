package org.rapidnewsawards.shared;

import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class VotesIndex {
    @Id Long id; 
    @Parent Key<User> parent; 
    public LinkedList<Key<Link>> votes;
    
    public void voteFor(Link l) {
    	if (votes == null)
    		throw new AssertionError();
    	votes.add(l.getKey());
    }

    public VotesIndex(Key<User> parent, LinkedList<Key<Link>> votes) {
    	this.parent = parent;
    	this.votes = votes;
    }

    public VotesIndex(Key<User> parent) {
    	this(parent, new LinkedList<Key<Link>>());
    }

    public VotesIndex(User parent, LinkedList<Key<Link>> votes) {
    	this.parent = parent.getKey();
    	this.votes = votes;
    }

    public VotesIndex(User parent) {
    	this(parent, new LinkedList<Key<Link>>());
    }

    public void ensureState() {
    	if (votes == null)
    		votes = new LinkedList<Key<Link>>();	    	
    }
    
    public VotesIndex() {}

}
