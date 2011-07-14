package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

import org.rapidnewsawards.core.User;

public class UserInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	public User user;
	public LinkedList<Vote_Link> votes;
	public LinkedList<User> followers;
	public LinkedList<User> follows;
	public LinkedList<SocialInfo> socials;
}
