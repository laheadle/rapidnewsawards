package org.rapidnewsawards.core;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;

@Entity
public class ScoreRoot {

	@Id public String id;
	
	public static Key<ScoreRoot> keyFromEditionKey (Key<Edition> key) {
		return new Key<ScoreRoot>(ScoreRoot.class, key.getName());
	}
}
