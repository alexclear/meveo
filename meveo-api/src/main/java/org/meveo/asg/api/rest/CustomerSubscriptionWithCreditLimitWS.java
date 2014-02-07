package org.meveo.asg.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectServiceInstanceException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.api.SubscriptionApiStatusEnum;
import org.meveo.api.dto.SubscriptionWithCreditLimitDto;
import org.meveo.api.dto.SubscriptionWithCreditLimitUpdateDto;
import org.meveo.api.exception.CreditLimitExceededException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.ParentSellerDoesNotExistsException;
import org.meveo.api.exception.SellerDoesNotExistsException;
import org.meveo.api.exception.ServiceTemplateDoesNotExistsException;
import org.meveo.api.rest.response.SubscriptionWithCreditLimitResponse;
import org.meveo.asg.api.CustomerSubscriptionWithCreditLimitServiceApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Dec 17, 2013
 **/
@Path("/asg/customerSubscriptionWithCreditLimit")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class CustomerSubscriptionWithCreditLimitWS {

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private Logger log;

	@Inject
	private CustomerSubscriptionWithCreditLimitServiceApi customerSubscriptionWithCreditLimitServiceApi;

	@POST
	@Path("/")
	public SubscriptionWithCreditLimitResponse create(
			SubscriptionWithCreditLimitDto subscriptionDto) {
		SubscriptionWithCreditLimitResponse result = new SubscriptionWithCreditLimitResponse();

		subscriptionDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
				"asp.api.userId", "1")));
		subscriptionDto.setProviderId(Long.valueOf(paramBean.getProperty(
				"asp.api.providerId", "1")));

		try {
			result = customerSubscriptionWithCreditLimitServiceApi
					.create(subscriptionDto);
		} catch (CreditLimitExceededException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (SellerDoesNotExistsException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (ParentSellerDoesNotExistsException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (ServiceTemplateDoesNotExistsException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (BusinessException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			e.printStackTrace();
		}

		return result;
	}

	@PUT
	@Path("/")
	public SubscriptionWithCreditLimitResponse update(
			SubscriptionWithCreditLimitUpdateDto subscriptionUpdateDto) {
		SubscriptionWithCreditLimitResponse result = new SubscriptionWithCreditLimitResponse();

		subscriptionUpdateDto.setCurrentUserId(Long.valueOf(paramBean
				.getProperty("asp.api.userId", "1")));
		subscriptionUpdateDto.setProviderId(Long.valueOf(paramBean.getProperty(
				"asp.api.providerId", "1")));

		try {
			customerSubscriptionWithCreditLimitServiceApi
					.update(subscriptionUpdateDto);
		} catch (MeveoApiException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (IncorrectSusbcriptionException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (IncorrectServiceInstanceException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		} catch (BusinessException e) {
			result.setStatus(SubscriptionApiStatusEnum.FAIL.toString());
			result.setAccepted(Boolean.valueOf(false));
			log.error(e.getMessage());
		}

		return result;
	}

}
