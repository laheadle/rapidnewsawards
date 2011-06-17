package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class EditorInfluence {
	@Id
	public Long id;

	public int funded;
	
	@Parent
	public Key<Edition> edition;
	public Key<User> editor;
	
	public EditorInfluence() {}
	
	public EditorInfluence(Key<Edition> edition, Key<User> editor) {
		this.edition = edition;
		this.editor = editor;
		funded = 0;
	}
}
