package org.rapidnewsawards.shared;

import java.util.LinkedList;

public class AllEditions {
	public LinkedList<Edition> editions;
	public Edition current;
	
	public AllEditions(LinkedList<Edition> ll, Edition current) {
		this.editions = ll;
		this.current = current;
	}

}
