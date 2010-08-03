package org.rapidnewsawards.shared;

import java.util.HashMap;

public enum Name {

	JOURNALISM("Journalism"),
	USERNAME("username"), 
	URL("url"), 
	NAME("name"), 
	NEXT("next");

	public String name;

	Name(String name) {
		this.name = name;
	}

}
