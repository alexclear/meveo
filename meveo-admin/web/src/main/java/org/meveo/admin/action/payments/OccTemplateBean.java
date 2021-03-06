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
package org.meveo.admin.action.payments;

import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.payments.OCCTemplate;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.omnifaces.cdi.ViewScoped;

/**
 * Standard backing bean for {@link OCCTemplate} (extends {@link BaseBean} that
 * provides almost all common methods to handle entities filtering/sorting in
 * datatable, their create, edit, view, delete operations). It works with Manaty
 * custom JSF components.
 */
@Named
@ViewScoped
public class OccTemplateBean extends BaseBean<OCCTemplate> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected @{link OCCTemplate} service. Extends {@link PersistenceService}.
	 */
	@Inject
	private OCCTemplateService occTemplateService;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public OccTemplateBean() {
		super(OCCTemplate.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@Produces
	@Named("occTemplate")
	public OCCTemplate init() {
		return initEntity();
	}
	
	@Override
	public String getNewViewName() {
		return "occTemplateDetail";
	}

	@Override
	protected String getListViewName() {
		return "occTemplates";
	}

	@Override
	public String getEditViewName() {
		return "occTemplateDetail";
	}

	public List<OCCTemplate> listOCCTemplate() {
		return (List<OCCTemplate>) occTemplateService
				.getListOccSortedByName(getCurrentProvider().getCode());
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<OCCTemplate> getPersistenceService() {
		return occTemplateService;
	}

}
