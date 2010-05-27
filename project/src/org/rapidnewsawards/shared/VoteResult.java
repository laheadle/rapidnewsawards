package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;


public class VoteResult implements IsSerializable {
	public VoteResult() {};
	public Return returnVal;
	public String authUrl;
	public Boolean supporting;
}
