package org.rapidnewsawards.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TopStories implements IsSerializable {
	public int numEditions;
	public Edition edition;
	public LinkedList<StoryInfo> list;
}
