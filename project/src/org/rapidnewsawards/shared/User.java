package org.rapidnewsawards.shared;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;



@Entity
public class User implements IsSerializable {

	@Id
	public Long id;

	public Date lastLogin;

	public String email;
	public String domain;
	
	public User() {}
	
	public User(String email, String domain) {
		this.email = email;
		this.domain = domain;
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

	@Override
	public String toString() {
		return email;
	}
	
	public Key<User> getKey() {
		return new Key<User>(User.class, id);
	}
}
