package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Editor_Judge implements IsSerializable {
	public User editor;
	public User judge;
	public Editor_Judge(User e, User j) { this.editor = e; this.judge = j; }
	public Editor_Judge() {};
}
