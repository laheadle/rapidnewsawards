package org.rapidnewsawards.shared;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;

@Entity
public class Edition implements IsSerializable, Comparable<Edition> {
	
	@Id
	Long id;
	
	public Date end;

	public int number;
		
	public Edition() {}

	public Edition(Date end, int number) {
		this.end = end;
		this.number = number;
	}
		
	@Override
	public boolean equals(Object e) {
		if(!(e instanceof Edition)) {
			return false;
		}
		return id.equals(((Edition) e).id);
	}
	
	public int compareTo(Edition e) {
		return new Integer(number).compareTo(new Integer(e.number));
	}
	
	@Override
	public String toString() {
		return "Edition #" + number + "(" + end.toString() + ")";
	}

	public Key<Edition> getKey() {
		return new Key<Edition>(Edition.class, id);
	}

}
