package org.rapidnewsawards.shared;

import java.util.Date;

import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;

@Cached
public class Vote implements IsSerializable {
	@Parent
	public Key<User> voter;

	public Key<Edition> edition;
	
	public Key<Link> link;
	
	public int authority;
	
	public Date time;
	
	@Id Long id;
	
	public Vote() {}
	
	public Vote(Key<User> voter, Key<Edition> edition, Key<Link> link, Date time, int authority) {
		this.voter = voter;
		this.edition = edition;
		this.link = link;
		this.time = time;
		this.authority = authority;
	}
	
	public Key<Vote> getKey() { return new Key<Vote>(voter, Vote.class, id); }
}
