package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class EditionUserAuthority {

	@Id
	public Long id;

	public int authority;
	
	@Parent
	public Key<Edition> edition;
	public Key<User> user;

	public EditionUserAuthority() {}
	
	public EditionUserAuthority(int authority, Key<Edition> edition, Key<User> user) {
		this.authority = authority;
		this.edition = edition;
		this.user = user;
	}
	
}
