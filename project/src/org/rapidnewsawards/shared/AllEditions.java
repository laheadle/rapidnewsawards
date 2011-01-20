package org.rapidnewsawards.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AllEditions implements IsSerializable {
	public LinkedList<Edition> editions;
	public Edition current;
	
	public AllEditions(LinkedList<Edition> ll, Edition current) {
		this.editions = ll;
		this.current = current;
	}

}
