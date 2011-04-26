package org.rapidnewsawards.shared;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Periodical {

	private Key<Edition> currentEditionKey;
	
	public String name;

	@Parent public Key<Root> root;
	
	@Id
	private Long id;

	public boolean live;

	public boolean inSocialTransition;

	public boolean tallying;

	public int balance;

	public int numEditions;
	
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
		this.balance = 500000; // 5k dollars (in pennies)
	}
	
	public Periodical() {}	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Key<Periodical> getKey() {
		return new Key<Periodical>(root, Periodical.class, id);
	}

	public void setcurrentEditionKey(Key<Edition> Key) {
		this.currentEditionKey = Key;
	}

	public Key<Edition> getCurrentEditionKey() {
		return this.currentEditionKey;
	}

	public static String moneyPrint(int amount) {
		return "$" + amount / 100 + "." + amount % 100;	
	}
	
}
