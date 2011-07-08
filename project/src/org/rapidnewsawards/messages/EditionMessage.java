package org.rapidnewsawards.messages;

import java.util.Date;

import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.ScoreSpace;

public class EditionMessage {
	public final int id;
	public final Date end;
	public final int revenue;
	public final int number;
	public final int totalSpend;
	public final int numFundedLinks;
	public final int totalScore;
	public final boolean finished;
	public final String totalSpendStr;
	public final String revenueStr;
	public String displayStr;
	

	public EditionMessage(Edition e, ScoreSpace s) {
		this.id = e.getNumber();
		this.end = e.end;
		this.number =e.number;
		this.totalSpend = s.totalSpend;
		this.totalScore = s.totalScore;
		this.numFundedLinks = s.numFundedLinks;
		this.revenue = s.balance;
		this.revenueStr = Periodical.moneyPrint(this.revenue);
		this.totalSpendStr = Periodical.moneyPrint(this.totalSpend);
		this.finished = s.finished;
		
		if (this.number == 0) {
			this.displayStr = "Signup Round";
		}
		else {
			this.displayStr = String.format("Edition Number %d", this.number);
		}
	}
}
