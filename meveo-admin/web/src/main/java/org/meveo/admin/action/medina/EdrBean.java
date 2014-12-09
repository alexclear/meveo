package org.meveo.admin.action.medina;

import java.util.Map;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.rating.EDR;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.EdrService;

@Named
@ConversationScoped
public class EdrBean extends BaseBean<EDR> {

	private static final long serialVersionUID = 7833532801870480214L;

	@Inject
	private EdrService edrService;

	public EdrBean() {
		super(EDR.class);
	}

	@Override
	public String getEditViewName() {
		return "edrDetail";
	}

	@Override
	public Map<String, Object> getFilters() {
		filters = super.getFilters();

		filters.put("status", EDRStatusEnum.REJECTED);

		return filters;
	}

	public void updateStatus(EDR selectedEdr) {
		selectedEdr.setStatus(EDRStatusEnum.OPEN);

		getPersistenceService().update(selectedEdr);
	}

	@Override
	protected IPersistenceService<EDR> getPersistenceService() {
		return edrService;
	}

}
