package org.meveo.asg.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.TerminateCustomerSubscriptionDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.rest.response.TerminateCustomerSubscriptionResponse;
import org.meveo.asg.api.CustomerSubscriptionWithCreditLimitServiceApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 **/
@Path("/asg/terminateCustomerSubscription")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class TerminateCustomerSubscriptionWS {

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private Logger log;

	@Inject
	private CustomerSubscriptionWithCreditLimitServiceApi customerSubscriptionWithCreditLimitServiceApi;

	@POST
	@Path("/")
	public TerminateCustomerSubscriptionResponse terminate(
			TerminateCustomerSubscriptionDto terminateCustomerSubscriptionDto) {
		log.debug("terminate={}", terminateCustomerSubscriptionDto);

		TerminateCustomerSubscriptionResponse response = new TerminateCustomerSubscriptionResponse();

		terminateCustomerSubscriptionDto.setCurrentUserId(Long
				.valueOf(paramBean.getProperty("asp.api.userId", "1")));
		terminateCustomerSubscriptionDto.setProviderId(Long.valueOf(paramBean
				.getProperty("asp.api.providerId", "1")));

		try {
			response = customerSubscriptionWithCreditLimitServiceApi
					.terminateSubscription(terminateCustomerSubscriptionDto);
		} catch (MeveoApiException e) {
			log.error(e.getMessage());
		} catch (BusinessException e) {
			e.printStackTrace();
		}

		return response;
	}

}
