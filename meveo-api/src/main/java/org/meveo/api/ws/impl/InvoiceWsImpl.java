package org.meveo.api.ws.impl;

import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.jws.WebService;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.invoice.GenerateInvoiceRequestDto;
import org.meveo.api.dto.invoice.GenerateInvoiceResponseDto;
import org.meveo.api.dto.invoice.GetPdfInvoiceResponseDto;
import org.meveo.api.dto.invoice.GetXmlInvoiceResponseDto;
import org.meveo.api.dto.invoice.InvoiceDto;
import org.meveo.api.dto.response.CustomerInvoicesResponse;
import org.meveo.api.dto.response.InvoiceCreationResponse;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.api.invoice.InvoiceApi;
import org.meveo.api.logging.LoggingInterceptor;
import org.meveo.api.ws.InvoiceWs;

@WebService(serviceName = "InvoiceWs", endpointInterface = "org.meveo.api.ws.InvoiceWs")
@Interceptors({ LoggingInterceptor.class })
public class InvoiceWsImpl extends BaseWs implements InvoiceWs {

	@Inject
	InvoiceApi invoiceApi;

	@Override
	public InvoiceCreationResponse create(InvoiceDto invoiceDto) {
		InvoiceCreationResponse result = new InvoiceCreationResponse();

		try {
			String invoiceNumber = invoiceApi.create(invoiceDto, getCurrentUser());
			result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);
			result.setInvoiceNumber(invoiceNumber);
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
	public CustomerInvoicesResponse find(String customerAccountCode) {
		CustomerInvoicesResponse result = new CustomerInvoicesResponse();

		try {
			result.setCustomerInvoiceDtoList(invoiceApi.list(customerAccountCode, getCurrentUser().getProvider()));
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
	public GenerateInvoiceResponseDto generateInvoice(GenerateInvoiceRequestDto generateInvoiceRequestDto) {
		GenerateInvoiceResponseDto result = new GenerateInvoiceResponseDto();
		try{

			result.setGenerateInvoiceResultDto(invoiceApi.generateInvoice(generateInvoiceRequestDto, getCurrentUser()));
			result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);

		}catch(MissingParameterException mpe){
			result.getActionStatus().setErrorCode(mpe.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(mpe.getMessage());
		}catch (EntityDoesNotExistsException ednep) {
			result.getActionStatus().setErrorCode(ednep.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(ednep.getMessage());
		}catch (BusinessApiException bae) {
			result.getActionStatus().setErrorCode(bae.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(bae.getMessage());			
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}
		log.info("generateInvoice Response={}", result);
		return result;
	}

	@Override
	public GetXmlInvoiceResponseDto getXMLInvoice(String invoiceNumber) {
		GetXmlInvoiceResponseDto result = new GetXmlInvoiceResponseDto();
		try{
			
			result.setXmlContent(invoiceApi.getXMLInvoice(invoiceNumber, getCurrentUser()));
			result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);
			
		}catch(MissingParameterException mpe){
			result.getActionStatus().setErrorCode(mpe.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(mpe.getMessage());
		}catch (EntityDoesNotExistsException ednep) {
			result.getActionStatus().setErrorCode(ednep.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(ednep.getMessage());
		}catch (BusinessException bae) {
			result.getActionStatus().setErrorCode("BusinessException");
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(bae.getMessage());			
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}
		log.info("getXMLInvoice Response={}", result);
		return result;
	}

	@Override
	public GetPdfInvoiceResponseDto getPdfInvoice(String invoiceNumber) {
		GetPdfInvoiceResponseDto result = new GetPdfInvoiceResponseDto();
		try {
			
			result.setPdfContent(invoiceApi.getPdfInvoince(invoiceNumber, getCurrentUser()));
			result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);
			
		}catch(MissingParameterException mpe){
			result.getActionStatus().setErrorCode(mpe.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(mpe.getMessage());
		}catch (EntityDoesNotExistsException ednep) {
			result.getActionStatus().setErrorCode(ednep.getErrorCode());
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(ednep.getMessage());			
		} catch (Exception e) {
			result.getActionStatus().setErrorCode(MeveoApiErrorCode.GENERIC_API_EXCEPTION);
			result.getActionStatus().setStatus(ActionStatusEnum.FAIL);
			result.getActionStatus().setMessage(e.getMessage());
		}
		log.info("getPdfInvoice Response={}", result);
		return result;
	}

}
