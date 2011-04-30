package org.rapidnewsawards.shared;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.googlecode.objectify.Key;

@Entity
public class Donation {

	@Id Long id;

	public Donation(Key<User> user, int donation) {
		this.user = user;
		this.amount = donation;
		this.date = new Date();
	}

	public Donation() {}
	
	public Key<User> user;
	public int amount;
	public Date date;
}
