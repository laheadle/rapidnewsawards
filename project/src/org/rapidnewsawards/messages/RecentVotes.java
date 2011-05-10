package org.rapidnewsawards.messages;

import java.util.LinkedList;

public class RecentVotes {
	public RecentVotes() {};
	public EditionMessage edition;
	public int numEditions;
	public LinkedList<User_Vote_Link> list;
}
