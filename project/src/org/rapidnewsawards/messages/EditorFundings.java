package org.rapidnewsawards.messages;

import java.io.Serializable;
import java.util.LinkedList;

import org.rapidnewsawards.core.User;

public class EditorFundings implements Serializable {
	private static final long serialVersionUID = 1L;
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public LinkedList<User_Vote_Link> list;
	public User editor;
}
