package org.rapidnewsawards.messages;

import java.util.LinkedList;

public class TopStories {
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public boolean isStoryList = true;
	public boolean isNetworkList = false;	
	public String order = "top";
	public LinkedList<StoryInfo> list;
}
