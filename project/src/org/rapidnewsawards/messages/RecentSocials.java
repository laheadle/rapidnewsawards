package org.rapidnewsawards.messages;

import java.util.LinkedList;

public class RecentSocials {
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public boolean isStoryList = false;
	public boolean isNetworkList = true;
	public String order = "recent";
	public LinkedList<SocialInfo> list;
}
