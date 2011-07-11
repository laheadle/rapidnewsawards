package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

public class EditorVote {
	@Id Long id;
	@Parent
	public Key<Vote> vote;

	@Unindexed 
	public Key<User> judge;
	public Key<User> editor;
	public Key<Link> link;
	public Key<Edition> edition;

	public EditorVote() {}

	public EditorVote(Key<Vote> v, Key<User> ed, Key<User> judge, Key<Link> link, Key<Edition> edition) {
		this.vote = v;
		this.editor = ed;
		this.judge = judge;
		this.link = link;
		this.edition = edition;
	}

}
