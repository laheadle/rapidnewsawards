package rapidnews.shared;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.OKey;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class Periodical {
	private OKey<Edition> currentEditionKey;
	
	private String name;

	@Id
	private Long id;

	@Transient
	private ArrayList<Edition> editions;

	@Transient
	private Edition currentEdition;

	public static class EditionsIndex {
	    @Id Long id; 
	    @Parent OKey<Periodical> parent; 
	    public ArrayList<OKey<Edition>> editions;

	    public EditionsIndex() {}

		public EditionsIndex(Periodical parent, ArrayList<Edition> editions) {
			ArrayList<OKey<Edition>> eKeys = new ArrayList<OKey<Edition>>();
			for(Edition e : editions) {
				eKeys.add(e.getOKey());
			}
	    	this.parent = parent.getOKey();
	    	this.editions = eKeys;
		}

	}

	public Periodical(String name) {
		this();
		this.name = name;
	}
	
	public Periodical() {
		editions = new ArrayList<Edition>();
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

	public void setEditions(ArrayList<Edition> editions) {
		this.editions = editions;
	}

	public Edition getCurrentEdition() {
		return currentEdition;
	}

	public void setCurrentEdition(Edition edition) {
		this.currentEdition = edition;
	}

	public void verifyState() {
		if (this.currentEdition == null && this.currentEditionKey == null)
			return; // finalized
		if (this.currentEdition != null && this.currentEditionKey != null && 
				this.currentEditionKey.equals(this.currentEdition.getOKey()) &&
				!this.currentEdition.isExpired())
			return; // ongoing
		// inconsistent
		throw new IllegalStateException();
	}

	
}
