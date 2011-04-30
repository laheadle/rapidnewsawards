package org.rapidnewsawards.messages;

import java.util.LinkedList;

import org.rapidnewsawards.core.Edition;

public class AllEditions {
	public LinkedList<Edition> editions;
	public Edition current;
	
	public AllEditions(LinkedList<Edition> ll, Edition current) {
		this.editions = ll;
		this.current = current;
	}

}
