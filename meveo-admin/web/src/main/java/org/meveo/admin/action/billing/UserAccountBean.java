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
package org.meveo.admin.action.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.AccountBean;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.DuplicateDefaultAccountException;
import org.meveo.admin.util.pagination.EntityListDataModelPF;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.cache.WalletCacheContainerProvider;
import org.meveo.model.Auditable;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.CounterInstance;
import org.meveo.model.billing.OperationTypeEnum;
import org.meveo.model.billing.ProductChargeInstance;
import org.meveo.model.billing.ProductInstance;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.CounterInstanceService;
import org.meveo.service.billing.impl.ProductChargeInstanceService;
import org.meveo.service.billing.impl.ProductInstanceService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.billing.impl.WalletOperationService;
import org.meveo.service.billing.impl.WalletReservationService;
import org.meveo.service.catalog.impl.ProductTemplateService;
import org.omnifaces.cdi.ViewScoped;
import org.omnifaces.util.Faces;
import org.primefaces.model.LazyDataModel;

/**
 * Standard backing bean for {@link UserAccount} (extends {@link BaseBean} that
 * provides almost all common methods to handle entities filtering/sorting in
 * datatable, their create, edit, view, delete operations). It works with Manaty
 * custom JSF components.
 */
@Named
@ViewScoped
public class UserAccountBean extends AccountBean<UserAccount> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected
	 * 
	 * @{link UserAccount} service. Extends {@link PersistenceService} .
	 */

	@Inject
	private WalletOperationBean walletOperationBean;

	@Inject
	WalletOperationService walletOperationService;

	@Inject
	WalletReservationService walletReservationService;

	@Inject
	private UserAccountService userAccountService;

	@Inject
	private RatedTransactionService ratedTransactionService;

	@Inject
	private BillingAccountService billingAccountService;
	
	@Inject
	private WalletCacheContainerProvider walletCacheContainerProvider;

    @Inject 
    private CounterInstanceService counterInstanceService;
    
    @Inject
	private ProductChargeInstanceService productChargeInstanceService;
    
    @Inject
    private ProductInstanceService productInstanceService;
    
    @Inject
    private ProductTemplateService productTemplateService;
	   
	private CounterInstance selectedCounterInstance;
	private ProductInstance productInstance;
	private ProductChargeInstance productChargeInstance = new ProductChargeInstance();

	private Long billingAccountId;
	private WalletOperation reloadOperation;
	private String selectedWalletCode;

	// Retrieved wallet operations to improve GUI performance for Ajax request
    private Map<String, List<WalletOperation>> walletOperations = new HashMap<String, List<WalletOperation>>();
    
    private EntityListDataModelPF<ProductChargeInstance> productChargeInstances = null;
	
	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public UserAccountBean() {
		super(UserAccount.class);
	}

	public Long getBillingAccountId() {
		return billingAccountId;
	}

	public void setBillingAccountId(Long billingAccountId) {
		this.billingAccountId = billingAccountId;
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@Override
	public UserAccount initEntity() {
		super.initEntity();

		if (entity.getId() == null && billingAccountId != null) {
			BillingAccount billingAccount = billingAccountService.findById(billingAccountId,
					Arrays.asList("customerAccount"));
			entity.setBillingAccount(billingAccount);
			populateAccounts(billingAccount);

		}
		selectedCounterInstance=entity.getCounters()!=null && entity.getCounters().size()>0?entity.getCounters().values().iterator().next():null;
		return entity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
	 */
	@Override
    @ActionMethod
	public String saveOrUpdate(boolean killConversation) throws BusinessException{
		
	    entity.setBillingAccount(billingAccountService.attach(entity.getBillingAccount()));
	    
	    try {

            String outcome = super.saveOrUpdate(killConversation);

            if (outcome != null) {
                return getEditViewName(); // "/pages/billing/userAccounts/userAccountDetail.xhtml?edit=true&userAccountId=" + entity.getId() +
                                          // "&faces-redirect=true&includeViewParams=true";
            }

		} catch (DuplicateDefaultAccountException e1) {
			messages.error(new BundleKey("messages", "error.account.duplicateDefautlLevel"));
		} catch (Exception e) {
			log.error("failed to save or update user account ",e);
			messages.error(new BundleKey("messages", "javax.el.ELException"));

		}

		return null;
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<UserAccount> getPersistenceService() {
		return userAccountService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.meveo.admin.action.BaseBean#saveOrUpdate(org.meveo.model.IEntity)
	 */
	@Override 
    @ActionMethod
	// TODO this has to be removed as BaseBean has identical method. Only need to take care of userAccountService.createUserAccount method call
	protected UserAccount saveOrUpdate(UserAccount entity) throws BusinessException{

        if (entity.isTransient()) {
            userAccountService.createUserAccount(entity.getBillingAccount(), entity, getCurrentUser());
        } else {
            entity = getPersistenceService().update(entity, getCurrentUser());
        }

        setObjectId((Long) entity.getId());

		return entity;
	}

    public String terminateAccount() {
        log.debug("resiliateAccount userAccountId:" + entity.getId());
        try {
            entity = userAccountService.attach(entity);
            entity = userAccountService.userAccountTermination(entity, entity.getTerminationDate(), entity.getTerminationReason(), getCurrentUser());
            messages.info(new BundleKey("messages", "resiliation.resiliateSuccessful"));

        } catch (Exception e) {
            log.error("Failed to terminate account ", e);
            messages.error(new BundleKey("messages", "resiliation.resiliateUnsuccessful"), e.getMessage());
        }
        return getEditViewName();
	}

	public String cancelAccount() {
		log.info("cancelAccount userAccountId:" + entity.getId());
		try {
		    entity = userAccountService.attach(entity);
            entity = userAccountService.userAccountCancellation(entity, new Date(), getCurrentUser());
			messages.info(new BundleKey("messages", "cancellation.cancelSuccessful"));
			        
        } catch (Exception e) {
            log.error("Failed to cancel account ", e);
            messages.error(new BundleKey("messages", "cancellation.cancelUnsuccessful"), e.getMessage());
        }
        return getEditViewName();
	}

	public String reactivateAccount() {
		log.info("reactivateAccount userAccountId:" + entity.getId());
		try {
		    entity = userAccountService.attach(entity);
            entity = userAccountService.userAccountReactivation(entity, new Date(), getCurrentUser());
			messages.info(new BundleKey("messages", "reactivation.reactivateSuccessful"));
			
        } catch (Exception e) {
            log.error("Failed to reactivate account ", e);
            messages.error(new BundleKey("messages", "reactivation.reactivateUnsuccessful"), e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
		}
        return getEditViewName();
	}

	public LazyDataModel<WalletOperation> getWalletOperationsNoInvoiced() {
		log.debug("getWalletOperationsNoInvoiced");
		LazyDataModel<WalletOperation> result = null;
		HashMap<String, Object> filters = new HashMap<String, Object>();
		if (entity == null) {
			log.debug("getWalletOperationsNoInvoiced: userAccount is null");
			initEntity();
			log.debug("entity.id=" + entity.getId());
		}
		if (entity.getWallet() == null) {
			log.debug("getWalletOperationsNoInvoiced: userAccount " + entity.getId() + " has no wallet");
		} else {
			filters.put("wallet", entity.getWallet());
			filters.put("status", WalletOperationStatusEnum.OPEN);
			result = walletOperationBean.getLazyDataModel(filters, true);
		}
		return result;
	}

	public List<WalletOperation> getWalletOperations(String walletCode) {
		
        if (entity != null && !entity.isTransient() && !walletOperations.containsKey(walletCode)) {
            log.debug("getWalletOperations {}", walletCode);
            walletOperations.put(walletCode, walletOperationService.findByUserAccountAndWalletCode(walletCode, entity, false));
        }

		return walletOperations.get(walletCode);
	}

	@Produces
	@Named("getRatedTransactionsInvoiced")
	public List<RatedTransaction> getRatedTransactionsInvoiced() {
		return ratedTransactionService.getRatedTransactionsInvoiced(entity);
	}

	public void populateAccounts(BillingAccount billingAccount) {
		entity.setBillingAccount(billingAccount);
		
		if (billingAccount.getProvider() != null && billingAccount.getProvider().isLevelDuplication()) {
			entity.setCode(billingAccount.getCode());
			entity.setDescription(billingAccount.getDescription());
			entity.setAddress(billingAccount.getAddress());
			entity.setExternalRef1(billingAccount.getExternalRef1());
			entity.setExternalRef2(billingAccount.getExternalRef2());
			entity.setProviderContact(billingAccount.getProviderContact());
			entity.setName(billingAccount.getName());
			entity.setProvider(billingAccount.getProvider());
			entity.setSubscriptionDate(billingAccount.getSubscriptionDate());
			entity.setPrimaryContact(billingAccount.getPrimaryContact());
		}
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider", "billingAccount", "billingAccount.customerAccount",
				"billingAccount.customerAccount.customer");
	}

	@Override
	protected List<String> getListFieldsToFetch() {
		return Arrays.asList("provider", "billingAccount");
	}

	public WalletOperation getReloadOperation() {
		return reloadOperation;
	}

	public void setReloadOperation(WalletOperation reloadOperation) {
		this.reloadOperation = reloadOperation;
	}

	public String getSelectedWalletCode() {
		return selectedWalletCode;
	}

	public void setSelectedWalletCode(String selectedWalletCode) {
		this.selectedWalletCode = selectedWalletCode;
		this.reloadOperation = new WalletOperation();
		reloadOperation.setCode("RELOAD");
		reloadOperation.setOperationDate(new Date());
		reloadOperation.setQuantity(BigDecimal.ONE);
		reloadOperation.setCurrency(entity.getBillingAccount().getCustomerAccount().getTradingCurrency().getCurrency());
		reloadOperation.setWallet(entity.getWalletInstance(selectedWalletCode));
		reloadOperation.setDescription("reload");
		reloadOperation.setProvider(entity.getProvider());
		reloadOperation.setSeller(entity.getBillingAccount().getCustomerAccount().getCustomer().getSeller());
		reloadOperation.setStatus(WalletOperationStatusEnum.TREATED);
		reloadOperation.setType(OperationTypeEnum.CREDIT);
	}

	public void reload() throws BusinessException {
		walletOperationService.create(reloadOperation, getCurrentUser());
		reloadOperation = null;
	}

	public String getBalance(WalletInstance wallet) {
        
		String result = null;
		BigDecimal balance = walletCacheContainerProvider.getBalance(wallet.getId());
		if (balance != null) {
			result = balance.toPlainString();
		}
		return result;
	}

	public String getReservedBalance(WalletInstance wallet) {
		String result = null;
		BigDecimal balance = walletCacheContainerProvider.getReservedBalance(wallet.getId());
		if (balance != null) {
			result = balance.toPlainString();
		}
		return result;
	}

	public String getOpenBalanceWithoutTax(Provider provider, String sellerCode, String userAccountCode,
			Date startDate, Date endDate) throws BusinessException {
		String result = null;
		BigDecimal balance = walletReservationService.getOpenBalanceWithoutTax(provider, sellerCode, userAccountCode,
				startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public String getOpenBalanceWithTax(Provider provider, String sellerCode, String userAccountCode, Date startDate,
			Date endDate) throws BusinessException {

	    String result = null;
		BigDecimal balance = walletReservationService.getOpenBalanceWithTax(provider, sellerCode, userAccountCode,
				startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public String getReservedBalanceWithoutTax(Provider provider, String sellerCode, String userAccountCode,
			Date startDate, Date endDate) throws BusinessException {
		String result = null;
		BigDecimal balance = walletReservationService.getReservedBalanceWithoutTax(provider, sellerCode,
				userAccountCode, startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public String getReservedBalanceWithTax(Provider provider, String sellerCode, String userAccountCode,
			Date startDate, Date endDate) throws BusinessException {
		String result = null;
		BigDecimal balance = walletReservationService.getReservedBalanceWithTax(provider, sellerCode, userAccountCode,
				startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public String getCurrentBalanceWithoutTax(Provider provider, String sellerCode, String userAccountCode,
			Date startDate, Date endDate) throws BusinessException {
		String result = null;
		BigDecimal balance = walletReservationService.getCurrentBalanceWithoutTax(provider, sellerCode,
				userAccountCode, startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public String getCurrentBalanceWithTax(Provider provider, String sellerCode, String userAccountCode,
			Date startDate, Date endDate) throws BusinessException {
		String result = null;
		BigDecimal balance = walletReservationService.getCurrentBalanceWithTax(provider, sellerCode, userAccountCode,
				startDate, endDate);
		if (balance != null) {
			result = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}
		return result;
	}

	public List<SelectItem> getWalletOperationStatusList() {
		ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Faces.getLocale());

		List<SelectItem> filterLockedOptions = new ArrayList<SelectItem>(Arrays.asList(new SelectItem("OPEN",
				resourceBundle.getString("walletOperationStatus.open")),
				new SelectItem("TREATED", resourceBundle.getString("walletOperationStatus.treated")), new SelectItem(
						"CANCELED", resourceBundle.getString("walletOperationStatus.canceled")), new SelectItem(
						"RESERVED", resourceBundle.getString("walletOperationStatus.reserved")), new SelectItem(
						"TO_RERATE", resourceBundle.getString("walletOperationStatus.to_rerate"))));

		return filterLockedOptions;
	}


	 public CounterInstance getSelectedCounterInstance() {
		   if(entity==null){
		    initEntity();
		   }
		  return selectedCounterInstance;
		 }

    public void setSelectedCounterInstance(CounterInstance selectedCounterInstance) {
        if (selectedCounterInstance != null) {
            this.selectedCounterInstance = counterInstanceService.refreshOrRetrieve(selectedCounterInstance);
        } else {
            this.selectedCounterInstance = null;
        }
    }
    
    public EntityListDataModelPF<ProductChargeInstance> getProductChargeInstances() {

        if (productChargeInstances != null || (entity == null || entity.getId() == null)) {
            return productChargeInstances;
        }

        productChargeInstances = new EntityListDataModelPF<ProductChargeInstance>(new ArrayList<ProductChargeInstance>());
        productChargeInstances.addAll(productChargeInstanceService.findByUserAccountId(entity.getId()));
        return productChargeInstances;
    }

	public ProductInstance getProductInstance() {
		return productInstance;
	}

	public void setProductInstance(ProductInstance productInstance) {
		this.productInstance = productInstance;
	}
	
	public void initProductInstance() {
		productInstance = new ProductInstance();
		productChargeInstance = new ProductChargeInstance();
	}

	public void applyProduct() {
		if (productInstance != null) {
			productInstance.setCode(productInstance.getProductTemplate().getCode());
			productInstance.setDescription(productInstance.getProductTemplate().getDescription());
			if (productInstance.getApplicationDate() == null) {
				productInstance.setApplicationDate(new Date());
			}
			Auditable auditable = new Auditable();
			auditable.setCreated(new Date());
			auditable.setCreator(currentUser);
			productInstance.setAuditable(auditable);
		}
		productInstance.setUserAccount(getPersistenceService().refreshOrRetrieve(entity));
		productInstance.setProductTemplate(productTemplateService.refreshOrRetrieve(productInstance.getProductTemplate()));

		try {
			productInstanceService.create(productInstance, currentUser);
			List<WalletOperation> walletOps = productInstanceService.applyProductInstance(productInstance, null, null, null, currentUser, true);
			
			if (walletOps == null || walletOps.size() == 0) {
				messages.error(new BundleKey("messages", "message.userAccount.applyProduct.noProductCharge"));
			}
			
		} catch (BusinessException e) {
			messages.error(new BundleKey("messages", "message.product.application.fail"), e.getMessage());
		} catch (Exception e) {
			log.error("unexpected exception when applying a product! {}", e.getMessage());
			messages.error(new BundleKey("messages", "message.product.application.fail"), e.getMessage());
		}
		productChargeInstances = null;
	}

	public ProductChargeInstance getProductChargeInstance() {
		return productChargeInstance;
	}

	public void setProductChargeInstance(ProductChargeInstance productChargeInstance) {
		this.productChargeInstance = productChargeInstance;
	}

}