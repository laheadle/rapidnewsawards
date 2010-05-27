package org.rapidnewsawards.shared;

public enum Return {

	ALREADY_FOLLOWING("Already Following"),
	SUCCESS("Success"),
	NOT_FOLLOWING("Not Following"), 
	ALREADY_ABOUT_TO_FOLLOW("Already About to Follow"), 
	PENDING_UNFOLLOW_CANCELLED("Pending UnFollow Cancelled"),
	ABOUT_TO_FOLLOW("Your follow will take effect in the next edition"), 
	ABOUT_TO_UNFOLLOW("Your unfollow will take effect in the next edition"), 
	PENDING_FOLLOW_CANCELLED("Pending Follow Cancelled"), 
	NO_LONGER_CURRENT("Sorry, that edition is no longer current"), 
	FAILED("The operation failed.  Please try again"), 
	TRANSITION_IN_PROGRESS("A transition between editions is in progress.  Please try again"), 
	ILLEGAL_OPERATION("Illegal Operation"), 
	ALREADY_VOTED("You have already voted for this"),
	NOT_LOGGED_IN("You must log in first"), 
	HAS_NOT_VOTED("You cannot cancel a vote unless you have voted");


public String s;

	Return(String s) {
		this.s = s;
	}

}
