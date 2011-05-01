package org.rapidnewsawards.core;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;


@Cached
public class Link {
	
	public String url;
	public String title;
	public Key<User> submitter;
	public String domain;
	
	@Id
	Long id;

		 	
	public Key<Link> getKey() {
		return new Key<Link>(Link.class, id);
	}

	public Link(String url, String title, String domain, Key<User> submitter) {
		this.submitter = submitter;
		this.title = title;
		this.url = url;
		this.domain = domain;
	}
		
	public Link() {}
}
