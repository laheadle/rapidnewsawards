package org.rapidnewsawards.shared;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;

@Entity
public class User implements IsSerializable {

    public String name;
		
	@Id
	public Long id;

	public String username;

	public boolean isRNA;
		
	public User() {}
	
	public User(String name, String username, boolean isRNA) {
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
		return new Key<User>(User.class, id);
	}
}
