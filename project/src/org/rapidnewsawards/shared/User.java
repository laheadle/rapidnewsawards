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
public class User implements IsSerializable {

    @Parent 
    public Key<Edition> parent; 

    public String name;
		
	@Id 	
	public Long id;

	public String username;

	@Transient
	public LinkedList<Link> votes;	
	
	public User() {}
	
	public User(Edition e, String name, String username, LinkedList<Link> votes) {
		this.parent = e.getKey();
		this.name = name;
		this.username = username;
		this.votes = votes;
	}

	public User(Edition e, String name, String username) {
		this(e, name, username, new LinkedList<Link>());
	}	

	@Override
	public boolean equals(Object u) {
		if(!(u instanceof User)) {
			return false;
		}
		User u0 = (User) u;
		if(!id.equals(u0.id)) {
			return false;
		}
		if (!parent.equals(u0.parent)) {
			return false;
		}
		return true;
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

	public Key<User> getKey() {
		return new Key<User>(this.parent, User.class, id);
	}

	public LinkedList<Link> getVotes() {
		return votes;
	}

	public void setVotes(LinkedList<Link> votes) {
		this.votes = votes; 
	}

}
