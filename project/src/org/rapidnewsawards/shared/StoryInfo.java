package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class StoryInfo implements IsSerializable {
	public StoryInfo () {}
	public User submitter;
	public Link link;
	public int score;
	public int revenue;
}
