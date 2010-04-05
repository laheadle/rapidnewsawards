package org.rapidnewsawards.shared;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;

@Entity
public class Edition implements IsSerializable, Comparable<Edition> {

	@Transient
	private LinkedList<User> users;
	
	@Id
	Long id;
	
	public Date end;

	public int number;
	
	/*
	 * The special editor who follows new Users without empowering them.  These follows are called Joins.
	 */
	public Key<User> rnaEditor;
	
	public Edition() {}

	public Edition(Date end, int number) {
		this.end = end;
		this.number = number;
	}
	
	public LinkedList<User> getUsers() {
		return users;
	}
	
	public void setUsers(LinkedList<User> users) {
		this.users = users;
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

	public void addUser(User u) {
		users.add(u);
	}

	public Date getEnd() {
		return end;	
	}

	public Key<Edition> getKey() {
		return new Key<Edition>(Edition.class, id);
	}

	public int getNumber() {
		return number;
	}

	public void setRNAEditor(User u) {
		rnaEditor = u.getKey();
	}

}
