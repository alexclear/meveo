package org.meveo.api.billing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.billing.FindWalletOperationsDto;
import org.meveo.api.dto.billing.WalletBalanceDto;
import org.meveo.api.dto.billing.WalletOperationDto;
import org.meveo.api.dto.billing.WalletReservationDto;
import org.meveo.api.dto.billing.WalletTemplateDto;
import org.meveo.api.dto.response.billing.FindWalletOperationsResponseDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidEnumValue;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.Currency;
import org.meveo.model.admin.Seller;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingWalletTypeEnum;
import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.billing.OperationTypeEnum;
import org.meveo.model.billing.Reservation;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.model.billing.WalletOperation;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.catalog.Calendar;
import org.meveo.model.catalog.WalletTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.service.admin.impl.CurrencyService;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.billing.impl.ChargeInstanceService;
import org.meveo.service.billing.impl.ReservationService;
import org.meveo.service.billing.impl.SubscriptionService;
import org.meveo.service.billing.impl.UserAccountService;
import org.meveo.service.billing.impl.WalletOperationService;
import org.meveo.service.billing.impl.WalletReservationService;
import org.meveo.service.billing.impl.WalletService;
import org.meveo.service.billing.impl.WalletTemplateService;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
public class WalletApi extends BaseApi {

	@SuppressWarnings("rawtypes")
	@Inject
	private ChargeInstanceService chargeInstanceService;

	@Inject
	private WalletTemplateService walletTemplateService;

	@Inject
	private WalletService walletService;

	@Inject
	private WalletReservationService walletReservationService;

	@Inject
	private ReservationService reservationService;

	@Inject
	private WalletOperationService walletOperationService;

	@Inject
	private SubscriptionService subscriptionService;

	@Inject
	private UserAccountService userAccountService;

	@Inject
	private SellerService sellerService;

	@Inject
	private CurrencyService currencyService;

	public BigDecimal getCurrentAmount(WalletBalanceDto walletBalance, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletBalance.getSellerCode())
				&& !StringUtils.isBlank(walletBalance.getUserAccountCode())) {
			try {
				if (walletBalance.isAmountWithTax()) {
					return walletReservationService.getCurrentBalanceWithTax(provider, walletBalance.getSellerCode(),
							walletBalance.getUserAccountCode(), walletBalance.getStartDate(),
							walletBalance.getEndDate());
				} else {
					return walletReservationService.getCurrentBalanceWithoutTax(provider,
							walletBalance.getSellerCode(), walletBalance.getUserAccountCode(),
							walletBalance.getStartDate(), walletBalance.getEndDate());
				}
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}
		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();
			if (StringUtils.isBlank(walletBalance.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletBalance.getUserAccountCode())) {
				missingFields.add("userAccountCode");
			}
			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public BigDecimal getReservedAmount(WalletBalanceDto walletBalance, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletBalance.getSellerCode())
				&& !StringUtils.isBlank(walletBalance.getUserAccountCode())) {
			try {
				if (walletBalance.isAmountWithTax()) {
					return walletReservationService.getReservedBalanceWithTax(provider, walletBalance.getSellerCode(),
							walletBalance.getUserAccountCode(), walletBalance.getStartDate(),
							walletBalance.getEndDate());
				} else {
					return walletReservationService.getReservedBalanceWithoutTax(provider,
							walletBalance.getSellerCode(), walletBalance.getUserAccountCode(),
							walletBalance.getStartDate(), walletBalance.getEndDate());
				}
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}
		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();
			if (StringUtils.isBlank(walletBalance.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletBalance.getUserAccountCode())) {
				missingFields.add("userAccountCode");
			}
			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public BigDecimal getOpenAmount(WalletBalanceDto walletBalance, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletBalance.getSellerCode())
				&& !StringUtils.isBlank(walletBalance.getUserAccountCode())) {

			try {
				if (walletBalance.isAmountWithTax()) {
					return walletReservationService.getOpenBalanceWithoutTax(provider, walletBalance.getSellerCode(),
							walletBalance.getUserAccountCode(), walletBalance.getStartDate(),
							walletBalance.getEndDate());
				} else {
					return walletReservationService.getOpenBalanceWithTax(provider, walletBalance.getSellerCode(),
							walletBalance.getUserAccountCode(), walletBalance.getStartDate(),
							walletBalance.getEndDate());
				}
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}
		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(walletBalance.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletBalance.getUserAccountCode())) {
				missingFields.add("userAccountCode");
			}
			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public Long createReservation(WalletReservationDto walletReservation, Provider provider) throws MeveoApiException {

		if (!StringUtils.isBlank(walletReservation.getSellerCode())
				&& !StringUtils.isBlank(walletReservation.getOfferCode())
				&& !StringUtils.isBlank(walletReservation.getUserAccountCode())
				&& walletReservation.getSubscriptionDate() != null && walletReservation.getCreditLimit() != null) {

			try {
				return reservationService.createReservation(provider, walletReservation.getSellerCode(),
						walletReservation.getOfferCode(), walletReservation.getUserAccountCode(),
						walletReservation.getSubscriptionDate(), walletReservation.getExpirationDate(),
						walletReservation.getCreditLimit(), walletReservation.getParam1(),
						walletReservation.getParam2(), walletReservation.getParam3(),
						walletReservation.isAmountWithTax());
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}
		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (StringUtils.isBlank(walletReservation.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletReservation.getOfferCode())) {
				missingFields.add("offerCode");
			}
			if (StringUtils.isBlank(walletReservation.getUserAccountCode())) {
				missingFields.add("userAccountCode");
			}
			if (walletReservation.getSubscriptionDate() == null) {
				missingFields.add("subscriptionDate");
			}
			if (walletReservation.getCreditLimit() == null) {
				missingFields.add("creditLimit");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public void updateReservation(WalletReservationDto walletReservation, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletReservation.getSellerCode())
				&& !StringUtils.isBlank(walletReservation.getOfferCode())
				&& !StringUtils.isBlank(walletReservation.getUserAccountCode())
				&& walletReservation.getSubscriptionDate() != null && walletReservation.getCreditLimit() != null) {

			try {
				reservationService.updateReservation(walletReservation.getReservationId(), provider,
						walletReservation.getSellerCode(), walletReservation.getOfferCode(),
						walletReservation.getUserAccountCode(), walletReservation.getSubscriptionDate(),
						walletReservation.getExpirationDate(), walletReservation.getCreditLimit(),
						walletReservation.getParam1(), walletReservation.getParam2(), walletReservation.getParam3(),
						walletReservation.isAmountWithTax());
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (walletReservation.getReservationId() == null) {
				missingFields.add("reservationId");
			}
			if (StringUtils.isBlank(walletReservation.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletReservation.getOfferCode())) {
				missingFields.add("offerCode");
			}
			if (StringUtils.isBlank(walletReservation.getUserAccountCode())) {
				missingFields.add("userAccountCode");
			}
			if (walletReservation.getSubscriptionDate() == null) {
				missingFields.add("subscriptionDate");
			}
			if (walletReservation.getCreditLimit() == null) {
				missingFields.add("creditLimit");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public void cancelReservation(Long reservationId, Provider provider) throws MeveoApiException {
		Reservation reservation = reservationService.findById(reservationId, provider);
		if (reservation == null) {
			throw new MeveoApiException("Reservation with id=" + reservationId + " does not exists.");
		} else if (!reservation.getProvider().getCode().equals(provider.getCode())) {
			throw new MeveoApiException("Reservation with id=" + reservationId
					+ " does not belong to current provider.");
		}

		try {
			reservationService.cancelReservation(reservation);
		} catch (BusinessException e) {
			throw new MeveoApiException(e.getMessage());
		}
	}

	public BigDecimal confirmReservation(WalletReservationDto walletReservation, Provider provider)
			throws MeveoApiException {
		if (walletReservation.getReservationId() != null && !StringUtils.isBlank(walletReservation.getSellerCode())
				&& !StringUtils.isBlank(walletReservation.getOfferCode())
				&& walletReservation.getSubscriptionDate() != null && walletReservation.getCreditLimit() != null) {

			try {
				return reservationService.confirmReservation(walletReservation.getReservationId(), provider,
						walletReservation.getSellerCode(), walletReservation.getOfferCode(),
						walletReservation.getSubscriptionDate(), walletReservation.getTerminationDate(),
						walletReservation.getParam1(), walletReservation.getParam2(), walletReservation.getParam3());
			} catch (BusinessException e) {
				throw new MeveoApiException(e.getMessage());
			}

		} else {
			StringBuilder sb = new StringBuilder("The following parameters are required ");
			List<String> missingFields = new ArrayList<String>();

			if (walletReservation.getReservationId() == null) {
				missingFields.add("reservationId");
			}
			if (StringUtils.isBlank(walletReservation.getSellerCode())) {
				missingFields.add("sellerCode");
			}
			if (StringUtils.isBlank(walletReservation.getOfferCode())) {
				missingFields.add("offerCode");
			}
			if (walletReservation.getSubscriptionDate() == null) {
				missingFields.add("subscriptionDate");
			}
			if (walletReservation.getCreditLimit() == null) {
				missingFields.add("creditLimit");
			}

			if (missingFields.size() > 1) {
				sb.append(org.apache.commons.lang.StringUtils.join(missingFields.toArray(), ", "));
			} else {
				sb.append(missingFields.get(0));
			}
			sb.append(".");

			throw new MissingParameterException(sb.toString());
		}
	}

	public void createOperation(WalletOperationDto postData, User currentUser) throws MeveoApiException {
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())
				&& !StringUtils.isBlank(postData.getUserAccount())
				&& !StringUtils.isBlank(postData.getChargeInstance())
				&& !StringUtils.isBlank(postData.getSubscription()) && !StringUtils.isBlank(postData.getSeller())) {
			Provider provider = currentUser.getProvider();

			UserAccount userAccount = userAccountService.findByCode(postData.getUserAccount(), provider);
			if (userAccount == null) {
				throw new EntityDoesNotExistsException(UserAccount.class, postData.getUserAccount());
			}

			if (walletOperationService.findByUserAccountAndCode(postData.getCode(), userAccount, provider) != null) {
				throw new EntityAlreadyExistsException(WalletOperation.class, postData.getCode());
			}

			Subscription subscription = subscriptionService.findByCode(postData.getSubscription(), provider);
			if (subscription == null) {
				throw new EntityDoesNotExistsException(Subscription.class, postData.getSubscription());
			}

			WalletTemplate walletTemplate = null;
			if (!StringUtils.isBlank(postData.getWalletTemplate())) {
				if (!postData.getWalletTemplate().equals(WalletTemplate.PRINCIPAL)) {
					walletTemplate = walletTemplateService.findByCode(postData.getWalletTemplate(), provider);
					if (walletTemplate == null) {
						throw new EntityDoesNotExistsException(WalletTemplate.class, postData.getWalletTemplate());
					}
				} else {
					walletTemplate = new WalletTemplate();
					walletTemplate.setCode(WalletTemplate.PRINCIPAL);
				}
			} else {
				walletTemplate = new WalletTemplate();
				walletTemplate.setCode(WalletTemplate.PRINCIPAL);
			}

			WalletInstance walletInstance = walletService.getWalletInstance(userAccount, walletTemplate, currentUser,
					provider);
			if (walletInstance == null) {
				throw new EntityDoesNotExistsException(WalletInstance.class, walletTemplate.getCode());
			}

			Seller seller = sellerService.findByCode(postData.getSeller(), provider);
			if (seller == null) {
				throw new EntityDoesNotExistsException(Seller.class, postData.getSeller());
			}

			Currency currency = currencyService.findByCode(postData.getCurrency());
			if (currency == null) {
				throw new EntityDoesNotExistsException(Currency.class, postData.getCurrency());
			}

			ChargeInstance chargeInstance = chargeInstanceService.findByCodeAndSubscription(
					postData.getChargeInstance(), subscription, provider);
			if (chargeInstance == null) {
				throw new EntityDoesNotExistsException(ChargeInstance.class, postData.getChargeInstance());
			}

			WalletOperation walletOperation = new WalletOperation();
			walletOperation.setDescription(postData.getDescription());
			walletOperation.setProvider(provider);
			walletOperation.setCode(postData.getCode());
			walletOperation.setOfferCode(subscription.getOffer().getCode());
			walletOperation.setSeller(seller);
			walletOperation.setCurrency(currency);
			walletOperation.setWallet(walletInstance);
			walletOperation.setChargeInstance(chargeInstance);

			try {
				walletOperation.setType(OperationTypeEnum.valueOf(postData.getType()));
			} catch (IllegalArgumentException e) {
				log.warn("error in type={}", e);
			}
			try {
				walletOperation.setStatus(WalletOperationStatusEnum.valueOf(postData.getStatus()));
			} catch (IllegalArgumentException e) {
				log.warn("error in status={}", e);
			}
			walletOperation.setCounter(null);
			walletOperation.setRatingUnitDescription(postData.getRatingUnitDescription());
			walletOperation.setTaxPercent(postData.getTaxPercent());
			walletOperation.setUnitAmountTax(postData.getUnitAmountTax());
			walletOperation.setUnitAmountWithoutTax(postData.getUnitAmountWithoutTax());
			walletOperation.setUnitAmountWithTax(postData.getUnitAmountWithTax());
			walletOperation.setQuantity(postData.getQuantity());
			walletOperation.setAmountTax(postData.getAmountTax());
			walletOperation.setAmountWithoutTax(postData.getAmountWithoutTax());
			walletOperation.setAmountWithTax(postData.getAmountWithTax());
			walletOperation.setParameter1(postData.getParameter1());
			walletOperation.setParameter2(postData.getParameter2());
			walletOperation.setParameter3(postData.getParameter3());
			walletOperation.setStartDate(postData.getStartDate());
			walletOperation.setEndDate(postData.getEndDate());
			walletOperation.setSubscriptionDate(postData.getSubscriptionDate());
			walletOperation.setOperationDate(postData.getOperationDate() == null ? new Date() : postData
					.getOperationDate());
			if (chargeInstance.getInvoicingCalendar() != null) {
				Calendar cal = chargeInstance.getInvoicingCalendar();
				cal.setInitDate(postData.getSubscriptionDate());
				walletOperation.setInvoicingDate(cal.nextCalendarDate(walletOperation.getOperationDate()));
			}

			walletOperationService.create(walletOperation, currentUser, provider);
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			if (StringUtils.isBlank(postData.getDescription())) {
				missingParameters.add("description");
			}
			if (StringUtils.isBlank(postData.getUserAccount())) {
				missingParameters.add("userAccount");
			}
			if (StringUtils.isBlank(postData.getSubscription())) {
				missingParameters.add("subscription");
			}
			if (StringUtils.isBlank(postData.getChargeInstance())) {
				missingParameters.add("chargeInstance");
			}
			if (StringUtils.isBlank(postData.getSeller())) {
				missingParameters.add("seller");
			}

			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
	}

	public FindWalletOperationsResponseDto findOperations(FindWalletOperationsDto postData, Provider provider)
			throws MeveoApiException {
		FindWalletOperationsResponseDto result = new FindWalletOperationsResponseDto();

		if (!StringUtils.isBlank(postData.getUserAccount())) {
			WalletOperationStatusEnum status = null;
			WalletTemplate walletTemplate = null;
			WalletInstance walletInstance = null;
			UserAccount userAccount = null;

			userAccount = userAccountService.findByCode(postData.getUserAccount(), provider);
			if (userAccount == null) {
				throw new EntityDoesNotExistsException(UserAccount.class, postData.getUserAccount());
			}

			if (!StringUtils.isBlank(postData.getWalletTemplate())
					&& !postData.getWalletTemplate().equals(WalletTemplate.PRINCIPAL)) {
				walletTemplate = walletTemplateService.findByCode(postData.getWalletTemplate(), provider);
				if (walletTemplate == null) {
					throw new EntityDoesNotExistsException(WalletTemplate.class, postData.getWalletTemplate());
				}
			} else {
				walletInstance = walletService.findByUserAccountAndCode(userAccount, WalletTemplate.PRINCIPAL);
			}

			if (!StringUtils.isBlank(postData.getStatus())) {
				try {
					status = WalletOperationStatusEnum.valueOf(postData.getStatus());
				} catch (IllegalArgumentException e) {
					log.warn("enum: {}", e);
				}
			}

			List<WalletOperation> walletOperations = walletOperationService.findWalletOperation(status, walletTemplate,
					walletInstance, userAccount, Arrays.asList("wallet"), provider, 1000);

			for (WalletOperation wo : walletOperations) {
				result.getWalletOperations().add(new WalletOperationDto(wo));
			}

			return result;
		} else {
			missingParameters.add("userAccount");

			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
	}

	public void create(WalletTemplateDto postData, User currentUser) throws MeveoApiException {
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())) {
			if (walletTemplateService.findByCode(postData.getCode(), currentUser.getProvider()) != null) {
				throw new EntityAlreadyExistsException(WalletTemplate.class, postData.getCode());
			}

			WalletTemplate wt = new WalletTemplate();
			wt.setCode(postData.getCode());
			wt.setDescription(postData.getDescription());
			try {
				wt.setWalletType(BillingWalletTypeEnum.valueOf(postData.getWalletType()));
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(BillingWalletTypeEnum.class.getName(), postData.getWalletType());
			}
			wt.setConsumptionAlertSet(postData.isConsumptionAlertSet());
			wt.setFastRatingLevel(postData.getFastRatingLevel());
			wt.setLowBalanceLevel(postData.getLowBalanceLevel());

			walletTemplateService.create(wt, currentUser, currentUser.getProvider());
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			if (StringUtils.isBlank(postData.getDescription())) {
				missingParameters.add("description");
			}

			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
	}

	public void update(WalletTemplateDto postData, User currentUser) throws MeveoApiException {
		if (!StringUtils.isBlank(postData.getCode()) && !StringUtils.isBlank(postData.getDescription())) {
			WalletTemplate wt = walletTemplateService.findByCode(postData.getCode(), currentUser.getProvider());
			if (wt == null) {
				throw new EntityDoesNotExistsException(WalletTemplate.class, postData.getCode());
			}

			wt.setDescription(postData.getDescription());
			try {
				wt.setWalletType(BillingWalletTypeEnum.valueOf(postData.getWalletType()));
			} catch (IllegalArgumentException e) {
				throw new InvalidEnumValue(BillingWalletTypeEnum.class.getName(), postData.getWalletType());
			}
			wt.setConsumptionAlertSet(postData.isConsumptionAlertSet());
			wt.setFastRatingLevel(postData.getFastRatingLevel());
			wt.setLowBalanceLevel(postData.getLowBalanceLevel());

			walletTemplateService.update(wt, currentUser);
		} else {
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("code");
			}
			if (StringUtils.isBlank(postData.getDescription())) {
				missingParameters.add("description");
			}

			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
	}

	public WalletTemplateDto find(String walletTemplateCode, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletTemplateCode)) {
			WalletTemplate wt = walletTemplateService.findByCode(walletTemplateCode, provider);
			if (wt == null) {
				throw new EntityDoesNotExistsException(WalletTemplate.class, walletTemplateCode);
			}

			return new WalletTemplateDto(wt);
		} else {
			missingParameters.add("walletTemplateCode");
		}

		return null;
	}

	public void remove(String walletTemplateCode, Provider provider) throws MeveoApiException {
		if (!StringUtils.isBlank(walletTemplateCode)) {
			WalletTemplate wt = walletTemplateService.findByCode(walletTemplateCode, provider);
			if (wt == null) {
				throw new EntityDoesNotExistsException(WalletTemplate.class, walletTemplateCode);
			}

			walletTemplateService.remove(wt);
		} else {
			missingParameters.add("walletTemplateCode");
		}
	}
	
	/**
	 * Create or update walletTemplate
	 * @param postData
	 * @param currentUser
	 * @throws MeveoApiException
	 */
	public void createOrUpdate(WalletTemplateDto postData, User currentUser) throws MeveoApiException {
		if (walletTemplateService.findByCode(postData.getCode(), currentUser.getProvider()) == null) {
			create(postData, currentUser);
		} else {
			update(postData, currentUser);
		}
	}
}
