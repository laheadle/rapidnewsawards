package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class FullStoryInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	public StoryInfo info;
	public LinkedList<InfluenceMessage> funds;
}
