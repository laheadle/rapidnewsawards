package org.rapidnewsawards.messages;

import java.io.Serializable;

public class RelatedUserInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	public boolean isFollowing;
	public UserInfo userInfo;
	public RelatedUserInfo() {}
}
