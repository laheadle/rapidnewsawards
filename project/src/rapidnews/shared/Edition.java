package rapidnews.shared;

import java.util.*;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Edition implements IsSerializable {

	public Edition() {
		readers = new LinkedList<Reader>();
	}

	public LinkedList<Reader> getReaders() {
		return readers;
	}
	private LinkedList<Reader> readers;
	
	public void addReader(Reader r) {
		readers.add(r);
	}

}
