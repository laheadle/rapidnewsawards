package org.rapidnewsawards.messages;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.rapidnewsawards.core.Link;
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
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, 
				DateFormat.SHORT, Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT-4"));
		this.timeStr = (df.format(time));
	}

	public String getTimeStr() {
		return timeStr;
	};
	
}
