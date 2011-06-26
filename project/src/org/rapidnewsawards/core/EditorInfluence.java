package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class EditorInfluence {
	@Id public Long id;

	public int score;
	
	@Parent public Key<ScoreSpace> space;
	public Key<User> editor;
	
	public EditorInfluence() {}
	
	public EditorInfluence(Key<ScoreSpace> space, Key<User> editor) {
		this.space = space;
		this.editor = editor;
		score = 0;
	}
}
