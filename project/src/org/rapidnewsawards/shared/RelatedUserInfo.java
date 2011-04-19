package org.rapidnewsawards.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RelatedUserInfo implements IsSerializable {

	public boolean isFollowing;
	public UserInfo userInfo;
	
	public RelatedUserInfo() {}
	
}
