package org.rapidnewsawards.core;

import java.io.Serializable;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

public class JudgeInfluence implements Serializable {

	@Unindexed private static final long serialVersionUID = 1L;

	@Id public Long id;

	@Unindexed public int authority; 
	@Unindexed public int score;
	
	@Parent public Key<ScoreSpace> parentSpace;
	public Key<User> user;


	public JudgeInfluence() {}
	
	public JudgeInfluence(int authority, Key<ScoreSpace> parentSpace, Key<User> user) {
		this.authority = authority;
		this.score = 0;
		this.parentSpace = parentSpace;
		this.user = user;
	}
	
}
