package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class User_Vote_Link implements IsSerializable {
	public User user;
	public Link link;
	public Vote vote;
	public User_Vote_Link(User u, Vote v, Link l) { this.user = u; this.vote = v; this.link = l; }
	public User_Vote_Link() {};
}
