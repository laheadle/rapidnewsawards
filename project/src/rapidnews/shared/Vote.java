package rapidnews.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Vote implements IsSerializable {

	public Vote(Link l) {
		this.link = l;
	}
	
	public Vote() {
		link = null;
	}

	public Link getLink() {
		return link;
	}

	private Link link;
}
