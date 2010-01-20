package rapidnews.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;


public class Reader implements IsSerializable {
	private String name;	
	private LinkedList<Vote> votes; // chronological order
	private String username;

	public Reader(String name, String username) {
		this.name = name;
		this.votes = new LinkedList<Vote>();
		this.username = username;
	}

	public Reader() {
		this.name = "";
		this.votes = new LinkedList<Vote>();
		this.username = "";
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void vote(Vote v) {
		votes.add(v); // xxx twice?		
	}

	public LinkedList<Vote> getVotes() {
		return votes;
	}
	
}
