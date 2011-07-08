package org.rapidnewsawards.messages;

import java.util.LinkedList;

import org.rapidnewsawards.core.User;

public class EditorFundings {
	public int numEditions;
	public EditionMessage edition;
	public boolean isNext;
	public boolean isCurrent;
	public LinkedList<User_Vote_Link> list;
	public User editor;
}
