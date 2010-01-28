package rapidnews.shared;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.OKey;

@Entity
public class Vote implements IsSerializable{

	OKey<Reader> readerKey;
	
    @Id
    Long id;
 
	public OKey<Link> linkKey;
	
	@Transient
	Link link; // may be null

	public Vote(Reader reader, Link link) {
		this.readerKey = reader.getOKey();
		this.linkKey = link.getOKey();
		this.link = link;
	}

	public Vote () {} // required by objectify
	
	public OKey<Vote> getOKey() {
		return new OKey<Vote>(Vote.class, id);
	}

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

}
