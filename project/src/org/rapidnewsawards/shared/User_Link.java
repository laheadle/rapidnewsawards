package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class User_Link implements IsSerializable {
	public User user;
	public Link link;
	public User_Link(User u, Link l) { this.user = u; this.link = l; }
	public User_Link() {};
}
