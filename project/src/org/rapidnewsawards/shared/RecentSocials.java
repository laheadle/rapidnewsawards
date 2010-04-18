package org.rapidnewsawards.shared;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RecentSocials implements IsSerializable {
	public int numEditions;
	public Edition edition;
	public LinkedList<Editor_Judge> socials;
}
