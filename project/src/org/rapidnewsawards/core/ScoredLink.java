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
    public Key<Root> root;
    
	public Key<Edition> edition;

	@Unindexed 
	public Key<Link> link;
	
	public int score;

	@Unindexed 
	public int revenue;
		
	public ScoredLink() {}
	
	public ScoredLink(Key<Edition> edition, Key<Root> root, 
			Key<Link> link, int score, int revenue) {
		this.root = root;
		this.edition = edition;
		this.link = link;
		this.score = score;
		this.revenue = revenue;
	}
}
