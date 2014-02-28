package org.meveo.asg.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.OfferPricePlanDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.ServiceTemplateDoesNotExistsException;
import org.meveo.asg.api.OfferPricePlanServiceApi;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Path("/asg/offerPricePlan")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class OfferPricePlanWS {

	@Inject
	private Logger log;

	@Inject
	private ParamBean paramBean;

	@Inject
	private OfferPricePlanServiceApi offerPricePlanServiceApi;

	@POST
	@Path("/")
	public ActionStatus create(OfferPricePlanDto offerPricePlanDto) {
		log.debug("create={}", offerPricePlanDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		String offerPricePlanId = offerPricePlanDto.getOfferId();
		try {
			offerPricePlanDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			offerPricePlanDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			offerPricePlanServiceApi.create(offerPricePlanDto);
		} catch (ServiceTemplateDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.SERVICE_TEMPLATE_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.FAIL) {
			offerPricePlanServiceApi.removeAsgMapping(offerPricePlanId,
					EntityCodeEnum.OPF);
		}

		return result;
	}

	@DELETE
	@Path("/{offerId}/{organizationId}")
	public ActionStatus remove(@PathParam("offerId") String offerId,
			@PathParam("organizationId") String organizationId) {
		log.debug("remove offerId={}, organizationId={}", offerId,
				organizationId);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			offerPricePlanServiceApi.remove(offerId, organizationId, Long
					.valueOf(paramBean.getProperty("asp.api.userId", "1")),
					Long.valueOf(paramBean.getProperty("asp.api.providerId",
							"1")));
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.SUCCESS) {
			offerPricePlanServiceApi.removeAsgMapping(offerId,
					EntityCodeEnum.OPF);
		}

		return result;
	}

	@PUT
	@Path("/")
	public ActionStatus update(OfferPricePlanDto offerPricePlanDto) {
		log.debug("update={}", offerPricePlanDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			offerPricePlanDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			offerPricePlanDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			offerPricePlanServiceApi.update(offerPricePlanDto);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
