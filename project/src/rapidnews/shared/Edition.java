package rapidnews.shared;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;

@Entity
public class Edition implements IsSerializable {

	@Transient
	private LinkedList<Reader> readers;
	
	@Id
	Long id;
	
	private Date end;

	private int number;

	public Edition() {
		readers = new LinkedList<Reader>();
	}

	public Edition(Date end, int number) {
		this();
		this.end = end;
		this.number = number;
	}

	public LinkedList<Reader> getReaders() {
		return readers;
	}
	
	@Override
	public boolean equals(Object e) {
		if(!(e instanceof Edition)) {
			return false;
		}
		return id.equals(((Edition) e).id);
	}
	
	public void addReader(Reader r) {
		readers.add(r);
	}

	public Date getEnd() {
		return end;	
	}

	public Key<Edition> getKey() {
		return new Key<Edition>(Edition.class, id);
	}

	public boolean isExpired() {
		return this.end.before(new Date());
	}

	public int getNumber() {
		return number;
	}

}
