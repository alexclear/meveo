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
import org.meveo.api.dto.ServiceDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.ServiceTemplateAlreadyExistsException;
import org.meveo.asg.api.ServiceTemplateServiceApi;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 11, 2013
 **/
@Path("/asg/serviceTemplate")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ServiceTemplateWS {

	@Inject
	private Logger log;

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private ServiceTemplateServiceApi serviceTemplateServiceApi;

	@POST
	@Path("/")
	public ActionStatus create(ServiceDto serviceDto) {
		log.debug("create={}", serviceDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		String serviceId = serviceDto.getServiceId();
		try {
			serviceDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			serviceDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			serviceTemplateServiceApi.create(serviceDto);
		} catch (ServiceTemplateAlreadyExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.SERVICE_TEMPLATE_ALREADY_EXISTS);
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
			serviceTemplateServiceApi.removeAsgMapping(serviceId,
					EntityCodeEnum.S);
		}

		return result;
	}

	@PUT
	@Path("/")
	public ActionStatus update(ServiceDto serviceDto) {
		log.debug("update={}", serviceDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			serviceDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			serviceDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			serviceTemplateServiceApi.update(serviceDto);
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

	@DELETE
	@Path("/{serviceId}")
	public ActionStatus remove(@PathParam("serviceId") String serviceId) {
		log.debug("remove serviceId={}", serviceId);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			serviceTemplateServiceApi.remove(Long.valueOf(paramBean
					.getProperty("asp.api.providerId", "1")), serviceId);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		if (result.getStatus() == ActionStatusEnum.SUCCESS) {
			serviceTemplateServiceApi.removeAsgMapping(serviceId,
					EntityCodeEnum.S);
		}

		return result;
	}
}
