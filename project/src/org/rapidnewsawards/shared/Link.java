package org.rapidnewsawards.shared;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;


@Entity
public class Link implements IsSerializable {
	
	public String url;
	public String title;
	public Key<User> submitter;
	
	@Id
	Long id;
	 	
	public Key<Link> getKey() {
		return new Key<Link>(Link.class, id);
	}

	public Link(String url, String title, Key<User> submitter) {
		this.submitter = submitter;
		if (title == null)
			title = "Something Just Happened!";
		this.title = title;
		this.url = url;
	}

	public Link() {}
}
