package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;

public class ShadowUser {
	@Id public long id;
	public Key<User> user;

	public ShadowUser() {}
	
	public ShadowUser(Key<User> user) {
		id = 1;
		this.user = user;
	}
}
