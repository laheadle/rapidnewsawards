package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.User;


/* User Interface Info Package
 * 
 */
public class SocialInfo {
	public User editor;
	public User judge;
	/*
	 * follow (true) or unfollow
	 */
	public boolean on;

	public SocialInfo(User e, User j, boolean on) { 
		this.on = on; 
		this.editor = e; 
		this.judge = j; 
	}
	
	public SocialInfo() {};
}
