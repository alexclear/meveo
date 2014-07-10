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
package org.meveo.admin.action.catalog;

import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.model.billing.CatMessages;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceSubcategoryCountry;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.InvoiceSubCategoryCountryService;
import org.meveo.service.catalog.impl.CatMessagesService;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.omnifaces.util.Messages;

/**
 * Standard backing bean for {@link InvoiceSubCategory} (extends
 * {@link BaseBean} that provides almost all common methods to handle entities
 * filtering/sorting in datatable, their create, edit, view, delete operations).
 * It works with Manaty custom JSF components.
 * 
 * @author Ignas
 * @created Dec 15, 2010
 */
@Named
@ConversationScoped
public class InvoiceSubCategoryBean extends BaseBean<InvoiceSubCategory> {
	private static final long serialVersionUID = 1L;

	/**
	 * Injected @{link InvoiceSubCategory} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	private CatMessagesService catMessagesService;

	@Inject
	private InvoiceSubCategoryCountryService invoiceSubCategoryCountryService;

	/**
	 * Inject InvoiceCategory service, that is used to load default category if
	 * its id was passed in parameters.
	 */
	@Inject
	private InvoiceCategoryService invoiceCategoryService;

	/**
	 * InvoiceCategory Id passed as a parameter. Used when creating new
	 * InvoiceSubCategory from InvoiceCategory window, so default
	 * InvoiceCategory will be set on newly created InvoiceSubCategory.
	 */
	@Inject
	@RequestParam
	private Instance<Long> invoiceCategoryId;

	@Produces
	@Named
	private InvoiceSubcategoryCountry invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();

	public void newInvoiceSubcategoryCountryInstance() {
		this.invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();
	}

	public void saveInvoiceSubCategoryCountry() {
		log.info("saveOneShotChargeIns getObjectId=#0", getObjectId());

		try {
			if (invoiceSubcategoryCountry != null) {
				for (InvoiceSubcategoryCountry inc : entity.getInvoiceSubcategoryCountries()) {
					if (inc.getTradingCountry()
							.getCountry()
							.getCountryCode()
							.equalsIgnoreCase(
									invoiceSubcategoryCountry.getTradingCountry().getCountry()
											.getCountryCode())
							&& !inc.getId().equals(invoiceSubcategoryCountry.getId())) {
						throw new Exception();
					}
				}
				if (invoiceSubcategoryCountry.getId() != null) {
					invoiceSubCategoryCountryService.update(invoiceSubcategoryCountry);
					Messages.createInfo( "update.successful");
				} else {
					invoiceSubcategoryCountry.setInvoiceSubCategory(entity);
					invoiceSubCategoryCountryService.create(invoiceSubcategoryCountry);
					entity.getInvoiceSubcategoryCountries().add(invoiceSubcategoryCountry);
					Messages.createInfo( "save.successful");
				}
			}
		} catch (Exception e) {
			log.error("exception when applying one invoiceSubCategoryCountry !", e);
			Messages.createError( "invoiceSubCategory.uniqueTaxFlied");
		}
		invoiceSubcategoryCountry = new InvoiceSubcategoryCountry();
	}

	public void deleteInvoiceSubcategoryCountry(InvoiceSubcategoryCountry invoiceSubcategoryCountry) {
		invoiceSubCategoryCountryService.remove(invoiceSubcategoryCountry);
		entity.getInvoiceSubcategoryCountries().remove(invoiceSubcategoryCountry);
	}

	public void editInvoiceSubcategoryCountry(InvoiceSubcategoryCountry invoiceSubcategoryCountry) {
		this.invoiceSubcategoryCountry = invoiceSubcategoryCountry;
	}

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public InvoiceSubCategoryBean() {
		super(InvoiceSubCategory.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public InvoiceSubCategory initEntity() {
		InvoiceSubCategory invoiceCatSub = super.initEntity();
		languageMessagesMap.clear();
		if (invoiceCatSub.getId() != null) {
			for (CatMessages msg : catMessagesService.getCatMessagesList(InvoiceSubCategory.class
					.getSimpleName() + "_" + invoiceCatSub.getId())) {
				languageMessagesMap.put(msg.getLanguageCode(), msg.getDescription());
			}
		}
		if (invoiceCategoryId.get() != null) {
			entity.setInvoiceCategory(invoiceCategoryService.findById(invoiceCategoryId.get()));
		}
		return invoiceCatSub;
	}

	public List<InvoiceSubCategory> listAll() {
		getFilters();
		if (filters.containsKey("languageCode")) {
			filters.put("language.languageCode", filters.get("languageCode"));
			filters.remove("languageCode");
		} else if (filters.containsKey("language.languageCode")) {
			filters.remove("language.languageCode");
		}
		return super.listAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
	 */
	@Override
	public String saveOrUpdate(boolean killConversation) {
		String back = null;
		if (entity.getId() != null) {
			for (String msgKey : languageMessagesMap.keySet()) {
				String description = languageMessagesMap.get(msgKey);
				CatMessages catMsg = catMessagesService.getCatMessages(entity.getClass()
						.getSimpleName() + "_" + entity.getId(), msgKey);
				if (catMsg != null) {
					catMsg.setDescription(description);
					catMessagesService.update(catMsg);
				} else {
					CatMessages catMessages = new CatMessages(entity.getClass().getSimpleName()
							+ "_" + entity.getId(), msgKey, description);
					catMessagesService.create(catMessages);
				}
			}
			super.saveOrUpdate(killConversation);

		} else {
			getPersistenceService().create(entity);
			Messages.createInfo( "invoiceSubCaterogy.AddTax");
			if (killConversation) {
				endConversation();
			}
			for (String msgKey : languageMessagesMap.keySet()) {
				String description = languageMessagesMap.get(msgKey);
				CatMessages catMessages = new CatMessages(entity.getClass().getSimpleName() + "_"
						+ entity.getId(), msgKey, description);
				catMessagesService.create(catMessages);
			}

		}

		return back;
	}

	@Override
	protected String getListViewName() {
		return "invoiceSubCategories";
	}

	/**
	 * Override default list view name. (By default its class name starting
	 * lower case + 's').
	 * 
	 * @see org.meveo.admin.action.BaseBean#getDefaultViewName()
	 */
	protected String getDefaultViewName() {
		return "invoiceSubCategories";
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<InvoiceSubCategory> getPersistenceService() {
		return invoiceSubCategoryService;
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

}