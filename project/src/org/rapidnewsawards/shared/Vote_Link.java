package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Vote_Link implements IsSerializable {
	public Vote vote;
	public Link link;
	public Vote_Link() {};
	public Vote_Link (Vote v, Link l) { vote = v; link = l; }
}
