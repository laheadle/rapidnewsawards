package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class ScoreSpace {
	@Id
	public String id;

	@Parent 
	public Key<ScoreRoot> root;

	public int revenue;

	public int totalSpend;
	
	public int numFundedLinks;

	public int totalScore;

	public ScoreSpace() {}
	
	public ScoreSpace(String id) {
		this.root = new Key<ScoreRoot>(ScoreRoot.class, id);
		this.id = id;
		this.revenue = 0;
		this.totalScore = 0;
		this.totalSpend = 0;
		this.numFundedLinks = 0;
	}

	public static Key<ScoreSpace> keyFromEditionKey (Key<Edition> key) {
		return new Key<ScoreSpace>(
				new Key<ScoreRoot>(ScoreRoot.class, key.getName()),
				ScoreSpace.class, key.getName());
	}

}
