package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

public class ScoreSpace {
	@Id
	private String id;

	@Parent 
	public Key<ScoreRoot> root;

	@Unindexed public int balance;

	@Unindexed public int totalSpend;
	
	@Unindexed public int numFundedLinks;

	@Unindexed public int totalScore;

	@Unindexed public boolean finished;
	

	public ScoreSpace() {}
	
	public ScoreSpace(String id) {
		this.root = new Key<ScoreRoot>(ScoreRoot.class, id);
		this.id = id;
		this.balance = 0;
		this.totalScore = 0;
		this.totalSpend = 0;
		this.numFundedLinks = 0;
		this.finished = false;
	}

	public static Key<ScoreSpace> keyFromEditionKey (Key<Edition> key) {
		return new Key<ScoreSpace>(ScoreRoot.keyFromEditionKey(key), ScoreSpace.class, key.getName());
	}

	public static Key<Edition> editionKeyFromKey(Key<ScoreSpace> ssKey) {
		return Edition.createKey(getNumber(ssKey));
	}

	public static int getNumber(Key<ScoreSpace> ss) {
		return Integer.valueOf(ss.getName());
	}

	public int getNumber() {
		return Integer.valueOf(id);
	}

}
