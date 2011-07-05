package org.rapidnewsawards.core;
import java.util.Date;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;



public class User {

	@Id public Long id;

	@Parent public Key<Root> root;
	
	public Date lastLogin;

	public String email;
	public String domain;

	public String nickname;

	public boolean isInitialized;
	
	public boolean isEditor;

	public String webPage;
		
	public User() {}
	
	public User(String email, String domain, boolean isEditor) {
		if (email == null || domain == null)
			return;
		this.root = Periodical.rootKey();
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
		return new Key<User>(Periodical.rootKey(), User.class, 1L);
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
			if (email.indexOf('@') == -1) {
				return "??";
			}
			return email.substring(0, email.indexOf('@'));
		}
		else {
			return nickname;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s) id: %s", getDisplayName(), email, (id == null? "NULL" : Long.toString(id)));
	}
	
	public Key<User> getKey() {
		return new Key<User>(Periodical.rootKey(), User.class, id);
	}

	public static Key<User> createKey(long id) {
		return new Key<User>(Periodical.rootKey(), User.class, id);
	}
}
