package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;


@Cached
public class Link {

	@Parent public Key<Root> parent;
	
	@Id public Long id;
	public String url;
	public String title;
	public Key<User> submitter;
	public String domain;
		 	
	public Key<Link> getKey() {
		return createKey(id);
	}

	public Link(String url, String title, String domain, Key<User> submitter) {
		this.parent = Periodical.rootKey();
		this.submitter = submitter;
		this.title = title;
		this.url = url;
		this.domain = domain;
	}
		
	public Link() {}

	public static Key<Link> createKey(Long linkId) {
		return new Key<Link>(Periodical.rootKey(), Link.class, linkId);	
	}
}
