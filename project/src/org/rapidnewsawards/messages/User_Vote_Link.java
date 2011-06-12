package org.rapidnewsawards.messages;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;


public class User_Vote_Link {
	public User user;
	public Link link;
	public Vote vote;
	private Date time;
	private String timeStr;
	
	public User_Vote_Link(User u, Vote v, Link l) { 
		this.user = u;
		this.vote = v;
		this.link = l;
		this.setTime(v.time);
	}

	public void setTime(Date time) {
		this.time = time;
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, 
				DateFormat.SHORT, Locale.US);
		df.setTimeZone(TimeZone.getTimeZone("GMT-4"));
		this.timeStr = (df.format(time));
	}

	public Date getTime() {
		return time;
	}

	public String getTimeStr() {
		return timeStr;
	};

	public User_Vote_Link() {};
}
