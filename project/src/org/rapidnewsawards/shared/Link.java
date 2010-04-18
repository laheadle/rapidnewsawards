package org.rapidnewsawards.shared;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;


@Entity
public class Link implements IsSerializable {
	
	public String url;

	@Id
	Long id;
	 	
	public Key<Link> getKey() {
		return new Key<Link>(Link.class, id);
	}

	public Link(String url) {
		this.url = url;
	}

	public Link() {}
}
