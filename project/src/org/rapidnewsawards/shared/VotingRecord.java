package org.rapidnewsawards.shared;

import java.util.Date;
import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class VotingRecord extends RecentVotes implements IsSerializable {
	public Date joinedOn;
	public User user;
	public LinkedList<User_Link> votes;
	public int numEditions;
	public Edition edition;
}
