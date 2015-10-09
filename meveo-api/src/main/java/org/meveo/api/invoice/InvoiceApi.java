package org.meveo.api.invoice;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.PDFParametersConstruction;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.RatedTransactionDto;
import org.meveo.api.dto.SubCategoryInvoiceAgregateDto;
import org.meveo.api.dto.billing.GenerateInvoiceResultDto;
import org.meveo.api.dto.invoice.GenerateInvoiceRequestDto;
import org.meveo.api.dto.invoice.InvoiceDto;
import org.meveo.api.exception.BusinessApiException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingProcessTypesEnum;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.model.payments.PaymentMethodEnum;
import org.meveo.service.billing.impl.BillingAccountService;
import org.meveo.service.billing.impl.BillingRunService;
import org.meveo.service.billing.impl.InvoiceAgregateService;
import org.meveo.service.billing.impl.InvoiceService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.XMLInvoiceCreator;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.catalog.impl.TaxService;
import org.meveo.service.crm.impl.ProviderService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.payments.impl.OCCTemplateService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.meveo.util.MeveoParamBean;

@Stateless
public class InvoiceApi extends BaseApi {

	@Inject
	RecordedInvoiceService recordedInvoiceService;

	@Inject
	ProviderService providerService;

	@Inject
	CustomerAccountService customerAccountService;

	@Inject
	BillingAccountService billingAccountService;

	@Inject
	BillingRunService billingRunService;

	@Inject
	InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	RatedTransactionService ratedTransactionService;

	@Inject
	OCCTemplateService oCCTemplateService;

	@Inject
	private InvoiceAgregateService invoiceAgregateService;

	@Inject
	InvoiceService invoiceService;

	@Inject
	TaxService taxService;

	@Inject
	XMLInvoiceCreator xmlInvoiceCreator;

	@Inject
	private PDFParametersConstruction pDFParametersConstruction;

	@Inject
	@MeveoParamBean
	private ParamBean paramBean;

	public String create(InvoiceDto invoiceDTO, User currentUser)
			throws MeveoApiException {
		Provider provider = currentUser.getProvider();

		if (invoiceDTO.getSubCategoryInvoiceAgregates().size() > 0
				&& !StringUtils.isBlank(invoiceDTO.getBillingAccountCode())
				&& !StringUtils.isBlank(invoiceDTO.getDueDate())
				&& !StringUtils.isBlank(invoiceDTO.getAmountTax())
				&& !StringUtils.isBlank(invoiceDTO.getAmountWithoutTax())
				&& !StringUtils.isBlank(invoiceDTO.getAmountWithTax())) {
			BillingAccount billingAccount = billingAccountService.findByCode(
					invoiceDTO.getBillingAccountCode(), provider);

			if (billingAccount == null) {
				throw new EntityDoesNotExistsException(BillingAccount.class,
						invoiceDTO.getBillingAccountCode());
			}

			// FIXME : store that in SubCategoryInvoiceAgregateDto

			// FIXME : store that in SubCategoryInvoiceAgregateDto




			Invoice invoice = new Invoice();
			invoice.setBillingAccount(billingAccount);
			invoice.setProvider(provider);
			Date invoiceDate = new Date();
			invoice.setInvoiceDate(invoiceDate);
			invoice.setDueDate(invoiceDTO.getDueDate());
			PaymentMethodEnum paymentMethod= billingAccount.getPaymentMethod();
			if(paymentMethod==null){
				paymentMethod=billingAccount.getCustomerAccount().getPaymentMethod();
			}
			invoice.setPaymentMethod(paymentMethod);
			invoice.setAmountTax(invoiceDTO.getAmountTax());
			invoice.setAmountWithoutTax(invoiceDTO.getAmountWithoutTax());
			invoice.setAmountWithTax(invoiceDTO.getAmountWithTax());
			invoice.setDiscount(invoiceDTO.getDiscount());
			invoice.setInvoiceNumber(invoiceService.getInvoiceNumber(invoice));
			invoiceService.create(invoice, currentUser, provider);
			UserAccount userAccount = billingAccount.getDefaultUserAccount();

			for (SubCategoryInvoiceAgregateDto subCategoryInvoiceAgregateDTO : invoiceDTO
					.getSubCategoryInvoiceAgregates()) {
				String invoiceSubCategoryCode=subCategoryInvoiceAgregateDTO.getInvoiceSubCategoryCode();
				InvoiceSubCategory invoiceSubCategory = invoiceSubCategoryService
						.findByCode(invoiceSubCategoryCode);
				if (invoiceSubCategory == null) {
					throw new EntityDoesNotExistsException(
							InvoiceSubCategory.class, invoiceSubCategoryCode);
				}
				if (subCategoryInvoiceAgregateDTO.getRatedTransactions().size() > 0
						&& !StringUtils.isBlank(subCategoryInvoiceAgregateDTO
								.getItemNumber())
								&& !StringUtils.isBlank(subCategoryInvoiceAgregateDTO
										.getAmountTax())
										&& !StringUtils.isBlank(subCategoryInvoiceAgregateDTO
												.getAmountWithoutTax())
												&& !StringUtils.isBlank(subCategoryInvoiceAgregateDTO
														.getAmountWithTax())) {
					SubCategoryInvoiceAgregate subCategoryInvoiceAgregate = new SubCategoryInvoiceAgregate();
					for(String taxCode:subCategoryInvoiceAgregateDTO.getTaxesCodes()){

						Tax tax = taxService.findByCode(taxCode, provider);
						if (tax == null) {
							throw new EntityDoesNotExistsException(Tax.class, taxCode);
						}

						TaxInvoiceAgregate taxInvoiceAgregate = new TaxInvoiceAgregate();
						taxInvoiceAgregate
						.setAmountWithoutTax(subCategoryInvoiceAgregateDTO
								.getAmountWithoutTax());
						taxInvoiceAgregate
						.setAmountTax(subCategoryInvoiceAgregateDTO
								.getAmountWithoutTax().multiply(tax.getPercent()).divide(new BigDecimal("100")));

						taxInvoiceAgregate
						.setTaxPercent(tax.getPercent());
						taxInvoiceAgregate.setBillingAccount(billingAccount);
						taxInvoiceAgregate.setInvoice(invoice);
						taxInvoiceAgregate.setUserAccount(billingAccount
								.getDefaultUserAccount());
						taxInvoiceAgregate
						.setItemNumber(subCategoryInvoiceAgregateDTO
								.getItemNumber());
						taxInvoiceAgregate.setTax(tax);
						invoiceAgregateService.create(taxInvoiceAgregate,
								currentUser, provider);
						subCategoryInvoiceAgregate.addTaxInvoiceAggregate(taxInvoiceAgregate);
						subCategoryInvoiceAgregate.addSubCategoryTax(tax);
					}



					subCategoryInvoiceAgregate
					.setAmountWithoutTax(subCategoryInvoiceAgregateDTO
							.getAmountWithoutTax());
					subCategoryInvoiceAgregate
					.setAmountWithTax(subCategoryInvoiceAgregateDTO
							.getAmountWithTax());
					subCategoryInvoiceAgregate
					.setAmountTax(subCategoryInvoiceAgregateDTO
							.getAmountTax());
					subCategoryInvoiceAgregate
					.setAccountingCode(subCategoryInvoiceAgregateDTO
							.getAccountingCode());
					subCategoryInvoiceAgregate
					.setBillingAccount(billingAccount);
					subCategoryInvoiceAgregate.setUserAccount(userAccount);
					subCategoryInvoiceAgregate.setInvoice(invoice);
					subCategoryInvoiceAgregate
					.setItemNumber(subCategoryInvoiceAgregateDTO
							.getItemNumber());
					subCategoryInvoiceAgregate
					.setInvoiceSubCategory(invoiceSubCategory);
					subCategoryInvoiceAgregate.setWallet(userAccount
							.getWallet());

					CategoryInvoiceAgregate categoryInvoiceAgregate = new CategoryInvoiceAgregate();
					categoryInvoiceAgregate
					.setAmountWithTax(subCategoryInvoiceAgregateDTO
							.getAmountWithTax());
					categoryInvoiceAgregate
					.setAmountWithoutTax(subCategoryInvoiceAgregateDTO
							.getAmountWithoutTax());
					categoryInvoiceAgregate
					.setAmountTax(subCategoryInvoiceAgregateDTO
							.getAmountTax());
					categoryInvoiceAgregate.setBillingAccount(billingAccount);
					categoryInvoiceAgregate.setInvoice(invoice);
					categoryInvoiceAgregate
					.setItemNumber(subCategoryInvoiceAgregateDTO
							.getItemNumber());
					categoryInvoiceAgregate.setUserAccount(billingAccount
							.getDefaultUserAccount());
					categoryInvoiceAgregate
					.setInvoiceCategory(invoiceSubCategory
							.getInvoiceCategory());
					invoiceAgregateService.create(categoryInvoiceAgregate,
							currentUser, provider);



					subCategoryInvoiceAgregate
					.setCategoryInvoiceAgregate(categoryInvoiceAgregate);
					invoiceAgregateService.create(subCategoryInvoiceAgregate,
							currentUser, provider);

					for (RatedTransactionDto ratedTransaction : subCategoryInvoiceAgregateDTO
							.getRatedTransactions()) {
						RatedTransaction meveoRatedTransaction = new RatedTransaction(
								null, ratedTransaction.getUsageDate(),
								ratedTransaction.getUnitAmountWithoutTax(),
								ratedTransaction.getUnitAmountWithTax(),
								ratedTransaction.getUnitAmountTax(),
								ratedTransaction.getQuantity(),
								ratedTransaction.getAmountWithoutTax(),
								ratedTransaction.getAmountWithTax(),
								ratedTransaction.getAmountTax(),
								RatedTransactionStatusEnum.BILLED, provider,
								null, billingAccount, invoiceSubCategory, null,
								null, null, null,null,null,null);
						meveoRatedTransaction.setCode(ratedTransaction
								.getCode());
						meveoRatedTransaction.setDescription(ratedTransaction
								.getDescription());
						meveoRatedTransaction
						.setUnityDescription(ratedTransaction
								.getUnityDescription());
						meveoRatedTransaction.setInvoice(invoice);
						meveoRatedTransaction
						.setWallet(userAccount.getWallet());
						ratedTransactionService.create(meveoRatedTransaction,
								currentUser, provider);

					}
				} else {
					if (subCategoryInvoiceAgregateDTO.getRatedTransactions()
							.size() <= 0) {
						missingParameters.add("ratedTransactions");
					}
					if (StringUtils.isBlank(subCategoryInvoiceAgregateDTO
							.getItemNumber())) {
						missingParameters.add("itemNumber");
					}
					if (StringUtils.isBlank(subCategoryInvoiceAgregateDTO
							.getAmountTax())) {
						missingParameters.add("amountTax");
					}
					if (StringUtils.isBlank(subCategoryInvoiceAgregateDTO
							.getAmountWithoutTax())) {
						missingParameters.add("amountWithoutTax");
					}
					if (StringUtils.isBlank(subCategoryInvoiceAgregateDTO
							.getAmountWithTax())) {
						missingParameters.add("amountWithTax");
					}

					throw new MissingParameterException(
							getMissingParametersExceptionMessage());
				}
			}
			return invoice.getInvoiceNumber();
		} else {
			if (invoiceDTO.getSubCategoryInvoiceAgregates().size() <= 0) {
				missingParameters.add("subCategoryInvoiceAgregates");
			}
			if (StringUtils.isBlank(invoiceDTO.getBillingAccountCode())) {
				missingParameters.add("billingAccountCode");
			}
			if (StringUtils.isBlank(invoiceDTO.getDueDate())) {
				missingParameters.add("dueDate");
			}
			if (StringUtils.isBlank(invoiceDTO.getAmountTax())) {
				missingParameters.add("amountTax");
			}
			if (StringUtils.isBlank(invoiceDTO.getAmountWithoutTax())) {
				missingParameters.add("amountWithoutTax");
			}
			if (StringUtils.isBlank(invoiceDTO.getAmountWithTax())) {
				missingParameters.add("amountWithTax");
			}

			throw new MissingParameterException(
					getMissingParametersExceptionMessage());
		}

	}

	public List<InvoiceDto> list(String customerAccountCode, Provider provider)
			throws MeveoApiException {
		List<InvoiceDto> customerInvoiceDtos = new ArrayList<InvoiceDto>();

		if (!StringUtils.isBlank(customerAccountCode)) {
			CustomerAccount customerAccount = customerAccountService
					.findByCode(customerAccountCode, provider);
			if (customerAccount == null) {
				throw new EntityDoesNotExistsException(CustomerAccount.class,
						customerAccountCode);
			}

			for (BillingAccount billingAccount : customerAccount
					.getBillingAccounts()) {
				List<Invoice> invoiceList = billingAccount.getInvoices();

				for (Invoice invoice : invoiceList) {
					InvoiceDto customerInvoiceDto = new InvoiceDto();
					customerInvoiceDto.setBillingAccountCode(billingAccount
							.getCode());
					customerInvoiceDto.setInvoiceDate(invoice.getInvoiceDate());
					customerInvoiceDto.setDueDate(invoice.getDueDate());

					customerInvoiceDto.setAmountWithoutTax(invoice
							.getAmountWithoutTax());
					customerInvoiceDto.setAmountTax(invoice.getAmountTax());
					customerInvoiceDto.setAmountWithTax(invoice
							.getAmountWithTax());
					customerInvoiceDto.setInvoiceNumber(invoice
							.getInvoiceNumber());
					customerInvoiceDto.setPaymentMathod(invoice
							.getPaymentMethod().toString());
					customerInvoiceDto.setPDFpresent(invoice.getPdf() != null);
					SubCategoryInvoiceAgregateDto subCategoryInvoiceAgregateDto = null;

					for (InvoiceAgregate invoiceAgregate : invoice
							.getInvoiceAgregates()) {

						subCategoryInvoiceAgregateDto = new SubCategoryInvoiceAgregateDto();

						if (invoiceAgregate instanceof CategoryInvoiceAgregate) {
							subCategoryInvoiceAgregateDto.setType("R");
						} else if (invoiceAgregate instanceof SubCategoryInvoiceAgregate) {
							subCategoryInvoiceAgregateDto.setType("F");
						} else if (invoiceAgregate instanceof TaxInvoiceAgregate) {
							subCategoryInvoiceAgregateDto.setType("T");
						}

						subCategoryInvoiceAgregateDto
						.setItemNumber(invoiceAgregate.getItemNumber());
						subCategoryInvoiceAgregateDto
						.setAccountingCode(invoiceAgregate
								.getAccountingCode());
						subCategoryInvoiceAgregateDto
						.setDescription(invoiceAgregate
								.getDescription());
						subCategoryInvoiceAgregateDto
						.setQuantity(invoiceAgregate.getQuantity());
						subCategoryInvoiceAgregateDto
						.setDiscount(invoiceAgregate.getDiscount());
						subCategoryInvoiceAgregateDto
						.setAmountWithoutTax(invoiceAgregate
								.getAmountWithoutTax());
						subCategoryInvoiceAgregateDto
						.setAmountTax(invoiceAgregate.getAmountTax());
						subCategoryInvoiceAgregateDto
						.setAmountWithTax(invoiceAgregate
								.getAmountWithTax());
						customerInvoiceDto.getSubCategoryInvoiceAgregates()
						.add(subCategoryInvoiceAgregateDto);
					}

					customerInvoiceDtos.add(customerInvoiceDto);
				}
			}

		} else {
			if (StringUtils.isBlank(customerAccountCode)) {
				missingParameters.add("CustomerAccountCode");
			}

			throw new MissingParameterException(
					getMissingParametersExceptionMessage());
		}

		return customerInvoiceDtos;
	}


	public BillingRun launchExceptionalInvoicing(GenerateInvoiceRequestDto generateInvoiceRequestDto,User currentUser,List<Long> BAids) throws MissingParameterException, EntityDoesNotExistsException, BusinessException,BusinessApiException, Exception{
		return   billingRunService.launchExceptionalInvoicing(BAids, generateInvoiceRequestDto.getInvoicingDate(), generateInvoiceRequestDto.getLastTransactionDate(),BillingProcessTypesEnum.AUTOMATIC,currentUser);
	}
	
	public void  updateBAtotalAmount(BillingAccount billingAccount,BillingRun billingRun,User currentUser){
		billingAccountService.updateBillingAccountTotalAmounts(billingAccount,billingRun,currentUser); 
		log.debug("updateBillingAccountTotalAmounts ok");
	}
	
	public void createRatedTransaction(Long billingAccountId,User currentUser, Date invoicingDate) throws Exception{
		ratedTransactionService.createRatedTransaction(billingAccountId, currentUser, invoicingDate);
	}

	
	public BillingRun updateBR(BillingRun billingRun,BillingRunStatusEnum status,Integer billingAccountNumber,Integer billableBillingAcountNumber ){
		billingRun.setStatus(status);
		if(billingAccountNumber != null){
			billingRun.setBillingAccountNumber(billingAccountNumber);
		}
		if(billableBillingAcountNumber != null){
			billingRun.setBillableBillingAcountNumber(billableBillingAcountNumber);
		}
		return billingRunService.update(billingRun);
	}
	
	public void validateBR(BillingRun billingRun,User user){
		billingRunService.validate(billingRun, user);
	}
	
	public void createAgregatesAndInvoice(Long billingRunId, Date lastTransactionDate, User currentUser) throws BusinessException, Exception{
		billingRunService.createAgregatesAndInvoice(billingRunId, lastTransactionDate, currentUser, 1, 0);
	}
	
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public GenerateInvoiceResultDto generateInvoice(GenerateInvoiceRequestDto generateInvoiceRequestDto,User currentUser) throws MissingParameterException, EntityDoesNotExistsException, BusinessException,BusinessApiException, Exception{

		if (generateInvoiceRequestDto == null) {
			missingParameters.add("generateInvoiceRequest");
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		if (StringUtils.isBlank(generateInvoiceRequestDto.getBillingAccountCode()) ) {
			missingParameters.add("billingAccountCode");
		}
		
		if (generateInvoiceRequestDto.getInvoicingDate() == null  ) {
			missingParameters.add("invoicingDate");
		}
		if (generateInvoiceRequestDto.getLastTransactionDate() == null  ) {
			missingParameters.add("lastTransactionDate");
		}
		if(!missingParameters.isEmpty()){
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
		BillingAccount billingAccount = billingAccountService.findByCode(generateInvoiceRequestDto.getBillingAccountCode(), currentUser.getProvider());
		if(billingAccount == null){
			throw new EntityDoesNotExistsException(BillingAccount.class,generateInvoiceRequestDto.getBillingAccountCode());
		}

		if(billingAccount.getBillingRun() != null && (
			billingAccount.getStatus().equals(BillingRunStatusEnum.NEW) ||
			billingAccount.getStatus().equals(BillingRunStatusEnum.ON_GOING) ||
			billingAccount.getStatus().equals(BillingRunStatusEnum.TERMINATED) ||
			billingAccount.getStatus().equals(BillingRunStatusEnum.WAITING))){
			
				throw new BusinessApiException("BillingAccount already in an invoicing");
		}
		
		List<Long> baIds = new ArrayList<Long>();
		baIds.add(billingAccount.getId());

		createRatedTransaction(billingAccount.getId(),currentUser,generateInvoiceRequestDto.getInvoicingDate());
		log.info("createRatedTransaction ok");
		
		BillingRun billingRun = launchExceptionalInvoicing(generateInvoiceRequestDto, currentUser, baIds);
		Long billingRunId = billingRun.getId();
		log.info("launchExceptionalInvoicing ok , billingRun.id:"+billingRunId);

		updateBAtotalAmount(billingAccount,billingRun,currentUser); 
		log.info("updateBillingAccountTotalAmounts ok");

		billingRun = updateBR(billingRun, BillingRunStatusEnum.ON_GOING,1,1);
		log.info("update billingRun ON_GOING");

		createAgregatesAndInvoice(billingRun.getId(),billingRun.getLastTransactionDate(), currentUser);	
		log.info("createAgregatesAndInvoice ok");
	
		billingRun = updateBR(billingRun, BillingRunStatusEnum.TERMINATED,null,null);
		log.info("update billingRun TERMINATED");
	
		validateBR(billingRun, currentUser);	
		log.info("billingRunService.validate ok");
		
		List<Invoice> invoices = invoiceService.getInvoices(billingRun);
		log.info((invoices==null)?"getInvoice is null" : "size="+ invoices.size());

		GenerateInvoiceResultDto generateInvoiceResultDto = new GenerateInvoiceResultDto();
		generateInvoiceResultDto.setInvoiceNumber(invoices.get(0).getInvoiceNumber());
		return  generateInvoiceResultDto;		
	}

	public String getXMLInvoice(String invoiceNumber,User currentUser) throws  FileNotFoundException, MissingParameterException, EntityDoesNotExistsException, BusinessException {
		log.debug("getXMLInvoice  invoiceNumber:{}",invoiceNumber);
		if (invoiceNumber == null) {
			missingParameters.add("invoiceNumber");
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		
		Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber, currentUser.getProvider().getCode());
		if(invoice == null){
			throw new EntityDoesNotExistsException(Invoice.class,invoiceNumber);
		}
		ParamBean param = ParamBean.getInstance();
		String invoicesDir = param.getProperty("providers.rootDir", "/tmp/meveo");
		String sep = File.separator ;
		String invoicePath = invoicesDir + sep + currentUser.getProvider().getCode() + sep + "invoices" + sep + "xml" + sep + invoice.getBillingRun().getId();
		File billingRundir = new File(invoicePath);
		xmlInvoiceCreator.createXMLInvoice(invoice.getId(), billingRundir);
		String xmlCanonicalPath = invoicePath + sep+ invoiceNumber + ".xml";
		Scanner scanner = new Scanner(new File(xmlCanonicalPath));
		String xmlContent = scanner.useDelimiter("\\Z").next();
		scanner.close();
		log.debug("getXMLInvoice  invoiceNumber:{} done.",invoiceNumber);
		return xmlContent;
	}

	public byte[] getPdfInvoince(String invoiceNumber,User currentUser) throws MissingParameterException, EntityDoesNotExistsException,Exception{
		log.debug("getPdfInvoince  invoiceNumber:{}",invoiceNumber);
		if (invoiceNumber == null) {
			missingParameters.add("invoiceNumber");
			throw new MissingParameterException(getMissingParametersExceptionMessage());
		}
		Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber, currentUser.getProvider().getCode());
		if(invoice == null){
			throw new EntityDoesNotExistsException(Invoice.class,invoiceNumber);
		}
		if(invoice.getPdf() == null){
		Map<String, Object> parameters = pDFParametersConstruction.constructParameters(invoice.getId(), currentUser.getProvider());
		invoiceService.producePdf(parameters, currentUser);
		}
		invoiceService.findById(invoice.getId(),true);
		log.debug("getXMLInvoice invoiceNumber:{} done.",invoiceNumber);
		return invoice.getPdf();
	}

}
