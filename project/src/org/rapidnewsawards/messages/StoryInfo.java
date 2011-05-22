package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.User;


public class StoryInfo {
	public StoryInfo () {}

	public void setFunding(int funding) {
		this.funding = funding;
		this.fundingStr = Periodical.moneyPrint(funding);
	}
	public int getRevenue() {
		return funding;
	}
	public int editionId;
	public User submitter;
	public Link link;
	public int score;
	private int funding;
	
	// only read by clients
	@SuppressWarnings("unused")
	private String fundingStr;
}
