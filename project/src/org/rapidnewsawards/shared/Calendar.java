package org.rapidnewsawards.shared;

import java.util.Date;

public class Calendar implements Perishable {
	
	private Date end;

	public Calendar(Date end) {
		this.end = end;
	}
	
	public boolean isExpired() {
		return end.before(new Date());
	}
}
