package org.meveo.security;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Model;
import javax.inject.Inject;

import org.meveo.model.security.Role;
import org.picketlink.Identity;

@Model
public class MeveoPermissionResolver implements Serializable{//, PermissionResolver {

	private static final long serialVersionUID = -7908760356168494113L;

	@Inject
	private Identity identity;

	private Map<String, Boolean> cachedPermissions = new HashMap<String, Boolean>();

	/*
	 * Check if user has given permission to a resource. Cache response
	 * 
	 * @see
	 * org.jboss.seam.security.permission.PermissionResolver#hasPermission(java
	 * .lang.Object, java.lang.String)
	 */
	public boolean hasPermission(Object resource, String permission) {
		if (!identity.isLoggedIn()) {
			return false;
		}

		String cacheKey = resource + "_" + permission;
		if (!cachedPermissions.containsKey(cacheKey)) {

			boolean has = false;
			if (((MeveoUser) identity.getAccount()) != null
					&& ((MeveoUser) identity.getAccount()).getUser() != null
					&& ((MeveoUser) identity.getAccount()).getUser().getRoles() != null) {
				for (Role role : ((MeveoUser) identity.getAccount()).getUser().getRoles()) {
					if (role.hasPermission(resource.toString(), permission)) {
						has = true;
						break;
					}
				}
			}
			cachedPermissions.put(cacheKey, has);
		}

		return cachedPermissions.get(cacheKey);
	}

	public void filterSetByAction(Set<Object> resources, String permission) {
	}
}
