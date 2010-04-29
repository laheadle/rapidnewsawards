package org.rapidnewsawards.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class UserInfo implements IsSerializable {
	public User user;
	public LinkedList<Vote_Link> votes;
}
