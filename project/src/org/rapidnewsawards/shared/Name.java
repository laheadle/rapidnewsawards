package org.rapidnewsawards.shared;


public enum Name {

	AGGREGATOR_NAME("Journalism"),
	USERNAME("username"), 
	URL("url"), 
	NAME("name"), 
	NEXT("next");

	public String name;

	Name(String name) {
		this.name = name;
	}

}
