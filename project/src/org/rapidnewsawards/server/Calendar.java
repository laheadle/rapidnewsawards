package org.rapidnewsawards.server;

import java.util.Date;


import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class Calendar implements Perishable {
	
	private final Date end;

	@Inject
	public Calendar(@Assisted Date end) {
		this.end = end;
	}
	
	public boolean isExpired() {
		return end.before(new Date());
	}
}
