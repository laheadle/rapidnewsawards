package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class AllEditions implements Serializable {
	private static final long serialVersionUID = 1L;
	public LinkedList<EditionMessage> editions;
	public EditionMessage current;
	
	public AllEditions(LinkedList<EditionMessage> ll, EditionMessage current) {
		this.editions = ll;
		this.current = current;
	}

}
