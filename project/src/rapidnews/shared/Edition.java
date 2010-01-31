package rapidnews.shared;

import java.util.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.OKey;

@Entity
public class Edition implements IsSerializable {

	@Transient
	private LinkedList<Reader> readers;
	
	@Id
	Long id;
	
	private OKey<Periodical> periodicalKey;
	
	private Date end;

	public Edition() {
		readers = new LinkedList<Reader>();
	}

	public Edition(Periodical m, Date end) {
		this();
		periodicalKey = m.getOKey();
		this.end = end;
	}

	public LinkedList<Reader> getReaders() {
		return readers;
	}
	
	public void addReader(Reader r) {
		readers.add(r);
	}

	public Date getEnd() {
		return end;	
	}

	public OKey<Edition> getOKey() {
		return new OKey<Edition>(Edition.class, id);
	}

}
