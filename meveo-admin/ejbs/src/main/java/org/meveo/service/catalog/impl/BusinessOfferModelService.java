package org.meveo.service.catalog.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.MeveoApiErrorCodeEnum;
import org.meveo.api.dto.catalog.ServiceCodeDto;
import org.meveo.model.admin.User;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.catalog.BusinessOfferModel;
import org.meveo.model.catalog.BusinessServiceModel;
import org.meveo.model.catalog.ChargeTemplate;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.PricePlanMatrix;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceChargeTemplateRecurring;
import org.meveo.model.catalog.ServiceChargeTemplateSubscription;
import org.meveo.model.catalog.ServiceChargeTemplateTermination;
import org.meveo.model.catalog.ServiceChargeTemplateUsage;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.TriggeredEDRTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.model.module.MeveoModuleItem;
import org.meveo.service.base.BusinessService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class BusinessOfferModelService extends BusinessService<BusinessOfferModel> {

	@Inject
	private BusinessServiceModelService businessServiceModelService;

	@Inject
	private PricePlanMatrixService pricePlanMatrixService;

	@Inject
	private OfferServiceTemplateService offerServiceTemplateService;

	@Inject
	private ServiceTemplateService serviceTemplateService;

	@Inject
	private ServiceChargeTemplateSubscriptionService serviceChargeTemplateSubscriptionService;

	@Inject
	private ServiceChargeTemplateTerminationService serviceChargeTemplateTerminationService;

	@Inject
	private ServiceChargeTemplateRecurringService serviceChargeTemplateRecurringService;

	@Inject
	private ServiceChargeTemplateUsageService serviceChargeTemplateUsageService;

	@Inject
	private RecurringChargeTemplateService recurringChargeTemplateService;

	@Inject
	private UsageChargeTemplateService usageChargeTemplateService;

	@Inject
	private OneShotChargeTemplateService oneShotChargeTemplateService;

	@Inject
	private CounterTemplateService<CounterTemplate> counterTemplateService;

	@Inject
	private OfferTemplateService offerTemplateService;

	public OfferTemplate createOfferFromBOM(BusinessOfferModel businessOfferModel, String prefix, String offerDescription, List<ServiceCodeDto> serviceCodes, User currentUser)
			throws BusinessException {
		OfferTemplate bomOffer = businessOfferModel.getOfferTemplate();

		// 1 create offer
		OfferTemplate newOfferTemplate = new OfferTemplate();

		// check if offer already exists
		if (offerTemplateService.findByCode(prefix + bomOffer.getCode(), currentUser.getProvider()) != null) {
			throw new BusinessException("" + MeveoApiErrorCodeEnum.ENTITY_ALREADY_EXISTS_EXCEPTION);
		}

		newOfferTemplate.setCode(prefix);
		newOfferTemplate.setDescription(offerDescription);

		newOfferTemplate.setBusinessOfferModel(businessOfferModel);

		offerTemplateService.create(newOfferTemplate, currentUser);

		prefix = prefix + "_";

		List<OfferServiceTemplate> newOfferServiceTemplates = new ArrayList<>();
		// 2 create services
		if (bomOffer.getOfferServiceTemplates() != null) {
			// check if service template exists
			if (serviceCodes != null && serviceCodes.size() > 0) {
				boolean serviceFound = false;
				for (ServiceCodeDto serviceCodeDto : serviceCodes) {
					String serviceCode = serviceCodeDto.getCode();

					for (OfferServiceTemplate offerServiceTemplate : bomOffer.getOfferServiceTemplates()) {
						ServiceTemplate serviceTemplate = offerServiceTemplate.getServiceTemplate();
						if (serviceCode.equals(serviceTemplate.getCode())) {
							serviceFound = true;
							break;
						}
					}

					if (!serviceFound) {
						throw new BusinessException("Service " + serviceCode + " is not defined in the offer");
					}
				}
			}

			List<PricePlanMatrix> pricePlansInMemory = new ArrayList<>();
			List<ChargeTemplate> chargeTemplateInMemory = new ArrayList<>();
			for (OfferServiceTemplate offerServiceTemplate : bomOffer.getOfferServiceTemplates()) {
				offerServiceTemplate = offerServiceTemplateService.refreshOrRetrieve(offerServiceTemplate);
				ServiceTemplate serviceTemplate = serviceTemplateService.findByCode(offerServiceTemplate.getServiceTemplate().getCode(), currentUser.getProvider());

				boolean serviceFound = false;
				ServiceCodeDto serviceCodeDto = new ServiceCodeDto();
				for (ServiceCodeDto tempServiceCodeDto : serviceCodes) {
					String serviceCode = tempServiceCodeDto.getCode();
					if (serviceCode.equals(serviceTemplate.getCode())) {
						serviceCodeDto = tempServiceCodeDto;
						serviceFound = true;
					}
				}
				if (!serviceFound) {
					continue;
				}

				// get the BSM from BOM
				BusinessServiceModel bsm = null;
				for (MeveoModuleItem item : businessOfferModel.getModuleItems()) {
					if (item.getItemClass().equals(BusinessServiceModel.class.getName())) {
						bsm = businessServiceModelService.findByCode(item.getItemCode(), currentUser.getProvider());
						if (bsm.getServiceTemplate().equals(serviceTemplate)) {
							break;
						}
					}
				}

				OfferServiceTemplate newOfferServiceTemplate = new OfferServiceTemplate();
				newOfferServiceTemplate.setMandatory(offerServiceTemplate.isMandatory());
				if (offerServiceTemplate.getIncompatibleServices() != null) {
					newOfferServiceTemplate.getIncompatibleServices().addAll(offerServiceTemplate.getIncompatibleServices());
				}
				ServiceTemplate newServiceTemplate = new ServiceTemplate();

				try {
					BeanUtils.copyProperties(newServiceTemplate, serviceTemplate);
					newServiceTemplate.setCode(prefix + serviceTemplate.getCode());
					newServiceTemplate.setDescription(serviceCodeDto.getDescription());
					newServiceTemplate.setBusinessServiceModel(bsm);
					newServiceTemplate.setAuditable(null);
					newServiceTemplate.setId(null);
					newServiceTemplate.clearUuid();
					newServiceTemplate.setVersion(0);
					newServiceTemplate.setServiceRecurringCharges(new ArrayList<ServiceChargeTemplateRecurring>());
					newServiceTemplate.setServiceTerminationCharges(new ArrayList<ServiceChargeTemplateTermination>());
					newServiceTemplate.setServiceSubscriptionCharges(new ArrayList<ServiceChargeTemplateSubscription>());
					newServiceTemplate.setServiceUsageCharges(new ArrayList<ServiceChargeTemplateUsage>());
					newOfferServiceTemplate.setServiceTemplate(newServiceTemplate);

					serviceTemplateService.create(newOfferServiceTemplate.getServiceTemplate(), currentUser);

					// create price plans
					if (serviceTemplate.getServiceRecurringCharges() != null && serviceTemplate.getServiceRecurringCharges().size() > 0) {
						for (ServiceChargeTemplateRecurring serviceCharge : serviceTemplate.getServiceRecurringCharges()) {
							// create price plan
							String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
							List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
							if (pricePlanMatrixes != null) {
								for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
									String ppCode = prefix + pricePlanMatrix.getCode();
									PricePlanMatrix ppMatrix = pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider());
									if (ppMatrix != null) {
										continue;
									}

									PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
									BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
									newPriceplanmaMatrix.setAuditable(null);
									newPriceplanmaMatrix.setId(null);
									newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
									newPriceplanmaMatrix.setCode(ppCode);
									newPriceplanmaMatrix.setVersion(0);
									newPriceplanmaMatrix.setOfferTemplate(null);

									if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
										continue;
									} else {
										pricePlansInMemory.add(newPriceplanmaMatrix);
									}

									pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
								}
							}
						}
					}

					if (serviceTemplate.getServiceSubscriptionCharges() != null && serviceTemplate.getServiceSubscriptionCharges().size() > 0) {
						for (ServiceChargeTemplateSubscription serviceCharge : serviceTemplate.getServiceSubscriptionCharges()) {
							// create price plan
							String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
							List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
							if (pricePlanMatrixes != null) {
								for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
									String ppCode = prefix + pricePlanMatrix.getCode();
									if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
										continue;
									}

									PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
									BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
									newPriceplanmaMatrix.setAuditable(null);
									newPriceplanmaMatrix.setId(null);
									newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
									newPriceplanmaMatrix.setCode(ppCode);
									newPriceplanmaMatrix.setVersion(0);
									newPriceplanmaMatrix.setOfferTemplate(null);

									if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
										continue;
									} else {
										pricePlansInMemory.add(newPriceplanmaMatrix);
									}

									pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
								}
							}
						}
					}

					if (serviceTemplate.getServiceTerminationCharges() != null && serviceTemplate.getServiceTerminationCharges().size() > 0) {
						for (ServiceChargeTemplateTermination serviceCharge : serviceTemplate.getServiceTerminationCharges()) {
							// create price plan
							String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
							List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
							if (pricePlanMatrixes != null) {
								for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
									String ppCode = prefix + pricePlanMatrix.getCode();
									if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
										continue;
									}

									PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
									BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
									newPriceplanmaMatrix.setAuditable(null);
									newPriceplanmaMatrix.setId(null);
									newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
									newPriceplanmaMatrix.setCode(ppCode);
									newPriceplanmaMatrix.setVersion(0);
									newPriceplanmaMatrix.setOfferTemplate(null);

									if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
										continue;
									} else {
										pricePlansInMemory.add(newPriceplanmaMatrix);
									}

									pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
								}
							}
						}
					}

					if (serviceTemplate.getServiceUsageCharges() != null && serviceTemplate.getServiceUsageCharges().size() > 0) {
						for (ServiceChargeTemplateUsage serviceCharge : serviceTemplate.getServiceUsageCharges()) {
							String chargeTemplateCode = serviceCharge.getChargeTemplate().getCode();
							List<PricePlanMatrix> pricePlanMatrixes = pricePlanMatrixService.listByEventCode(chargeTemplateCode, currentUser.getProvider());
							if (pricePlanMatrixes != null) {
								for (PricePlanMatrix pricePlanMatrix : pricePlanMatrixes) {
									String ppCode = prefix + pricePlanMatrix.getCode();
									if (pricePlanMatrixService.findByCode(ppCode, currentUser.getProvider()) != null) {
										continue;
									}

									PricePlanMatrix newPriceplanmaMatrix = new PricePlanMatrix();
									BeanUtils.copyProperties(newPriceplanmaMatrix, pricePlanMatrix);
									newPriceplanmaMatrix.setAuditable(null);
									newPriceplanmaMatrix.setId(null);
									newPriceplanmaMatrix.setEventCode(prefix + chargeTemplateCode);
									newPriceplanmaMatrix.setCode(ppCode);
									newPriceplanmaMatrix.setVersion(0);
									newPriceplanmaMatrix.setOfferTemplate(null);

									if (pricePlansInMemory.contains(newPriceplanmaMatrix)) {
										continue;
									} else {
										pricePlansInMemory.add(newPriceplanmaMatrix);
									}

									pricePlanMatrixService.create(newPriceplanmaMatrix, currentUser);
								}
							}
						}
					}

					// get charges
					if (serviceTemplate.getServiceRecurringCharges() != null && serviceTemplate.getServiceRecurringCharges().size() > 0) {
						for (ServiceChargeTemplateRecurring serviceCharge : serviceTemplate.getServiceRecurringCharges()) {
							RecurringChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
							RecurringChargeTemplate newChargeTemplate = new RecurringChargeTemplate();

							BeanUtils.copyProperties(newChargeTemplate, chargeTemplate);
							newChargeTemplate.setAuditable(null);
							newChargeTemplate.setId(null);
							newChargeTemplate.setCode(prefix + chargeTemplate.getCode());
							newChargeTemplate.clearUuid();
							newChargeTemplate.setVersion(0);
							newChargeTemplate.setChargeInstances(new ArrayList<ChargeInstance>());
							newChargeTemplate.setEdrTemplates(new ArrayList<TriggeredEDRTemplate>());

							if (chargeTemplateInMemory.contains(newChargeTemplate)) {
								continue;
							} else {
								chargeTemplateInMemory.add(newChargeTemplate);
							}

							recurringChargeTemplateService.create(newChargeTemplate, currentUser);

							ServiceChargeTemplateRecurring serviceChargeTemplate = new ServiceChargeTemplateRecurring();
							serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
							serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
							if (serviceCharge.getWalletTemplates() != null) {
								serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
								serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
							}
							serviceChargeTemplateRecurringService.create(serviceChargeTemplate, currentUser);

							newServiceTemplate.getServiceRecurringCharges().add(serviceChargeTemplate);
						}
					}

					if (serviceTemplate.getServiceSubscriptionCharges() != null && serviceTemplate.getServiceSubscriptionCharges().size() > 0) {
						for (ServiceChargeTemplateSubscription serviceCharge : serviceTemplate.getServiceSubscriptionCharges()) {
							OneShotChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
							OneShotChargeTemplate newChargeTemplate = new OneShotChargeTemplate();

							BeanUtils.copyProperties(newChargeTemplate, chargeTemplate);
							newChargeTemplate.setAuditable(null);
							newChargeTemplate.setId(null);
							newChargeTemplate.setCode(prefix + chargeTemplate.getCode());
							newChargeTemplate.clearUuid();
							newChargeTemplate.setVersion(0);
							newChargeTemplate.setChargeInstances(new ArrayList<ChargeInstance>());
							newChargeTemplate.setEdrTemplates(new ArrayList<TriggeredEDRTemplate>());

							if (chargeTemplateInMemory.contains(newChargeTemplate)) {
								continue;
							} else {
								chargeTemplateInMemory.add(newChargeTemplate);
							}

							oneShotChargeTemplateService.create(newChargeTemplate, currentUser);

							ServiceChargeTemplateSubscription serviceChargeTemplate = new ServiceChargeTemplateSubscription();
							serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
							serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
							if (serviceCharge.getWalletTemplates() != null) {
								serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
								serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
							}
							serviceChargeTemplateSubscriptionService.create(serviceChargeTemplate, currentUser);

							newServiceTemplate.getServiceSubscriptionCharges().add(serviceChargeTemplate);
						}
					}

					if (serviceTemplate.getServiceTerminationCharges() != null && serviceTemplate.getServiceTerminationCharges().size() > 0) {
						for (ServiceChargeTemplateTermination serviceCharge : serviceTemplate.getServiceTerminationCharges()) {
							OneShotChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
							OneShotChargeTemplate newChargeTemplate = new OneShotChargeTemplate();

							BeanUtils.copyProperties(newChargeTemplate, chargeTemplate);
							newChargeTemplate.setAuditable(null);
							newChargeTemplate.setId(null);
							newChargeTemplate.setCode(prefix + chargeTemplate.getCode());
							newChargeTemplate.clearUuid();
							newChargeTemplate.setVersion(0);
							newChargeTemplate.setChargeInstances(new ArrayList<ChargeInstance>());
							newChargeTemplate.setEdrTemplates(new ArrayList<TriggeredEDRTemplate>());

							if (chargeTemplateInMemory.contains(newChargeTemplate)) {
								continue;
							} else {
								chargeTemplateInMemory.add(newChargeTemplate);
							}

							oneShotChargeTemplateService.create(newChargeTemplate, currentUser);

							ServiceChargeTemplateTermination serviceChargeTemplate = new ServiceChargeTemplateTermination();
							serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
							serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
							if (serviceCharge.getWalletTemplates() != null) {
								serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
								serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
							}
							serviceChargeTemplateTerminationService.create(serviceChargeTemplate, currentUser);

							newServiceTemplate.getServiceTerminationCharges().add(serviceChargeTemplate);
						}
					}

					if (serviceTemplate.getServiceUsageCharges() != null && serviceTemplate.getServiceUsageCharges().size() > 0) {
						for (ServiceChargeTemplateUsage serviceCharge : serviceTemplate.getServiceUsageCharges()) {
							UsageChargeTemplate chargeTemplate = serviceCharge.getChargeTemplate();
							UsageChargeTemplate newChargeTemplate = new UsageChargeTemplate();

							BeanUtils.copyProperties(newChargeTemplate, chargeTemplate);
							newChargeTemplate.setAuditable(null);
							newChargeTemplate.setId(null);
							newChargeTemplate.setCode(prefix + chargeTemplate.getCode());
							newChargeTemplate.clearUuid();
							newChargeTemplate.setVersion(0);
							newChargeTemplate.setChargeInstances(new ArrayList<ChargeInstance>());
							newChargeTemplate.setEdrTemplates(new ArrayList<TriggeredEDRTemplate>());

							if (chargeTemplateInMemory.contains(newChargeTemplate)) {
								continue;
							} else {
								chargeTemplateInMemory.add(newChargeTemplate);
							}

							usageChargeTemplateService.create(newChargeTemplate, currentUser);

							ServiceChargeTemplateUsage serviceChargeTemplate = new ServiceChargeTemplateUsage();
							serviceChargeTemplate.setChargeTemplate(newChargeTemplate);
							serviceChargeTemplate.setServiceTemplate(newServiceTemplate);
							if (serviceCharge.getWalletTemplates() != null) {
								serviceChargeTemplate.setWalletTemplates(new ArrayList<WalletTemplate>());
								serviceChargeTemplate.getWalletTemplates().addAll(serviceCharge.getWalletTemplates());
							}
							serviceChargeTemplateUsageService.create(serviceChargeTemplate, currentUser);

							if (serviceCharge.getCounterTemplate() != null) {
								CounterTemplate newCounterTemplate = new CounterTemplate();
								BeanUtils.copyProperties(newCounterTemplate, serviceCharge.getCounterTemplate());
								newCounterTemplate.setAuditable(null);
								newCounterTemplate.setId(null);
								newCounterTemplate.setCode(prefix + serviceCharge.getCounterTemplate().getCode());

								counterTemplateService.create(newCounterTemplate, currentUser);

								serviceChargeTemplate.setCounterTemplate(newCounterTemplate);
							}

							newServiceTemplate.getServiceUsageCharges().add(serviceChargeTemplate);
						}
					}

					newOfferServiceTemplate.setServiceTemplate(newServiceTemplate);
					newOfferServiceTemplates.add(newOfferServiceTemplate);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new BusinessException(e.getMessage());
				}
			}
		}

		// add to offer
		for (OfferServiceTemplate newOfferServiceTemplate : newOfferServiceTemplates) {
			newOfferServiceTemplate.setOfferTemplate(newOfferTemplate);
			offerServiceTemplateService.create(newOfferServiceTemplate, currentUser);

			newOfferTemplate.addOfferServiceTemplate(newOfferServiceTemplate);
		}

		return newOfferTemplate;
	}
}