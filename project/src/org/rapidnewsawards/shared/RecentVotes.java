package org.rapidnewsawards.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RecentVotes implements IsSerializable {
	public RecentVotes() {};
	public Edition edition;
	public int numEditions;
	public LinkedList<User_Link> votes;
}
