package org.meveo.api.rest.billing.impl;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.billing.WalletApi;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.billing.FindWalletOperationsDto;
import org.meveo.api.dto.billing.WalletBalanceDto;
import org.meveo.api.dto.billing.WalletOperationDto;
import org.meveo.api.dto.billing.WalletReservationDto;
import org.meveo.api.dto.billing.WalletTemplateDto;
import org.meveo.api.dto.response.billing.FindWalletOperationsResponseDto;
import org.meveo.api.dto.response.billing.GetWalletTemplateResponseDto;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.logging.LoggingInterceptor;
import org.meveo.api.rest.billing.WalletRs;
import org.meveo.api.rest.impl.BaseRs;

/**
 * @author Edward P. Legaspi
 **/
@RequestScoped
@Interceptors({ LoggingInterceptor.class })
public class WalletRsImpl extends BaseRs implements WalletRs {

	@Inject
	private WalletApi walletApi;

	@Override
	public ActionStatus currentBalance(WalletBalanceDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			result.setMessage("" + walletApi.getCurrentAmount(postData, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus reservedBalance(WalletBalanceDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			result.setMessage("" + walletApi.getReservedAmount(postData, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus openBalance(WalletBalanceDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			result.setMessage("" + walletApi.getOpenAmount(postData, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus createReservation(WalletReservationDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			result.setMessage("" + walletApi.createReservation(postData, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus updateReservation(WalletReservationDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			walletApi.updateReservation(postData, getCurrentUser().getProvider());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus cancelReservation(Long reservationId) {
		ActionStatus result = new ActionStatus();

		try {
			walletApi.cancelReservation(reservationId, getCurrentUser().getProvider());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus confirmReservation(WalletReservationDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			result.setMessage("" + walletApi.confirmReservation(postData, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus createOperation(WalletOperationDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			walletApi.createOperation(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setErrorCode(e.getErrorCode());
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public FindWalletOperationsResponseDto findOperations(FindWalletOperationsDto postData) {
		FindWalletOperationsResponseDto result = new FindWalletOperationsResponseDto();

		try {
			result = walletApi.findOperations(postData, getCurrentUser().getProvider());
		} catch (MeveoApiException e) {
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus createWalletTemplate(WalletTemplateDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			walletApi.create(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}
	
	@Override
	public ActionStatus updateWalletTemplate(WalletTemplateDto postData) {
		ActionStatus result = new ActionStatus();

		try {
			walletApi.update(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public GetWalletTemplateResponseDto findWalletTemplate(String walletTemplateCode) {
		GetWalletTemplateResponseDto result = new GetWalletTemplateResponseDto();

		try {
			result.setWalletTemplate(walletApi.find(walletTemplateCode, getCurrentUser().getProvider()));
		} catch (MeveoApiException e) {
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}

	@Override
	public ActionStatus removeWalletTemplate(String walletTemplateCode) {
		ActionStatus result = new ActionStatus();

		try {
			walletApi.remove(walletTemplateCode, getCurrentUser().getProvider());
		} catch (MeveoApiException e) {
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}
	
	public ActionStatus createOrUpdateWalletTemplate(WalletTemplateDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
		
		try {
			walletApi.createOrUpdate(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setErrorCode(e.getErrorCode());
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		log.debug("RESPONSE={}", result);
		return result;
	}
}
