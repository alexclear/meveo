package org.meveo.api.rest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.CustomerHeirarchyApi;
import org.meveo.api.dto.CustomerHeirarchyDto;
import org.slf4j.Logger;

@Stateless
@Path("/customer")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class CustomerHeirarchyWS extends BaseWS {

	@Inject
	private Logger log;

	@Inject
	private CustomerHeirarchyApi customerHeirarchyApi;

	/*
	 * Creates the customer heirarchy including :
	 * - Trading Country
	 * - Trading Currency
	 * - Trading Language
	 * - Customer Brand
	 * - Customer Category
	 * - Seller
	 * - Customer
	 * - Customer Account
	 * - Billing Account
	 * - User Account
	 * 
	 * Required Parameters :customerId, customerBrandCode,customerCategoryCode,
	 * sellerCode,currencyCode,countryCode,lastName,languageCode,billingCycleCode
	 * 
	 */
	@POST
	@Path("/create")
	public ActionStatus create(CustomerHeirarchyDto customerHeirarchyDto) {
		log.info("Creating Customer Heirarchy...");
		log.debug("customerHeirarchy.create={}", customerHeirarchyDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			customerHeirarchyDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			customerHeirarchyDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			customerHeirarchyApi.createCustomerHeirarchy(customerHeirarchyDto);

		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@POST
	@Path("/update")
	public ActionStatus update(CustomerHeirarchyDto customerHeirarchyDto) {
		log.info("Updating Customer Heirarchy...");
		log.debug("customerHeirarchy.update={}", customerHeirarchyDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			customerHeirarchyDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			customerHeirarchyDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			customerHeirarchyApi.updateCustomerHeirarchy(customerHeirarchyDto);

		} catch (BusinessException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
