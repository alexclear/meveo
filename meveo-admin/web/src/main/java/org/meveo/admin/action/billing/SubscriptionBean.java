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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.EntityListDataModelPF;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.OneShotChargeInstance;
import org.meveo.model.billing.RecurringChargeInstance;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.billing.UsageChargeInstance;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.mediation.Access;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.OneShotChargeInstanceService;
import org.meveo.service.billing.impl.RecurringChargeInstanceService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.billing.impl.UsageChargeInstanceService;
import org.meveo.service.billing.impl.UserAccountService;
import org.omnifaces.util.Messages;

@Named
@ConversationScoped
public class SubscriptionBean extends BaseBean<Subscription> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected
	 * 
	 * @{link Subscription} service. Extends {@link PersistenceService}
	 */
	@Inject
	private SubscriptionService subscriptionService;

	/**
	 * UserAccount service. TODO (needed?)
	 */
	@Inject
	private UserAccountService userAccountService;

	private ServiceInstance selectedServiceInstance;

	private Integer quantity = 1;

	private OneShotChargeInstance oneShotChargeInstance = new OneShotChargeInstance();

	private RecurringChargeInstance recurringChargeInstance;

	@Inject
	private ServiceInstanceService serviceInstanceService;

	@Inject
	private OneShotChargeInstanceService oneShotChargeInstanceService;

	@Inject
	private RecurringChargeInstanceService recurringChargeInstanceService;

	@Inject
	private UsageChargeInstanceService usageChargeInstanceService;

	private Integer oneShotChargeInstanceQuantity = 1;

	private Integer recurringChargeServiceInstanceQuantity = 1;

	/**
	 * User Account Id passed as a parameter. Used when creating new
	 * subscription entry from user account definition window, so default uset
	 * Account will be set on newly created subscription entry.
	 */
	private Long userAccountId;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */

	private EntityListDataModelPF<ServiceTemplate> serviceTemplates = new EntityListDataModelPF<ServiceTemplate>(
			new ArrayList<ServiceTemplate>());

	private EntityListDataModelPF<ServiceInstance> serviceInstances = new EntityListDataModelPF<ServiceInstance>(
			new ArrayList<ServiceInstance>());

	public SubscriptionBean() {
		super(Subscription.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationExceptionC
	 */
	public Subscription initEntity() {
		super.initEntity();
		if (userAccountId != null) {
			UserAccount userAccount = userAccountService
					.findById(getUserAccountId());
			populateAccounts(userAccount);

			// check if has default
			if (!userAccount.getDefaultLevel()) {
				entity.setDefaultLevel(true);
			}
		}
		if (entity.getId() == null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(entity.getSubscriptionDate());
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			entity.setSubscriptionDate(calendar.getTime());
		} else {
			log.debug("entity.getOffer()=" + entity.getOffer().getCode());
			if (entity.getOffer() != null) {
				List<ServiceInstance> serviceInstances = entity
						.getServiceInstances();
				for (ServiceTemplate serviceTemplate : entity.getOffer()
						.getServiceTemplates()) {
					boolean alreadyInstanciated = false;
					for (ServiceInstance serviceInstance : serviceInstances) {
						if (serviceTemplate.getCode().equals(
								serviceInstance.getCode())) {
							alreadyInstanciated = true;
							break;
						}
					}
					if (!alreadyInstanciated) {
						serviceTemplates.add(serviceTemplate);
					}

				}
			}
			serviceInstances.addAll(entity.getServiceInstances());

		}

		log.debug("serviceInstances=" + serviceInstances.getSize());
		log.debug("servicetemplates=" + serviceTemplates.getSize());
		return entity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
	 */
	public String saveOrUpdate(boolean killConversation) {
		if (entity.getDefaultLevel() != null && entity.getDefaultLevel()) {
			// UserAccount userAccount = entity.getUserAccount();
			if (subscriptionService.isDuplicationExist(entity)) {
				entity.setDefaultLevel(false);
				Messages.createError(
						"error.account.duplicateDefautlLevel");
				return null;
			}

		}

		super.saveOrUpdate(killConversation);
		return "/pages/billing/subscriptions/subscriptionDetail?edit=false&subscriptionId="
				+ entity.getId()
				+ "&faces-redirect=true&includeViewParams=true";
	}

	@Override
	protected String saveOrUpdate(Subscription entity) {
		if (entity.isTransient()) {
			subscriptionService.create(entity);
			serviceTemplates.addAll(entity.getOffer().getServiceTemplates());
			Messages.createInfo( "save.successful");
		} else {
			subscriptionService.update(entity);
			Messages.createInfo( "update.successful");
		}

		return back();
	}

	public void newOneShotChargeInstance() {
		log.debug("newOneShotChargeInstance ");
		this.oneShotChargeInstance = new OneShotChargeInstance();
	}

	public void editOneShotChargeIns(OneShotChargeInstance oneShotChargeIns) {
		this.oneShotChargeInstance = oneShotChargeIns;
	}

	public void saveOneShotChargeIns() {
		log.debug("saveOneShotChargeIns getObjectId=" + getObjectId());

		try {
			if (oneShotChargeInstance != null
					&& oneShotChargeInstance.getId() != null) {
				oneShotChargeInstanceService.update(oneShotChargeInstance);
			} else {
				if (oneShotChargeInstance.getChargeDate() == null) {
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(new Date());
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MINUTE, 0);
					oneShotChargeInstance.setChargeDate(calendar.getTime());
				}

				oneShotChargeInstance.setSubscription(entity);
				oneShotChargeInstance.setSeller(entity.getUserAccount()
						.getBillingAccount().getCustomerAccount().getCustomer()
						.getSeller());
				oneShotChargeInstance.setCurrency(entity.getUserAccount()
						.getBillingAccount().getCustomerAccount()
						.getTradingCurrency());
				oneShotChargeInstance.setCountry(entity.getUserAccount()
						.getBillingAccount().getTradingCountry());
				Long id = oneShotChargeInstanceService
						.oneShotChargeApplication(entity,
								(OneShotChargeTemplate) oneShotChargeInstance
										.getChargeTemplate(),
								oneShotChargeInstance.getChargeDate(),
								oneShotChargeInstance.getAmountWithoutTax(),
								oneShotChargeInstance.getAmountWithTax(),
								oneShotChargeInstanceQuantity,
								oneShotChargeInstance.getCriteria1(),
								oneShotChargeInstance.getCriteria2(),
								oneShotChargeInstance.getCriteria3(),
								oneShotChargeInstance.getSeller(),
								getCurrentUser());
				oneShotChargeInstance.setId(id);
				oneShotChargeInstance.setProvider(oneShotChargeInstance
						.getChargeTemplate().getProvider());
			}
			Messages.createInfo( "save.successful");

			clearObjectId();
		} catch (Exception e) {
			log.error("exception when applying one shot charge!",
					e.getMessage());
			Messages.createError(e.getMessage());
		}
	}

	public void newRecurringChargeInstance() {
		this.recurringChargeInstance = new RecurringChargeInstance();
	}

	public void editRecurringChargeIns(
			RecurringChargeInstance recurringChargeIns) {
		this.recurringChargeInstance = recurringChargeIns;
		recurringChargeServiceInstanceQuantity = recurringChargeIns
				.getServiceInstance().getQuantity();
		log.debug("setting recurringChargeIns " + recurringChargeIns);
	}

	public void saveRecurringChargeIns() {
		log.debug("saveRecurringChargeIns getObjectId=#0", getObjectId());
		try {
			if (recurringChargeInstance != null) {
				if (recurringChargeInstance.getId() != null) {
					log.debug("update RecurringChargeIns #0, id:#1",
							recurringChargeInstance,
							recurringChargeInstance.getId());
					recurringChargeInstance.getServiceInstance().setQuantity(
							recurringChargeServiceInstanceQuantity);
					recurringChargeInstanceService
							.update(recurringChargeInstance);
				} else {
					log.debug("save RecurringChargeIns #0",
							recurringChargeInstance);

					recurringChargeInstance.setSubscription(entity);
					Long id = recurringChargeInstanceService
							.recurringChargeApplication(
									entity,
									(RecurringChargeTemplate) recurringChargeInstance
											.getChargeTemplate(),
									recurringChargeInstance.getChargeDate(),
									recurringChargeInstance
											.getAmountWithoutTax(),
									recurringChargeInstance.getAmountWithTax(),
									1, recurringChargeInstance.getCriteria1(),
									recurringChargeInstance.getCriteria2(),
									recurringChargeInstance.getCriteria3(),
									getCurrentUser());
					recurringChargeInstance.setId(id);
					recurringChargeInstance.setProvider(recurringChargeInstance
							.getChargeTemplate().getProvider());
				}
				Messages.createInfo( "save.successful");
				recurringChargeInstance = new RecurringChargeInstance();
				clearObjectId();
			}
		} catch (BusinessException e1) {
			Messages.createError(e1.getMessage());
		} catch (Exception e) {
			log.error("exception when applying recurring charge!", e);
			Messages.createError(e.getMessage());
		}
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<Subscription> getPersistenceService() {
		return subscriptionService;
	}

	// /**
	// * @see org.meveo.admin.action.BaseBean#getFormFieldsToFetch()
	// */
	// protected List<String> getFormFieldsToFetch() {
	// return Arrays.asList("serviceInstances");
	// }
	//
	// /**
	// * @see org.meveo.admin.action.BaseBean#getListFieldsToFetch()
	// */
	// protected List<String> getListFieldsToFetch() {
	// return Arrays.asList("serviceInstances");
	// }

	public EntityListDataModelPF<ServiceInstance> getServiceInstances() {
		return serviceInstances;
	}

	public EntityListDataModelPF<ServiceTemplate> getServiceTemplates() {
		return serviceTemplates;
	}

	public OneShotChargeInstance getOneShotChargeInstance() {
		return oneShotChargeInstance;
	}

	public RecurringChargeInstance getRecurringChargeInstance() {
		log.debug("getRecurringChargeInstance " + recurringChargeInstance);
		return recurringChargeInstance;
	}

	public List<OneShotChargeInstance> getOneShotChargeInstances() {
		return (entity == null || entity.getId() == null) ? null
				: oneShotChargeInstanceService
						.findOneShotChargeInstancesBySubscriptionId(entity
								.getId());
	}

	public List<WalletOperation> getOneShotWalletOperations() {
		log.debug("getOneShotWalletOperations");
		if (this.oneShotChargeInstance == null
				|| this.oneShotChargeInstance.getId() == null) {
			return null;
		}
		List<WalletOperation> results = new ArrayList<WalletOperation>(
				oneShotChargeInstance.getWalletOperations());

		Collections.sort(results, new Comparator<WalletOperation>() {
			public int compare(WalletOperation c0, WalletOperation c1) {

				return c1.getOperationDate().compareTo(c0.getOperationDate());
			}
		});
		log.debug("retrieved " + (results != null ? results.size() : 0)
				+ " WalletOperations");
		return results;
	}

	public List<WalletOperation> getRecurringWalletOperations() {
		log.debug("getRecurringWalletOperations");
		if (this.recurringChargeInstance == null
				|| this.recurringChargeInstance.getId() == null) {
			log.debug("recurringChargeInstance is null");
			return null;
		}
		log.debug("recurringChargeInstance is "
				+ recurringChargeInstance.getId());
		List<WalletOperation> results = new ArrayList<WalletOperation>(
				recurringChargeInstance.getWalletOperations());
		Collections.sort(results, new Comparator<WalletOperation>() {
			public int compare(WalletOperation c0, WalletOperation c1) {

				return c1.getOperationDate().compareTo(c0.getOperationDate());
			}
		});
		log.debug("retrieve " + (results != null ? results.size() : 0)
				+ " WalletOperations");
		return results;
	}

	// @Factory("recurringChargeInstances")
	public List<RecurringChargeInstance> getRecurringChargeInstances() {
		return (entity == null || entity.getId() == null) ? null
				: recurringChargeInstanceService
						.findRecurringChargeInstanceBySubscriptionId(entity
								.getId());
	}

	public List<UsageChargeInstance> getUsageChargeInstances() {
		return (entity == null || entity.getId() == null) ? null
				: usageChargeInstanceService
						.findUsageChargeInstanceBySubscriptionId(entity.getId());
	}

	public void instanciateManyServices() {
		log.debug("instanciateManyServices");
		try {
			if (quantity <= 0) {
				log.warn("instanciateManyServices quantity is negative! set it to 1");
				quantity = 1;
			}
			boolean isChecked = false;
			log.debug("serviceTemplates is " + serviceTemplates.getSize());

			log.debug("serviceTemplates is "
					+ serviceTemplates.getSelectedItemsAsList());

			for (ServiceTemplate serviceTemplate : serviceTemplates
					.getSelectedItemsAsList()) {
				isChecked = true;
				log.debug("instanciateManyServices id=#0 checked, quantity=#1",
						serviceTemplate.getId(), quantity);
				ServiceInstance serviceInstance = new ServiceInstance();
				serviceInstance.setProvider(serviceTemplate.getProvider());
				serviceInstance.setCode(serviceTemplate.getCode());
				serviceInstance
						.setDescription(serviceTemplate.getDescription());
				serviceInstance.setServiceTemplate(serviceTemplate);
				serviceInstance.setSubscription((Subscription) entity);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);

				serviceInstance.setSubscriptionDate(calendar.getTime());
				serviceInstance.setQuantity(quantity);
				serviceInstanceService.serviceInstanciation(serviceInstance,
						getCurrentUser());
				serviceInstances.add(serviceInstance);
				serviceTemplates.remove(serviceTemplate);
			}
			if (!isChecked) {
				Messages.createWarn(
						"instanciation.selectService");
			} else {
				Messages.createInfo(
						"instanciation.instanciateSuccessful");
			}
		} catch (BusinessException e1) {
			Messages.createError(e1.getMessage());
		} catch (Exception e) {
			log.error("error in SubscriptionBean.instanciateManyServices", e);
			Messages.createError(e.getMessage());
		}
	}

	public void activateService() {
		log.debug("activateService...");
		try {
			log.debug("activateService id={} checked",
					selectedServiceInstance.getId());
			if (selectedServiceInstance != null) {
				log.debug(
						"activateService:serviceInstance.getRecurrringChargeInstances.size=#0",
						selectedServiceInstance.getRecurringChargeInstances()
								.size());

				if (selectedServiceInstance.getStatus() == InstanceStatusEnum.TERMINATED) {
					Messages.createInfo(
							"error.activation.terminatedService");
					return;
				}
				if (selectedServiceInstance.getStatus() == InstanceStatusEnum.ACTIVE) {
					Messages.createInfo(
							"error.activation.activeService");
					return;
				}

				serviceInstanceService.serviceActivation(
						selectedServiceInstance, null, null, getCurrentUser());
			} else {
				log.error("activateService id=#0 is NOT a serviceInstance");
			}
			selectedServiceInstance = null;
			Messages.createInfo(
					"activation.activateSuccessful");

		} catch (BusinessException e1) {
			Messages.createError(e1.getMessage());
		} catch (Exception e) {
			log.error("unexpected exception when activating service!", e);
			Messages.createError(e.getMessage());
		}
	}

	public void terminateService() {
		try {
			Date terminationDate = selectedServiceInstance.getTerminationDate();

			SubscriptionTerminationReason newSubscriptionTerminationReason = selectedServiceInstance
					.getSubscriptionTerminationReason();
			log.debug(
					"selected subscriptionTerminationReason={},terminationDate={},selectedServiceInstanceId={},status={}",
					new Object[] {
							newSubscriptionTerminationReason != null ? newSubscriptionTerminationReason
									.getId() : null, terminationDate,
							selectedServiceInstance.getId(),
							selectedServiceInstance.getStatus() });

			if (selectedServiceInstance.getStatus() != InstanceStatusEnum.TERMINATED) {
				serviceInstanceService.terminateService(
						selectedServiceInstance, terminationDate,
						newSubscriptionTerminationReason, getCurrentUser());
			} else {
				serviceInstanceService.updateTerminationMode(
						selectedServiceInstance, terminationDate,
						getCurrentUser());
			}

			selectedServiceInstance = null;
			Messages.createInfo(
					"resiliation.resiliateSuccessful");

		} catch (BusinessException e1) {
			Messages.createError(e1.getMessage());
		} catch (Exception e) {
			log.error("unexpected exception when terminating service!", e);
			Messages.createError(e.getMessage());
		}
	}

	public void cancelService() {
		try {

			if (selectedServiceInstance.getStatus() != InstanceStatusEnum.ACTIVE) {
				Messages.createError(
						"error.termination.inactiveService");
				return;
			}
			// serviceInstanceService.cancelService(selectedServiceInstance,
			// getCurrentUser());

			selectedServiceInstance = null;
			Messages.createInfo(
					"cancellation.cancelSuccessful");

		} catch (Exception e) {
			log.error("unexpected exception when canceling service!", e);
			Messages.createError(e.getMessage());
		}
	}

	public void suspendService() {
		try {
			serviceInstanceService.serviceSuspension(selectedServiceInstance,
					new Date(), getCurrentUser());

			selectedServiceInstance = null;
			Messages.createInfo(
					"suspension.suspendSuccessful");

		} catch (BusinessException e1) {
			Messages.createError(e1.getMessage());
		} catch (Exception e) {
			log.error("unexpected exception when suspending service!", e);
			Messages.createError(e.getMessage());
		}
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getOneShotChargeInstanceQuantity() {
		return oneShotChargeInstanceQuantity;
	}

	public void setOneShotChargeInstanceQuantity(
			Integer oneShotChargeInstanceQuantity) {
		this.oneShotChargeInstanceQuantity = oneShotChargeInstanceQuantity;
	}

	public ServiceInstance getSelectedServiceInstance() {
		return selectedServiceInstance;
	}

	public void setSelectedServiceInstance(
			ServiceInstance selectedServiceInstance) {
		this.selectedServiceInstance = selectedServiceInstance;
	}

	public void populateAccounts(UserAccount userAccount) {
		entity.setUserAccount(userAccount);
		if (subscriptionService.isDuplicationExist(entity)) {
			entity.setDefaultLevel(false);
		} else {
			entity.setDefaultLevel(true);
		}
		if (userAccount != null && userAccount.getProvider() != null
				&& userAccount.getProvider().isLevelDuplication()) {
			entity.setCode(userAccount.getCode());
			entity.setDescription(userAccount.getDescription());
		}
	}

	public Integer getRecurringChargeServiceInstanceQuantity() {
		return recurringChargeServiceInstanceQuantity;
	}

	public void setRecurringChargeServiceInstanceQuantity(
			Integer recurringChargeServiceInstanceQuantity) {
		this.recurringChargeServiceInstanceQuantity = recurringChargeServiceInstanceQuantity;
	}

	private Long getUserAccountId() {
		return userAccountId;
	}

	public void setUserAccountId(Long userAccountId) {
		this.userAccountId = userAccountId;
	}

	public String getDate() {
		return (new Date()).toString();
	}

	public List<Access> getAccess() {
		List<Access> accessList = new ArrayList<Access>();
		getPersistenceService().refresh(entity);
		for (Access access : entity.getAccessPoints()) {
			accessList.add(access);
		}
		return accessList;
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}
}