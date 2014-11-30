package org.meveo.api.rest.impl;

import javax.inject.Inject;

import org.meveo.api.InvoiceSubCategoryCountryApi;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.InvoiceSubCategoryCountryDto;
import org.meveo.api.dto.response.GetInvoiceSubCategoryCountryResponse;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.rest.InvoiceSubCategoryCountryRs;

/**
 * @author Edward P. Legaspi
 **/
public class InvoiceSubCategoryCountryRsImpl extends BaseRs implements
		InvoiceSubCategoryCountryRs {

	@Inject
	private InvoiceSubCategoryCountryApi invoiceSubCategoryCountryApi;

	@Override
	public ActionStatus create(InvoiceSubCategoryCountryDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			invoiceSubCategoryCountryApi.create(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setErrorCode(e.getErrorCode());
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@Override
	public ActionStatus update(InvoiceSubCategoryCountryDto postData) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			invoiceSubCategoryCountryApi.update(postData, getCurrentUser());
		} catch (MeveoApiException e) {
			result.setErrorCode(e.getErrorCode());
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

	@Override
	public GetInvoiceSubCategoryCountryResponse find(
			String invoiceSubCategoryCode, String country) {
		GetInvoiceSubCategoryCountryResponse result = new GetInvoiceSubCategoryCountryResponse();

		try {
			result.setInvoiceSubCategoryCountryDto(invoiceSubCategoryCountryApi
					.find(invoiceSubCategoryCode, country, getCurrentUser()
							.getProvider()));
		} catch (MeveoApiException e) {
			result.getActionStatus().setErrorCode(e.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(
					MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}

		return result;
	}

	@Override
	public ActionStatus remove(String invoiceSubCategoryCode, String country) {
		ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

		try {
			invoiceSubCategoryCountryApi.remove(invoiceSubCategoryCode,
					country, getCurrentUser().getProvider());
		} catch (MeveoApiException e) {
			result.setErrorCode(e.getErrorCode());
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		} catch (Exception e) {
			result.setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.setStatus(ActionStatusEnum.FAIL);
			result.setMessage(e.getMessage());
		}

		return result;
	}

}
