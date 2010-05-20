package org.rapidnewsawards.shared;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;

@Entity
public class Edition implements IsSerializable, Comparable<Edition> {
	
	@Id	
	public String id;
	
	public Date end;

	public int number;
	
	public Key<Periodical> periodical;
	
	public Edition() {}

	public Edition(Date end, int number, Key<Periodical> periodical) {
		this.id = ""+number;
		this.periodical = periodical;
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

	public Key<Edition> getPreviousKey() {
		return getPreviousKey(id);
	}

	public Key<Edition> getNextKey() {
		return getNextKey(id);
	}

	public static Key<Edition> getNextKey(String id) {
		String nextId = new Integer(id) + 1 +"";
		return new Key<Edition>(Edition.class, nextId);		
	}
	public static Key<Edition> getPreviousKey(String id) {
		String nextId = new Integer(id) - 1 +"";
		return new Key<Edition>(Edition.class, nextId);		
	}
	
}
