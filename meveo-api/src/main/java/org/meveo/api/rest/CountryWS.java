package org.meveo.api.rest;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.ActionStatus;
import org.meveo.api.ActionStatusEnum;
import org.meveo.api.CountryServiceApi;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.CountryDto;
import org.meveo.api.exception.CountryDoesNotExistsException;
import org.meveo.api.exception.CurrencyDoesNotExistsException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.exception.TradingCountryAlreadyExistsException;
import org.meveo.api.exception.TradingCountryDoesNotExistsException;
import org.meveo.api.rest.response.CountryResponse;
import org.meveo.commons.utils.ParamBean;
import org.meveo.util.MeveoParamBean;
import org.slf4j.Logger;

/**
 * @author Edward P. Legaspi
 * @since Oct 7, 2013
 **/
@Path("/country")
@RequestScoped
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class CountryWS {

	@Inject
	private Logger log;

	@Inject
	@MeveoParamBean
	protected ParamBean paramBean;

	@Inject
	private CountryServiceApi countryServiceApi;

	@GET
	@Path("/index")
	public ActionStatus index() {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS,
				"MEVEO API Rest Web Service");

		return result;
	}

	@POST
	@Path("/")
	public ActionStatus create(CountryDto countryDto) {
		log.debug("createCountry={}", countryDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			countryDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			countryServiceApi.create(countryDto);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (CurrencyDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.CURRENCY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (TradingCountryAlreadyExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TRADING_COUNTRY_ALREADY_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@GET
	@Path("/")
	public CountryResponse find(@QueryParam("countryCode") String countryCode) {
		log.debug("country.find countryCode={}", countryCode);

		CountryResponse result = new CountryResponse();
		result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);

		try {
			result.setCountryDto(countryServiceApi.find(countryCode));
		} catch (Exception e) {
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}

		return result;
	}

	@DELETE
	@Path("/{countryCode}/{currencyCode}")
	public ActionStatus remove(@PathParam("countryCode") String countryCode,
			@PathParam("currencyCode") String currencyCode) {
		log.debug("country.remove countryCode={}, currencyCode={}",
				countryCode, currencyCode);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
		Long providerId = Long.valueOf(paramBean.getProperty(
				"asp.api.providerId", "1"));

		try {
			countryServiceApi.remove(countryCode, currencyCode, providerId);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (TradingCountryDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TRADING_COUNTRY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (CurrencyDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.CURRENCY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@PUT
	@Path("/")
	public ActionStatus update(CountryDto countryDto) {
		log.debug("country.update={}", countryDto);

		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			countryDto.setCurrentUserId(Long.valueOf(paramBean.getProperty(
					"asp.api.userId", "1")));
			countryDto.setProviderId(Long.valueOf(paramBean.getProperty(
					"asp.api.providerId", "1")));

			countryServiceApi.update(countryDto);
		} catch (MissingParameterException e) {
			result.setErrorCode(MeveoApiErrorCode.MISSING_PARAMETER);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (CountryDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.COUNTRY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (CurrencyDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.CURRENCY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (TradingCountryDoesNotExistsException e) {
			result.setErrorCode(MeveoApiErrorCode.TRADING_COUNTRY_DOES_NOT_EXISTS);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
