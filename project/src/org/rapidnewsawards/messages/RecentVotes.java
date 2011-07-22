package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class RecentVotes implements Serializable {
	private static final long serialVersionUID = 1L;
	public RecentVotes() {};
	public EditionMessage edition;
	public int numEditions;
	public boolean isNext;
	public boolean isCurrent;
	public boolean isStoryList = true;
	public boolean isNetworkList = false;	
	public String order = "recent";
	public LinkedList<User_Vote_Link> list;
}
