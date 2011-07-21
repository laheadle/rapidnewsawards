package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class Donations implements Serializable {
	private static final long serialVersionUID = 1L;
	public String totalStr;
	public LinkedList<DonationMessage> list;
}
