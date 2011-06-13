package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class FollowedBy {
	@Id
	Long id;

    @Parent
	public Key<User> judge;
	public Key<User> editor;
	public Key<Edition> edition;

	public FollowedBy() {}
	
	public FollowedBy(Key<User> judge, Key<User> editor, Key<Edition> e) {
		this.editor = editor;
		this.judge = judge;
		this.edition = e;
	}
	
	public Key<FollowedBy> getKey() {
		return new Key<FollowedBy>(this.judge, FollowedBy.class, id);
	}


}
