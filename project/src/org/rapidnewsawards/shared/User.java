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

	public String nickname;

	public boolean isInitialized;
	
	public User() {}
	
	public User(String email, String domain) {
		this.email = email;
		this.domain = domain;
		this.nickname = "";
		this.isInitialized = false;
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

	public String getDisplayName() {
		if (nickname == null || nickname.equals("")) {
			return email.substring(0, email.indexOf('@'));
		}
		else {
			return nickname;
		}
	}
	
	@Override
	public String toString() {
		return getDisplayName() + "(" + email + ")";
	}
	
	public Key<User> getKey() {
		return new Key<User>(User.class, id);
	}
}
