package org.rapidnewsawards.shared;

import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class JudgesIndex {
    @Id Long id; 
    @Parent
	public Key<User> parent; 
    public LinkedList<Key<User>> judges;

    public JudgesIndex() {}

	public JudgesIndex(User parent) {
    	this.parent = parent.getKey();
	}

    public void follow(User j) {
    	judges.add(j.getKey());
    }

    public void ensureState() {
    	if (judges == null)
    		judges = new LinkedList<Key<User>>();	    	
    }
    
}
