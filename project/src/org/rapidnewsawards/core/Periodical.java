package org.rapidnewsawards.core;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.rapidnewsawards.messages.Name;
import org.rapidnewsawards.server.DAO;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Periodical {

	private static final int ROOT_ID = 1;

	public Key<Edition> currentEditionKey;
	

	@Parent public Key<Root> root;
	
	@Id
	public String idName;

	public boolean live;

	public boolean inTransition;

	public boolean tallying;

	public int balance;

	public int numEditions;
	
	public boolean userlocked;
	
	/*
	 * only called when intializing the db
	 */
	public Periodical(Name name) {
		this.userlocked = false;
		this.root = rootKey();
		this.inTransition = false;
		this.tallying = false;
		this.live = true;
		this.idName = name.name;
		// TODO Remove.
		this.balance = 500000; // 5k dollars (in pennies)
	}
	
	private static Key<Root> rootKey() {
		return new Key<Root>(Root.class, ROOT_ID);
	}

	public Periodical() {}	
	
	public boolean isFinished() { return live == false || currentEditionKey == null; }
	
	public static Key<Periodical> getKey(String idName) {
		return new Key<Periodical>(rootKey(), 
				Periodical.class, DAO.periodicalName.name);
	}

	public Key<Periodical> getKey() {
		return Periodical.getKey(idName);
	}

	public void setcurrentEditionKey(Key<Edition> Key) {
		this.currentEditionKey = Key;
	}

	public static String moneyPrint(int amount) {
		return "$" + amount / 100 + "." + amount % 100;	
	}
	
}
