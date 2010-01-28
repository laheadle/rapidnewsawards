package rapidnews.shared;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.googlecode.objectify.OKey;


@Entity
public class Link implements IsSerializable {
	
	public String url;

	@Id
	Long id;
	 	
	public String getUrl() {
		return url;
	}

	public OKey<Link> getOKey() {
		return new OKey<Link>(Link.class, id);
	}

	public Link(String url) {
		this.url = url;
	}

	public Link() {
		this.url = "";
	}
}
