package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class TopStories implements Serializable {
	private static final long serialVersionUID = 1L;
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public boolean isStoryList = true;
	public boolean isNetworkList = false;	
	public String order = "top";
	public LinkedList<StoryInfo> list;
}
