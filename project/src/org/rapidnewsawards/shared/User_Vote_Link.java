package org.rapidnewsawards.shared;


public class User_Vote_Link {
	public User user;
	public Link link;
	public Vote vote;
	public User_Vote_Link(User u, Vote v, Link l) { this.user = u; this.vote = v; this.link = l; }
	public User_Vote_Link() {};
}
