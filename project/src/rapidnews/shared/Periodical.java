package rapidnews.shared;

import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.OKey;

@Entity
public class Periodical {
	private OKey<Edition> currentEditionKey;
	
	private String name;

	@Id
	private Long id;

	@Transient
	private LinkedList<Edition> editions;

	@Transient
	private Edition currentEdition;

	public Periodical(String name) {
		this();
		this.name = name;
	}
	
	public Periodical() {
		editions = new LinkedList<Edition>();
	}
	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OKey<Periodical> getOKey() {
		return new OKey<Periodical>(Periodical.class, id);
	}

	public void setcurrentEditionKey(OKey<Edition> oKey) {
		this.currentEditionKey = oKey;
	}

	public OKey<Edition> getCurrentEditionKey() {
		return this.currentEditionKey;
	}

	public void setEditions(LinkedList<Edition> editions) {
		this.editions = editions;
	}

	public Edition getCurrentEdition() {
		return currentEdition;
	}

	public void setCurrentEdition(Edition edition) {
		this.currentEdition = edition;
	}

	
}
