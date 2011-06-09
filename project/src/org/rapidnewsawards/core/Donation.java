package org.rapidnewsawards.core;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Donation {

	@Id Long id;
	public String webPage;
	public String name;
	public String statement;
	public int amount;
	public Date date;

	public Donation(String name, int donation, String webPage, String statement) {
		this.name = name;
		this.webPage = webPage;
		this.statement = statement;
		this.amount = donation;
		this.date = new Date();
	}

	public Donation() {}
}
