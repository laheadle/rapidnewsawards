package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.User;


public class User_Authority implements Comparable<User_Authority> {
	public User user;
	public Integer authority;
	public User_Authority (User u, int authority) { this.user = u; this.authority = authority; }
	public User_Authority() {};

	public int compareTo(User_Authority ua) {
		return authority.compareTo(ua.authority);
	}

}
