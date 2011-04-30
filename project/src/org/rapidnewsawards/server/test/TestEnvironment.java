package org.rapidnewsawards.server.test;

import java.util.HashMap;
import java.util.Map;

import com.google.apphosting.api.ApiProxy.Environment;

public class TestEnvironment implements Environment {

	public String getAppId() {
		return "test";
	}

	public String getVersionId() {
		return "1.0";
	}

	public String getEmail() {
		throw new UnsupportedOperationException();
	}

	public boolean isLoggedIn() {
		throw new UnsupportedOperationException();
	}

	public boolean isAdmin() {
		throw new UnsupportedOperationException();
	}

	public String getAuthDomain() {
		throw new UnsupportedOperationException();
	}

	public String getRequestNamespace() {
		return "";
	}

	public Map<String, Object> getAttributes() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("com.google.appengine.server_url_key", "http://localhost:8080");
		return map;
	}
}
