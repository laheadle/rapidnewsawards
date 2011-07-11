package org.rapidnewsawards.core;

import java.util.Date;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

@Cached
public class Vote implements Comparable<Vote> {
	@Parent
	public Key<User> voter;

	public Key<Edition> edition;
	
	public Key<Link> link;
	
	@Unindexed public int authority;
	
	public Date time;
	
	@Id 
	public Long id;

	public Vote() {}
	
	public Vote(Key<User> voter, Key<Edition> edition, Key<Link> link, 
			Date time, int authority) {
		this.voter = voter;
		this.edition = edition;
		this.link = link;
		this.time = time;
		this.authority = authority;
	}
	
	public int compareTo(Vote v) {
		return -time.compareTo(v.time);
	}

	public Key<Vote> getKey() { return new Key<Vote>(voter, Vote.class, id); }
}
