package org.rapidnewsawards.core;

import java.io.Serializable;

import javax.persistence.Id;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;


@Cached
public class Link implements Serializable {
	private static final long serialVersionUID = 1L;

	@Parent public Key<Root> parent;
	
	@Id public Long id;
	public String url;
	@Unindexed public String title;
	@Unindexed public Key<User> submitter;
	@Unindexed public String domain;
		 	
	public Key<Link> getKey() {
		return createKey(id);
	}

	public Link(String url, String title, String domain, Key<User> submitter) {
		this.parent = rootKey();
		this.submitter = submitter;
		this.title = title;
		this.url = url;
		this.domain = domain;
	}
		
	public Link() {}

	public static Key<Link> createKey(Long linkId) {
		return new Key<Link>(rootKey(), Link.class, linkId);	
	}

	public static Key<Root> rootKey() {
		return new Key<Root>(Root.class, Periodical.LINK_ROOT_ID);
	}
}
