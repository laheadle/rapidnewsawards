package org.rapidnewsawards.shared;

import java.util.Date;
import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Judge_Time {
	@Id
	Long id;

    @Parent
	public Key<User> parent;
	
	public Key<User> judge;
	public Date time;

	public boolean upcoming;

	public Judge_Time() {}
	
	public Judge_Time(Key<User> from, Key<User> to, Date date, boolean upcoming) {
		parent = from;
		judge = to;
		this.time = date;
		this.upcoming = upcoming;
	}

	public Key<Judge_Time> getKey() {
		return new Key<Judge_Time>(this.parent, Judge_Time.class, id);
	}

}
