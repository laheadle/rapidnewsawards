package rapidnews.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Link implements IsSerializable {
	private String url;

	public String getUrl() {
		return url;
	}

	public Link(String url) {
		super();
		this.url = url;
	}

	public Link() {
		this.url = "";
	}
}
