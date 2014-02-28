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
import org.meveo.api.dto.ServicePricePlanDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.ServiceTemplateDoesNotExistsException;
import org.meveo.asg.api.ServicePricePlanServiceApi;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Path("/asg/servicePricePlan")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ServicePricePlanWS {

	@Inject
	private Logger log;

	@Inject
	private ServicePricePlanServiceApi servicePricePlanServiceApi;

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@POST
	@Path("/")
	public ActionStatus create(ServicePricePlanDto servicePricePlanDto) {
		log.debug("create={}", servicePricePlanDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		String servicePricePlanId = servicePricePlanDto.getServiceId();
		try {
			servicePricePlanDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			servicePricePlanDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			servicePricePlanServiceApi.create(servicePricePlanDto);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.FAIL) {
			servicePricePlanServiceApi.removeAsgMapping(servicePricePlanId,
					EntityCodeEnum.SPF);
		}

		return result;
	}

	@DELETE
	@Path("/{serviceId}/{organizationId}")
	public ActionStatus remove(@PathParam("serviceId") String serviceId,
			@PathParam("organizationId") String organizationId) {
		log.debug("remove serviceId={}, organizationId={}", serviceId,
				organizationId);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			servicePricePlanServiceApi.remove(serviceId, organizationId, Long
					.valueOf(paramBean.getProperty("asp.api.userId", "1")),
					Long.valueOf(paramBean.getProperty("asp.api.providerId",
							"1")));
		} catch (ServiceTemplateDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.SERVICE_TEMPLATE_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.SUCCESS) {
			servicePricePlanServiceApi.removeAsgMapping(serviceId,
					EntityCodeEnum.SPF);
		}

		return result;
	}

	@PUT
	@Path("/")
	public ActionStatus update(ServicePricePlanDto servicePricePlanDto) {
		log.debug("update={}", servicePricePlanDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			servicePricePlanDto.setCurrentUserId(Long.valueOf(paramBean
					.getProperty("asp.api.userId", "1")));
			servicePricePlanDto.setProviderId(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")));

			servicePricePlanServiceApi.update(servicePricePlanDto);
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
