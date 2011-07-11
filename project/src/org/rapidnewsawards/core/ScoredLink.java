package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;

@Cached
public class ScoredLink {
	@Id
	public Long id;

    @Parent
    public Key<ScoreRoot> root;

    public Key<Link> link;
	
	public int score;

	public ScoredLink() {}
	
	public ScoredLink(Key<ScoreRoot> root, 
			Key<Link> link, int score) {
		this.root = root;
		this.link = link;
		this.score = score;
	}
}
