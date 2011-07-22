package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;

import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.User;


public class InfluenceMessage implements Comparable<InfluenceMessage>, 
Serializable {
	private static final long serialVersionUID = 1L;
	public User user;
	public String fundedStr;
	public int funded;
	public LinkedList<User> supportingEditors;
	public String timeStr;
	public long timeLong;
	
	public InfluenceMessage (User u, int funded) { 
		this.user = u;
		this.funded = funded;
		this.fundedStr = Periodical.moneyPrint(funded);
		this.supportingEditors = new LinkedList<User>();
	}
	
	public InfluenceMessage() {};

	public int compareTo(InfluenceMessage ua) {
		// descending
		return funded < ua.funded ? 1 : funded == ua.funded ? 0 : -1;
	}

	public void setTime(Date time) {
		this.timeStr = Periodical.timeFormat(time);
		this.timeLong = time.getTime();
	}

}
