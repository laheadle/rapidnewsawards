package org.rapidnewsawards.shared;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;



@Cached
public class User implements IsSerializable {

	@Id
	public Long id;

	public Date lastLogin;

	public String email;
	public String domain;

	public String nickname;

	public boolean isInitialized;
	
	public boolean isEditor;
	
	public User() {}
	
	public User(String email, String domain, boolean isEditor) {
		if (email == null || domain == null)
			return;
		
		this.email = email.toLowerCase();
		this.domain = domain.toLowerCase();
		this.nickname = "";
		this.isInitialized = false;
		this.isEditor = isEditor;
		this.lastLogin = new Date();
	}

	/*
	 * The special editor who follows new Users without empowering them.  These follows are called Joins because the user joins the community.
	 */
	public static Key<User> getRNAEditor() {
		return new Key<User>(User.class, 1L);
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
