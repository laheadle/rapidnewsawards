package org.rapidnewsawards.shared;

import java.util.Date;

import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Vote implements IsSerializable {
	@Parent
	public
	Key<User> voter;

	public Key<Link> link;
	
	public Date time;
	
	@Id Long id;
	
	public Vote() {}
	
	public Vote(Key<User> voter, Key<Link> link, Date time) {
		this.voter = voter;
		this.link = link;
		this.time = time;
	}
}
