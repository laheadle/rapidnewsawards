package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.Periodical;
import org.rapidnewsawards.core.User;


public class InfluenceMessage implements Comparable<InfluenceMessage> {
	public User user;
	public int authority;
	public String fundedStr;
	public int funded;

	public InfluenceMessage (User u, int authority, int funded) { 
		this.user = u;
		this.authority = authority;	
		this.funded = funded;
		this.fundedStr = Periodical.moneyPrint(funded);
	}
	
	public InfluenceMessage() {};

	public int compareTo(InfluenceMessage ua) {
		// descending
		return funded < ua.funded ? 1 : funded == ua.funded ? 0 : -1;
	}

}
