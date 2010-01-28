package rapidnews.shared;
import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.OKey;

@Entity
public class Reader implements IsSerializable {

	public
	String name;
		
	@Id 	
	public Long id;

	public String username;

	@Transient
	public LinkedList<Vote> votes;

	public Reader() {} // required by objectify
	
	public Reader(String name, String username, LinkedList<Vote> votes) {
		this.name = name;
		this.username = username;
		this.votes = votes;
	}

	public Reader(String name, String username) {
		this(name, username, new LinkedList<Vote>());
	}
	

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public OKey<Reader> getOKey() {
		return new OKey<Reader>(Reader.class, id);
	}

	public LinkedList<Vote> getVotes() {
		return votes;
	}

	public void setVotes(LinkedList<Vote> votes) {
		this.votes = votes; 
	}

}
