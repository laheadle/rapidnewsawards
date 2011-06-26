package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;

@Cached
public class ScoredLink {
	@Id
	public Long id;

    @Parent
    public Key<ScoreRoot> root;
    
	public Key<Edition> edition;

	public Key<Link> link;
	
	public int score;

	@Unindexed 
	private int funding;
		
	public ScoredLink() {}
	
	public ScoredLink(Key<Edition> edition, Key<ScoreRoot> root, 
			Key<Link> link, int score) {
		this.root = root;
		this.edition = edition;
		this.link = link;
		this.score = score;
	}
}
