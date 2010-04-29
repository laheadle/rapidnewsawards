package org.rapidnewsawards.shared;

public enum Return {

	ALREADY_FOLLOWING("Already Following"),
	SUCCESS("Success"),
	NOT_FOLLOWING("Not Following"), 
	ALREADY_ABOUT_TO_FOLLOW("Already About to Follow"), 
	PENDING_UNFOLLOW_CANCELLED("Pending UnFollow Cancelled"),
	ABOUT_TO_FOLLOW("About to Follow"), 
	ABOUT_TO_UNFOLLOW("About to Unfollow "), 
	PENDING_FOLLOW_CANCELLED("Pending Follow Cancelled");


public String s;

	Return(String s) {
		this.s = s;
	}

}
