package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.Date;

import org.rapidnewsawards.core.Periodical;

public class DonationMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	public String webPage;
	public String name;
	public String statement;
	public int amount;
	public Date date;
	public String amountStr;
	
	public DonationMessage(String name, int donation, String webPage, String statement) {
		this.name = name;
		this.webPage = webPage;
		this.statement = statement;
		this.amount = donation;
		this.amountStr = Periodical.moneyPrint(amount);
		this.date = new Date();
	}
}
