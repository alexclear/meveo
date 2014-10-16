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
package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.catalog.DayInYear;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.catalog.impl.DayInYearService;
import org.primefaces.model.DualListModel;

/**
 * Standard backing bean for {@link Calendar} (extends {@link BaseBean} that
 * provides almost all common methods to handle entities filtering/sorting in
 * datatable, their create, edit, view, delete operations). It works with Manaty
 * custom JSF components.
 * 
 * @author Ignas
 * @created 2009.10.13
 */
@Named
@ConversationScoped
public class CalendarBean extends BaseBean<Calendar> {

	private static final long serialVersionUID = 1L;

	/** Injected @{link Calendar} service. Extends {@link PersistenceService}. */
	@Inject
	private CalendarService calendarService;

	@Inject
	private DayInYearService dayInYearService;

	private DualListModel<DayInYear> perks;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public CalendarBean() {
		super(Calendar.class);
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<Calendar> getPersistenceService() {
		return calendarService;
	}

	/**
	 * Standard method for custom component with listType="pickList".
	 */
	public DualListModel<DayInYear> getDualListModel() {
		if (perks == null) {
			List<DayInYear> perksSource = dayInYearService.list();
			List<DayInYear> perksTarget = new ArrayList<DayInYear>();
			if (getEntity().getDays() != null) {
				perksTarget.addAll(getEntity().getDays());
			}
			perksSource.removeAll(perksTarget);
			perks = new DualListModel<DayInYear>(perksSource, perksTarget);
		}
		return perks;
	}

	public void setDualListModel(DualListModel<DayInYear> perks) {
		getEntity().setDays((List<DayInYear>) perks.getTarget());
	}

	@Override
	protected String getDefaultSort() {
		return "name";
	}
}