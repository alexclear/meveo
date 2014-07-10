/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.admin.action.billing;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.utils.ListItemsSelector;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.billing.BillingProcessTypesEnum;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.PostInvoicingReportsDTO;
import org.meveo.model.billing.PreInvoicingReportsDTO;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.BillingRunService;
import org.omnifaces.util.Messages;

@Named
@ConversationScoped
public class BillingRunBean extends BaseBean<BillingRun> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected
	 * 
	 * @{link Invoice} service. Extends {@link PersistenceService}.
	 */
	@Inject
	private BillingRunService billingRunService;

	@Inject
	@RequestParam
	private Instance<Boolean> preReport;

	@Inject
	@RequestParam
	private Instance<Boolean> postReport;

	private ListItemsSelector<Invoice> itemSelector;


	private DataModel<Invoice> invoicesModel;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public BillingRunBean() {
		super(BillingRun.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public BillingRun initEntity() {
		BillingRun billingRun = super.initEntity();
		try {
			log.info("postReport.get()=" + postReport.get());
			if (billingRun.getId() == null) {
				billingRun.setProcessType(BillingProcessTypesEnum.MANUAL);
			}
			if (billingRun != null && billingRun.getId() != null
					&& preReport.get() != null && preReport.get()) {
				PreInvoicingReportsDTO preInvoicingReportsDTO = billingRunService
						.generatePreInvoicingReports(billingRun);
				billingRun.setPreInvoicingReports(preInvoicingReportsDTO);
			} else if (billingRun != null && billingRun.getId() != null
					&& postReport.get() != null && postReport.get()) {
				PostInvoicingReportsDTO postInvoicingReportsDTO = billingRunService
						.generatePostInvoicingReports(billingRun);
				billingRun.setPostInvoicingReports(postInvoicingReportsDTO);
			}
			invoicesModel = new ListDataModel<Invoice>(billingRun.getInvoices());

		} catch (BusinessException e) {
			log.error("Failed to initialize an object", e);
		}

		return billingRun;
	}

	@Produces
	@Named("billingRunInvoices")
	@ConversationScoped
	public List<Invoice> getBillingRunInvoices() {
		if (entity == null) {
			return null;
		}
		return entity.getInvoices();
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<BillingRun> getPersistenceService() {
		return billingRunService;
	}

	public String launchRecurringInvoicing() {
		log.info("launchInvoicing billingRun BillingCycle={}", entity
				.getBillingCycle().getCode());
		try {
			ParamBean param = ParamBean.getInstance("meveo-admin.properties");
			String allowManyInvoicing = param.getProperty(
					"billingRun.allowManyInvoicing", "true");
			boolean isAllowed = Boolean.parseBoolean(allowManyInvoicing);
			log.info("launchInvoicing allowManyInvoicing=#", isAllowed);
			if (billingRunService
					.isActiveBillingRunsExist(getCurrentProvider())
					&& !isAllowed) {
				Messages.createError(
						"error.invoicing.alreadyLaunched");
				return null;
			}

			entity.setStatus(BillingRunStatusEnum.NEW);
			entity.setProcessDate(new Date());
			billingRunService.create(entity);
			entity.setProvider(entity.getBillingCycle().getProvider());

			return "/pages/billing/invoicing/billingRuns.xhtml?edit=false";

		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String confirmInvoicing() {
		try {
			// statusMessages.add("facturation confirmee avec succes");
			entity.setStatus(BillingRunStatusEnum.ON_GOING);
			billingRunService.update(entity);
			return "/pages/billing/invoicing/billingRuns.xhtml?edit=false";

		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String validateInvoicing() {
		try {
			entity.setStatus(BillingRunStatusEnum.CONFIRMED);
			billingRunService.update(entity);
			endConversation();
			return "/pages/billing/invoicing/billingRuns.xhtml?faces-redirect=true&edit=false";
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String cancelInvoicing() {
		try {
			entity.setStatus(BillingRunStatusEnum.CANCELED);
			billingRunService.update(entity);
			endConversation();
			return "/pages/billing/invoicing/billingRuns.xhtml?faces-redirect=true&edit=false";
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String cancelConfirmedInvoicing() {
		try {
			entity.setStatus(BillingRunStatusEnum.CANCELED);
			billingRunService.cleanBillingRun(entity);
			billingRunService.update(entity);
			endConversation();
			return "/pages/billing/invoicing/billingRuns.xhtml?faces-redirect=true&edit=false";
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String rerateConfirmedInvoicing() {
		try {
			billingRunService.retateBillingRunTransactions(entity);
			cancelConfirmedInvoicing();
			return "/pages/billing/invoicing/billingRuns.xhtml?edit=false";
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String rerateInvoicing() {
		try {
			billingRunService.retateBillingRunTransactions(entity);
			cancelInvoicing();
			return "/pages/billing/invoicing/billingRuns.xhtml?edit=false";
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String preInvoicingRepport(long id) {
		try {
			return "/pages/billing/invoicing/preInvoicingReports.xhtml?edit=false&preReport=true&objectId="
					+ id;

		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String postInvoicingRepport(long id) {
		try {
			return "/pages/billing/invoicing/postInvoicingReports.xhtml?edit=false&postReport=true&objectId="
					+ id;

		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError(e.getMessage());
		}
		return null;
	}

	public String excludeBillingAccounts() {
		try {
			log.debug("excludeBillingAccounts itemSelector.size()=#0",
					itemSelector.getSize());
			for (Invoice invoice : itemSelector.getList()) {
				billingRunService.deleteInvoice(invoice);
			}
			Messages.createInfo(
					"info.invoicing.billingAccountExcluded");

		} catch (Exception e) {
			log.error("unexpected exception when excluding BillingAccounts!", e);
			Messages.createError(e.getMessage());
			Messages.createError(e.getMessage());
		}

		return "/pages/billing/invoicing/postInvoicingReports.xhtml?edit=false&postReport=true&objectId="
				+ entity.getId();
	}

	/**
	 * Item selector getter. Item selector keeps a state of multiselect
	 * checkboxes.
	 */
	@Produces
	@Named("itemSelector")
	@ConversationScoped
	public ListItemsSelector<Invoice> getItemSelector() {
		if (itemSelector == null) {
			itemSelector = new ListItemsSelector<Invoice>(false);
		}
		return itemSelector;
	}

	/**
	 * Check/uncheck all select boxes.
	 */
	public void checkUncheckAll(ValueChangeEvent event) {
		itemSelector.switchMode();
	}

	/**
	 * Listener of select changed event.
	 */
	public void selectChanged(ValueChangeEvent event) {

		Invoice entity = (Invoice) invoicesModel.getRowData();
		log.debug("selectChanged=#0", entity != null ? entity.getId() : null);
		if (entity != null) {
			itemSelector.check(entity);
		}
		log.debug("selectChanged itemSelector.size()=#0",
				itemSelector.getSize());
	}

	/**
	 * Resets item selector.
	 */
	public void resetSelection() {
		if (itemSelector == null) {
			itemSelector = new ListItemsSelector<Invoice>(false);
		} else {
			itemSelector.reset();
		}
	}

	public DataModel<Invoice> getInvoicesModel() {
		return invoicesModel;
	}

	public void setInvoicesModel(DataModel<Invoice> invoicesModel) {
		this.invoicesModel = invoicesModel;
	}

	@Override
	protected String getListViewName() {
		return "billingRuns";
	}

	@Override
	protected String getDefaultSort() {
		return "id";
	}
}
