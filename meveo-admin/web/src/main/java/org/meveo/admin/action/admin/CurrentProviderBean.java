/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.admin;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.security.Identity;
import org.meveo.model.crm.Provider;
import org.meveo.security.MeveoUser;

/**
 * Class used to set current system provider
 */
@Named
@SessionScoped
public class CurrentProviderBean implements Serializable {

	private static final long serialVersionUID = 2L;

	private Provider currentProvider;

	@Inject
	private Identity identity;

	/**
	 * Sets current provider
	 */
	public String setCurrentProvider(Provider provider) {
		currentProvider = provider;

		return "/home.xhtml?faces-redirect=true";
	}

	@Produces
	@Named("currentProvider")
	@CurrentProvider
	public Provider getCurrentProvider() {
		if (currentProvider == null && identity.isLoggedIn()) {
			if (((MeveoUser) identity.getUser()).getUser().isOnlyOneProvider()) {
				currentProvider = ((MeveoUser) identity.getUser()).getUser().getProvider();
			} else {
				currentProvider = ((MeveoUser) identity.getUser()).getUser().getProviders()
						.iterator().next();
			}
			currentProvider.getLanguage().getLanguageCode(); // Lazy loading
																// issue
		}

		return currentProvider;
	}
}
