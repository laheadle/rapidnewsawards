package org.rapidnewsawards.core;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

@Entity
/*
 * SocialEvent Events -- Follows, Unfollows, and Joins
 */
public class SocialEvent {
	@Id
	Long id;

    /*
     * The actor; if this field is null, then this SocialEvent is a "Join" -- somebody has joined the site but no power is granted
     */
    @Parent
	public Key<User> editor;
	public Key<Edition> edition;
	public Key<User> judge;
	public Date time;
	/*
	 * false if this is an unfollow
	 */
	@Unindexed public boolean on;
	
	public SocialEvent() {}
	
	public SocialEvent(Key<User> from, Key<User> to, Key<Edition> edition, Date date, boolean on) {
		editor = from;
		judge = to;
		this.edition = edition;
		this.time = date;
		this.on = on;
	}

	public Key<SocialEvent> getKey() {
		return new Key<SocialEvent>(SocialEvent.class, id);
	}

}
