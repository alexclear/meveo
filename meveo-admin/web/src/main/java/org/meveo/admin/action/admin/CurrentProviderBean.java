/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.admin;

import java.io.Serializable;

import javax.ejb.EJB;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.security.Identity;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.security.MeveoUser;
import org.meveo.service.admin.impl.UserService;
import org.meveo.util.PersistenceUtils;

/**
 * Class used to set current system provider and user
 */
@Named
public class CurrentProviderBean implements Serializable {

    private static final long serialVersionUID = 224598087195379100L;

    @Inject
    private Identity identity;
    
    @Inject
    @EJB
    private UserService userService; 

    /**
     * Sets current provider
     */
    public String setCurrentProvider(Provider provider) {
        if (identity.isLoggedIn()) {
            ((MeveoUser) identity.getUser()).setCurrentProvider(provider);
        }

        return "/home.xhtml?faces-redirect=true";
    }

    @Produces
    @Named("currentProvider")
    @CurrentProvider
    public Provider getCurrentProvider() {
        if (identity != null && identity.isLoggedIn()) {
            return ((MeveoUser) identity.getUser()).getCurrentProvider();
        }
        return null;
    }

    @Produces
    @Named("currentUser")
    @CurrentUser
    public User getCurrentUser() {
        if (identity != null && identity.isLoggedIn()) {
            return ((MeveoUser) identity.getUser()).getUser();
        }
        return null;
    }

    /**
     * Refresh logged in user with a new information
     */
    public void refreshCurrentUser() {
        if (identity != null && identity.isLoggedIn()) {

            User reloadedUser = userService.findById(((MeveoUser) identity.getUser()).getUser().getId());
            PersistenceUtils.initializeAndUnproxy(reloadedUser.getRoles());

            ((MeveoUser) identity.getUser()).refreshUser(reloadedUser);
        }
    }
}