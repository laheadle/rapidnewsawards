package org.rapidnewsawards.core;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

@Entity

/*
 * An Editor is Following a Judge during an Edition
 */
@Cached
public class Follow {
	@Id
	Long id;

    @Parent
	public Key<User> editor;
	public Key<User> judge;
	public Key<Edition> edition;
	
	@Unindexed
	public Key<SocialEvent> event; // the event establishing the follow

	public Follow() {}
	
	public Follow(Key<User> from, Key<User> to, Key<Edition> e, Key<SocialEvent> s) {
		editor = from;
		judge = to;
		edition = e;
		event = s;
	}
	
	public Key<Follow> getKey() {
		return new Key<Follow>(this.editor, Follow.class, id);
	}

}
