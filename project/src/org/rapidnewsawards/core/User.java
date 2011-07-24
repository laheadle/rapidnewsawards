package org.rapidnewsawards.core;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Unindexed;

public class User implements Serializable {
	@Unindexed private static final long serialVersionUID = 1L;

	@Id public Long id;

	@Unindexed public Date lastLogin;

	public String email;
	public String domain;

	public String nickname;

	@Unindexed public boolean isInitialized;
	
	@Unindexed public boolean isEditor;

	@Unindexed
	public String webPage;

	public static final String GMAIL = "gmail.com";

	public static final String RNA_EDITOR_EMAIL = "__rnaeditor@gmail.com";
		
	public User() {}
	
	public User(String email, String domain, boolean isEditor) {
		if (email == null || domain == null)
			return;
		this.email = email.toLowerCase();
		this.domain = domain.toLowerCase();
		this.nickname = getDisplayName();
		this.isInitialized = false;
		this.isEditor = isEditor;
		this.lastLogin = new Date();
		this.webPage = "";
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
		if(!email.equals(u0.email)) {
			return false;
		}
		if(isInitialized != u0.isInitialized) {
			return false;
		}
		if(isEditor != u0.isEditor) {
			return false;
		}
		if(!webPage.equals(u0.webPage)) {
			return false;
		}
		return true;
	}

	public String getDisplayName() {
		if (nickname == null || nickname.equals("")) {
			if (email.indexOf('@') == -1) {
				return "??";
			}
			return "_" + email.substring(0, email.indexOf('@'));
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
		return new Key<User>(User.class, id);
	}

	public static Key<User> createKey(long id) {
		return new Key<User>(User.class, id);
	}
}
