package org.rapidnewsawards.core;

import java.util.Date;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;

@Cached
public class Edition implements Comparable<Edition> {
	
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

	public static Key<Edition> getKey(String id) {
		return new Key<Edition>(Edition.class, id);		
	}

	public static Key<Edition> getNextKey(String id) {
		String nextId = new Integer(id) + 1 +"";
		return getKey(nextId);		
	}
	public static Key<Edition> getPreviousKey(String id) {
		String prevId = new Integer(id) - 1 +"";
		return getKey(prevId);		
	}

	public static int getNumber(Key<Edition> e) {
		return Integer.valueOf(e.getName());
	}
	
	public static boolean isFinal(Key<Edition> edition, int numEditions) {
		return getNumber(edition)== numEditions - 1;
	}	
	
	public static boolean isFinalOrBad(Key<Edition> e, int numEditions) {
		int number = getNumber(e);
		return isFinal(e, numEditions) || number < 0 || number > numEditions -1;
	}


}
