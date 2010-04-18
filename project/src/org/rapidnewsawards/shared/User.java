package org.rapidnewsawards.shared;
import javax.persistence.Entity;
import javax.persistence.Id;

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

	public boolean isRNA;
		
	public User() {}
	
	public User(Edition e, String name, String username, boolean isRNA) {
		this.parent = e.getKey();
		this.name = name;
		this.username = username;
		this.isRNA = isRNA;
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
}
