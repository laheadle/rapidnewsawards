package rapidnews.server;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Link;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Vote {

	@Persistent 
	private Reader reader;
	
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;
    
    @Persistent
	private Link link;

	public Vote(Reader reader, Link link) {
		this.reader = reader;
		this.link = link;
	}

	public Reader getReader() {
		return reader;
	}

	public Link getLink() {
		return link;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public rapidnews.shared.Vote getDTO() {
		return new rapidnews.shared.Vote(getLinkDTO(this.link));
	}

	private static rapidnews.shared.Link getLinkDTO(Link link) {
		return new rapidnews.shared.Link(link.getValue());
	}

}
