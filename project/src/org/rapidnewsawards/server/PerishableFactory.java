package org.rapidnewsawards.server;

import java.util.Date;


public interface PerishableFactory {
		public Perishable create(Date end);
}
