package org.rapidnewsawards.messages;

import java.util.LinkedList;

public class RecentVotes {
	public RecentVotes() {};
	public EditionMessage edition;
	public int numEditions;
	public boolean isNext;
	public boolean isCurrent;
	public LinkedList<User_Vote_Link> list;
}
