package org.rapidnewsawards.shared;

import java.util.ArrayList;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.rapidnewsawards.shared.Name;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Periodical {

	private Key<Edition> currentEditionKey;
	
	public String name;

	@Parent public Key<Root> root;
	
	@Id
	private Long id;

	/*
	 * The special editor who follows new Users without empowering them.  These follows are called Joins.
	 */
	public Key<User> rnaEditor;

	public boolean live;

	public boolean inSocialTransition;

	public boolean tallying;

	/*
	 * only called when intializing the db
	 */
	public Periodical(Name name, Key<Root> root) {
		this();
		this.root = root;
		this.inSocialTransition = false;
		this.tallying = false;
		this.live = true;
		this.name = name.name;
	}
	
	public Periodical() {}	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Key<Periodical> getKey() {
		return new Key<Periodical>(Periodical.class, id);
	}

	public void setcurrentEditionKey(Key<Edition> Key) {
		this.currentEditionKey = Key;
	}

	public Key<Edition> getCurrentEditionKey() {
		return this.currentEditionKey;
	}
	
}
