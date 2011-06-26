package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class JudgeInfluence {

	@Id
	public Long id;

	public int authority; 
	public int score;
	
	@Parent
	public Key<Edition> edition;
	public Key<User> user;

	public JudgeInfluence() {}
	
	public JudgeInfluence(int authority, Key<Edition> edition, Key<User> user) {
		this.authority = authority;
		this.score = 0;
		this.edition = edition;
		this.user = user;
	}
	
}
