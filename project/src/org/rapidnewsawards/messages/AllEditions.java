package org.rapidnewsawards.messages;

import java.util.LinkedList;

public class AllEditions {
	public LinkedList<EditionMessage> editions;
	public EditionMessage current;
	
	public AllEditions(LinkedList<EditionMessage> ll, EditionMessage current) {
		this.editions = ll;
		this.current = current;
	}

}
