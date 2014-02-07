package org.meveo.asg.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.TaxDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.asg.api.TaxServiceApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 9, 2013
 **/
@Path("/asg/tax")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class TaxWS {

	@Inject
	private Logger log;

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	@Inject
	private TaxServiceApi taxServiceApi;

	@GET
	@Path("/index")
	public ActionStatus index() {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS,
				"MEVEO API Rest Web Service");

		return result;
	}

	@POST
	@Path("/")
	public ActionStatus create(TaxDto taxDto) {
		log.debug("create={}", taxDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			taxDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			taxDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			taxServiceApi.create(taxDto);
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

	@PUT
	@Path("/")
	public ActionStatus update(TaxDto taxDto) {
		log.debug("update={}", taxDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			taxDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			taxDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			taxServiceApi.update(taxDto);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@DELETE
	@Path("/{taxId}")
	public ActionStatus remove(@PathParam("taxId") String taxId) {
		log.debug("remove taxId={}", taxId);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			taxServiceApi.remove(taxId);
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
