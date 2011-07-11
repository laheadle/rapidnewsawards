package org.rapidnewsawards.messages;

import java.util.Date;

import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.Vote;


public class Vote_Link {
	public Vote vote;
	public Link link;
	private String timeStr;
	
	public Vote_Link() {};
	public Vote_Link (Vote v, Link l) {
		vote = v;
		link = l;
		this.setTime(v.time);
	}

	public void setTime(Date time) {
		this.timeStr = Periodical.timeFormat(time);
	}

	public String getTimeStr() {
		return timeStr;
	};
	
}
