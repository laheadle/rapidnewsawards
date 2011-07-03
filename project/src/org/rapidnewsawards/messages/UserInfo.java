package org.rapidnewsawards.messages;

import java.util.LinkedList;

import org.rapidnewsawards.core.User;

public class UserInfo {
	public User user;
	public LinkedList<Vote_Link> votes;
	public LinkedList<User> followers;
	public LinkedList<User> follows;
	public LinkedList<SocialInfo> socials;
}
