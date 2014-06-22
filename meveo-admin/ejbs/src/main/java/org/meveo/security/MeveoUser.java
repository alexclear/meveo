package org.meveo.security;


import org.picketlink.idm.model.basic.User;

public class MeveoUser extends User {

	private static final long serialVersionUID = 4333140556503076034L;

	private org.meveo.model.admin.User user;

	public MeveoUser(org.meveo.model.admin.User user) {
		this.user = user;
	}

	// TODO: @Override
	public String getKey() {
		return getId();
	}

	// TODO: @Override
	public String getId() {
		return user.getUserName();
	}

	public org.meveo.model.admin.User getUser() {
		return this.user;
	}
}
