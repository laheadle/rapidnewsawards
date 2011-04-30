package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.core.Vote;


public class User_Vote_Link {
	public User user;
	public Link link;
	public Vote vote;
	public User_Vote_Link(User u, Vote v, Link l) { this.user = u; this.vote = v; this.link = l; }
	public User_Vote_Link() {};
}
