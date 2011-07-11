package org.rapidnewsawards.core;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Entity;

@Entity
@Cached
public class Edition implements Serializable, Comparable<Edition> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id	
	private String id;
	
	public Date end;

	public int number;
	
	public Key<Periodical> periodical;

	public Edition() {}

	public Edition(Date end, int number, Key<Periodical> periodical) {
		this.id = Integer.toString(number);
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

	public static Key<Edition> getFinalKey(int numEditions) {
		return createKey(numEditions - 1);
	}

	// TODO 2.0 Move these into DAO
	private static Key<Edition> getNearKey(String id, int diff) {
		try {
			int next = Integer.valueOf(id) + diff;
			if (next < 0) {
				throw new IllegalArgumentException("Illegal edition key");
			}
			return createKey(next);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Illegal edition key");			
		}
	}

	private static Key<Edition> getNextKey(String id) {
		return getNearKey(id, 1);
	}

	private static Key<Edition> getPreviousKey(String id) {
		return getNearKey(id, -1);
	}

	public static Key<Edition> getPreviousKey(Key<Edition> e) {
		return getPreviousKey(e.getName());		
	}

	public static Key<Edition> getNextKey(Key<Edition> e) {
		return getNextKey(e.getName());
	}

	public static int getNumber(Key<Edition> e) {
		return Integer.valueOf(e.getName());
	}
	
	public static boolean isFinal(Key<Edition> edition, int numEditions) {
		return getNumber(edition)== numEditions - 1;
	}	
		
	public static boolean isBad(Key<Edition> e, int numEditions) {
		int number = getNumber(e);
		return number < 0 || number > numEditions -1;
	}

	public static Key<Edition> createKey(int i) {
		return new Key<Edition>(Edition.class, Integer.toString(i));
	}

	public static boolean isAfterFinal(Key<Edition> edition, int numEditions) {
		return getNumber(edition)> numEditions - 1;

	}

	public int getNumber() {
		return Edition.getNumber(this.getKey());
	}
}
