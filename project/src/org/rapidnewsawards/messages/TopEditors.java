package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

public class TopEditors implements Serializable {
	private static final long serialVersionUID = 1L;
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public LinkedList<InfluenceMessage> list;

}
