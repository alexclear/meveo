package org.meveo.api.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.meveo.api.dto.invoice.GenerateInvoiceRequestDto;
import org.meveo.api.dto.invoice.GenerateInvoiceResponseDto;
import org.meveo.api.dto.invoice.GetPdfInvoiceResponseDto;
import org.meveo.api.dto.invoice.GetXmlInvoiceResponseDto;
import org.meveo.api.dto.invoice.InvoiceDto;
import org.meveo.api.dto.response.CustomerInvoicesResponse;
import org.meveo.api.dto.response.InvoiceCreationResponse;

/**
 * @author Edward P. Legaspi
 **/
@WebService
public interface InvoiceWs extends IBaseWs {

	@WebMethod
	public InvoiceCreationResponse create(@WebParam(name = "invoice") InvoiceDto invoiceDto);

	@WebMethod
	public CustomerInvoicesResponse find(@WebParam(name = "customerAccountCode") String customerAccountCode);
	
	@WebMethod
	public GenerateInvoiceResponseDto generateInvoice(@WebParam(name = "generateInvoiceRequest") GenerateInvoiceRequestDto generateInvoiceRequestDto);
	
	@WebMethod
	public GetXmlInvoiceResponseDto getXMLInvoice(@WebParam(name = "invoiceNumber")String invoiceNumber);
	
	@WebMethod
	public GetPdfInvoiceResponseDto getPdfInvoice(@WebParam(name = "invoiceNumber")String invoiceNumber);

}
