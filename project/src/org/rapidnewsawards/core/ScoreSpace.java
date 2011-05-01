package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

public class ScoreSpace {
	@Id
	public Long id;

	@Parent 
	public Key<Root> root;

	public Key<Edition> edition;

	public int revenue;

	public int totalSpend;
	
	public int numFundedLinks;

	public int totalScore;

	public ScoreSpace() {}
	
	public ScoreSpace(Key<Edition> edition, Key<Root> root) {
		this.root = root;
		this.edition = edition;		
		this.revenue = 0;
		this.totalScore = 0;
		this.totalSpend = 0;
		this.numFundedLinks = 0;
	}

}
