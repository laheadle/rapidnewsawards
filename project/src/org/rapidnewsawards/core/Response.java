package org.rapidnewsawards.core;

public enum Response {

	ALREADY_FOLLOWING("Already Following"),
	SUCCESS("Success"),
	NOT_FOLLOWING("Not Following"), 
	ALREADY_ABOUT_TO_FOLLOW("Already About to Follow"), 
	PENDING_UNFOLLOW_CANCELLED("Pending UnFollow Cancelled"),
	ABOUT_TO_FOLLOW("Your follow will take effect when the next edition is published"), 
	ABOUT_TO_UNFOLLOW("Your unfollow will take effect when the next edition is published"), 
	PENDING_FOLLOW_CANCELLED("Pending Follow Cancelled"), 
	EDITION_NOT_CURRENT("Sorry, that edition is not next"), 
	FAILED("The operation failed.  Please try again"), 
	PUBLISHING_NEXT_EDITION_PLEASE_WAIT("An edition is being published.  Please try again"), 
	ILLEGAL_OPERATION("Illegal Operation"), 
	ALREADY_VOTED("You have already voted for this"),
	NOT_LOGGED_IN("You must log in first"), 
	HAS_NOT_VOTED("You cannot cancel a vote unless you have voted"), 
	FORBIDDEN_DURING_FINAL("You can't do that during the final edition"), 
	NOT_AN_EDITOR("Only Editors can do this"), 
	BAD_URL("Malformed URL.  Does it start with http://?"), 
	THE_EXPERIMENT_IS_FINISHED("The final edition has been published"), 
	YOU_ARE_ALREADY_ABOUT_TO_UNFOLLOW_THIS_JUDGE("Already about to unfollow"), 
	URL_TOO_LONG("The URL is too long"), 
	TITLE_TOO_LONG("The title is too long"), 
	VOTING_IS_FORBIDDEN_DURING_SIGNUP("Voting is not permitted during the signup round"), 
	ONLY_JUDGES_CAN_VOTE("Only judges can vote"), 
	YOUR_USER_ACCOUNT_IS_NOT_INITIALIZED_YET("You have not finished creating your account");
	
	public String s;

	Response(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
