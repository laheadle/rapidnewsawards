package rapidnews.shared;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.Key;


@Entity
public class Link implements IsSerializable {
	
	public String url;

	@Id
	Long id;
	 	
	public String getUrl() {
		return url;
	}

	public Key<Link> getKey() {
		return new Key<Link>(Link.class, id);
	}

	public Link(String url) {
		this.url = url;
	}

	public Link() {
	}
}
