package org.rapidnewsawards.core;

public enum Response {

	ALREADY_FOLLOWING("Already Following"),
	SUCCESS("Success"),
	NOT_FOLLOWING("Not Following"), 
	ALREADY_ABOUT_TO_FOLLOW("Already About to Follow"), 
	PENDING_UNFOLLOW_CANCELLED("Pending UnFollow Cancelled"),
	ABOUT_TO_FOLLOW("Your follow will take effect in the next edition"), 
	ABOUT_TO_UNFOLLOW("Your unfollow will take effect in the next edition"), 
	PENDING_FOLLOW_CANCELLED("Pending Follow Cancelled"), 
	EDITION_NOT_CURRENT("Sorry, that edition is not the upcoming one"), 
	FAILED("The operation failed.  Please try again"), 
	TRANSITION_IN_PROGRESS("A transition between editions is in progress.  Please try again"), 
	ILLEGAL_OPERATION("Illegal Operation"), 
	ALREADY_VOTED("You have already voted for this"),
	NOT_LOGGED_IN("You must log in first"), 
	HAS_NOT_VOTED("You cannot cancel a vote unless you have voted"), 
	FORBIDDEN_DURING_FINAL("You can't do that during the final edition"), 
	NOT_AN_EDITOR("Only Editors can do this"), 
	BAD_URL("Malformed URL"), IS_FINISHED("The last edition is finished"), 
	ALREADY_ABOUT_TO_UNFOLLOW("Already about to unfollow");


public String s;

	Response(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
