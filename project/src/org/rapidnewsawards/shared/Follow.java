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
public class Follow {
	@Id
	Long id;

    @Parent
	public Key<User> editor;
	
	public Key<User> judge;
	public Date time;

	public boolean upcoming;

	public Follow() {}
	
	public Follow(Key<User> from, Key<User> to, Date date, boolean upcoming) {
		editor = from;
		judge = to;
		this.time = date;
		this.upcoming = upcoming;
	}

	public Key<Follow> getKey() {
		return new Key<Follow>(this.editor, Follow.class, id);
	}

}
