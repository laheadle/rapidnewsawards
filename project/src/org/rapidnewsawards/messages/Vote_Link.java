package org.rapidnewsawards.messages;

import org.rapidnewsawards.core.Link;
import org.rapidnewsawards.core.Vote;


public class Vote_Link {
	public Vote vote;
	public Link link;
	public Vote_Link() {};
	public Vote_Link (Vote v, Link l) { vote = v; link = l; }
}
