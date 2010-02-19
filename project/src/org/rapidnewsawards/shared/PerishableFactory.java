package org.rapidnewsawards.shared;

import java.util.Date;

public interface PerishableFactory {
		public Perishable create(Date end);
}
