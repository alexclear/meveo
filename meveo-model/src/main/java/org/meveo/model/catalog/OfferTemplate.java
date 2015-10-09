/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
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
package org.meveo.model.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldInstance;

@Entity
@ObservableEntity
@CustomFieldEntity(accountLevel = AccountLevelEnum.OFFER)
@ExportIdentifier({ "code", "provider" })
@Table(name = "CAT_OFFER_TEMPLATE", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE", "PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "CAT_OFFER_TEMPLATE_SEQ")
public class OfferTemplate extends BusinessCFEntity {
    private static final long serialVersionUID = 1L;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "CAT_OFFER_SERV_TEMPLATES", joinColumns = @JoinColumn(name = "OFFER_TEMPLATE_ID"), inverseJoinColumns = @JoinColumn(name = "SERVICE_TEMPLATE_ID"))
    private List<ServiceTemplate> serviceTemplates;

    @OneToMany(mappedBy = "offerTemplate", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @MapKeyColumn(name = "code")
    private Map<String, CustomFieldInstance> customFields = new HashMap<String, CustomFieldInstance>();

    public List<ServiceTemplate> getServiceTemplates() {
        return serviceTemplates;
    }

    public void setServiceTemplates(List<ServiceTemplate> serviceTemplates) {
        this.serviceTemplates = serviceTemplates;
    }

    public Map<String, CustomFieldInstance> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, CustomFieldInstance> customFields) {
        this.customFields = customFields;
    }

    @Override
    public ICustomFieldEntity getParentCFEntity() {
        return null;
    }
}