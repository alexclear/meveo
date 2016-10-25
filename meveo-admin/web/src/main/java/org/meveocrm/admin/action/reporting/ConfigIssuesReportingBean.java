package org.meveocrm.admin.action.reporting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.CurrentProvider;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.BaseEntity;
import org.meveo.model.billing.InvoiceCategory;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TradingLanguage;
import org.meveo.model.billing.WalletOperationStatusEnum;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.catalog.OneShotChargeTemplate;
import org.meveo.model.catalog.RecurringChargeTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.model.catalog.UsageChargeTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.rating.EDRStatusEnum;
import org.meveo.model.scripts.CustomScript;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.TradingLanguageService;
import org.meveo.service.billing.impl.WalletOperationService;
import org.meveo.service.catalog.impl.CounterTemplateService;
import org.meveo.service.catalog.impl.InvoiceCategoryService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.catalog.impl.OneShotChargeTemplateService;
import org.meveo.service.catalog.impl.RecurringChargeTemplateService;
import org.meveo.service.catalog.impl.ServiceTemplateService;
import org.meveo.service.catalog.impl.TaxService;
import org.meveo.service.catalog.impl.UsageChargeTemplateService;
import org.meveo.service.script.ScriptInstanceService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.TabChangeEvent;

@Named
@ViewScoped
public class ConfigIssuesReportingBean extends BaseBean<BaseEntity> {

    private static final long serialVersionUID = 1L;

    @Inject
    private WalletOperationService walletOperationService;

    @Inject
    private UsageChargeTemplateService usageChargeTemplateService;

    @Inject
    private RecurringChargeTemplateService recurringChargeTemplateService;

    @Inject
    private OneShotChargeTemplateService oneShotChargeTemplateService;

    @Inject
    private TaxService taxService;

    @Inject
    private TradingLanguageService tradingLanguageService;

    @Inject
    private InvoiceCategoryService invoiceCategoryService;

    @Inject
    private InvoiceSubCategoryService invoiceSubCategoryService;

    @Inject
    private ServiceTemplateService serviceTemplateService;

    @SuppressWarnings("rawtypes")
    @Inject
    private CounterTemplateService counterTemplateService;

    @Inject
    private ScriptInstanceService scriptInstanceService;
    
	private ParamBean paramBean = ParamBean.getInstance();

    @Inject
    @CurrentProvider
    private Provider currentProvider;

    private List<Entry<String, String>> jaspers;

    List<Tax> taxesNotAssociatedList = new ArrayList<Tax>();
    List<UsageChargeTemplate> usagesWithNotPricePlList = new ArrayList<UsageChargeTemplate>();
    List<RecurringChargeTemplate> recurringWithNotPricePlanList = new ArrayList<RecurringChargeTemplate>();
    List<OneShotChargeTemplate> oneShotChrgWithNotPricePlanList = new ArrayList<OneShotChargeTemplate>();
    List<TradingLanguage> languagesNotAssociatedList = new ArrayList<TradingLanguage>();
    List<InvoiceCategory> invoiceCatNotAssociatedList = new ArrayList<InvoiceCategory>();
    List<InvoiceSubCategory> invoiceSubCatNotAssociatedList = new ArrayList<InvoiceSubCategory>();
    List<ServiceTemplate> servicesWithNotOfferList = new ArrayList<ServiceTemplate>();
    List<UsageChargeTemplate> usagesChrgNotAssociatedList = new ArrayList<UsageChargeTemplate>();
    List<CounterTemplate> counterWithNotServicList = new ArrayList<CounterTemplate>();
    List<RecurringChargeTemplate> recurringNotAssociatedList = new ArrayList<RecurringChargeTemplate>();
    List<OneShotChargeTemplate> terminationNotAssociatedList = new ArrayList<OneShotChargeTemplate>();
    List<OneShotChargeTemplate> subNotAssociatedList = new ArrayList<OneShotChargeTemplate>();
    List<CustomScript> scriptInstanceWithErrorList = new ArrayList<CustomScript>();
    Map<String, String> jasperFilesList = new HashMap<String, String>();

    public int getNbrUsagesWithNotPricePlan() {
        return usageChargeTemplateService.getNbrUsagesChrgWithNotPricePlan(currentProvider);
    }

    public int getNbrRecurringWithNotPricePlan() {
        return recurringChargeTemplateService.getNbrRecurringChrgWithNotPricePlan(currentProvider);
    }

    public int getNbrOneShotWithNotPricePlan() {
        return oneShotChargeTemplateService.getNbrOneShotWithNotPricePlan(currentProvider);
    }

    public void constructChargesWithNotPricePlan(TabChangeEvent event) {
        usagesWithNotPricePlList = usageChargeTemplateService.getUsagesChrgWithNotPricePlan(currentProvider);
        recurringWithNotPricePlanList = recurringChargeTemplateService.getRecurringChrgWithNotPricePlan(currentProvider);
        oneShotChrgWithNotPricePlanList = oneShotChargeTemplateService.getOneShotChrgWithNotPricePlan(currentProvider);
    }

    public void constructTaxesNotAssociated(TabChangeEvent event) {
        taxesNotAssociatedList = taxService.getTaxesNotAssociated(currentProvider);
    }

    public void constructLanguagesNotAssociated(TabChangeEvent event) {
        languagesNotAssociatedList = tradingLanguageService.getLanguagesNotAssociated(currentProvider);
    }

    public void constructInvoiceCatNotAssociated(TabChangeEvent event) {
        invoiceCatNotAssociatedList = invoiceCategoryService.getInvoiceCatNotAssociated(currentProvider);
    }

    public void constructInvoiceSubCatNotAssociated(TabChangeEvent event) {
        invoiceSubCatNotAssociatedList = invoiceSubCategoryService.getInvoiceSubCatNotAssociated(currentProvider);
    }

    public void constructServicesWithNotOffer(TabChangeEvent event) {
        servicesWithNotOfferList = serviceTemplateService.getServicesWithNotOffer(currentProvider);
    }

    public void constructUsagesChrgNotAssociated(TabChangeEvent event) {
        usagesChrgNotAssociatedList = usageChargeTemplateService.getUsagesChrgNotAssociated(currentProvider);
    }

    @SuppressWarnings("unchecked")
    public void constructCounterWithNotService(TabChangeEvent event) {
        counterWithNotServicList = counterTemplateService.getCounterWithNotService(currentProvider);
    }

    public void constructRecurringNotAssociated(TabChangeEvent event) {
        recurringNotAssociatedList = recurringChargeTemplateService.getRecurringChrgNotAssociated(currentProvider);
    }

    public void constructTermChrgNotAssociated(TabChangeEvent event) {
        terminationNotAssociatedList = oneShotChargeTemplateService.getTerminationChrgNotAssociated(currentProvider);
    }

    public void constructSubChrgNotAssociated(TabChangeEvent event) {
        subNotAssociatedList = oneShotChargeTemplateService.getSubscriptionChrgNotAssociated(currentProvider);
    }

    public void constructScriptInstancesWithError(TabChangeEvent event) {
        scriptInstanceWithErrorList = scriptInstanceService.getScriptInstancesWithError(currentProvider);
    }
    
    private  Map<String, String> getJasperFiles() throws IOException{
    	Map<String,String> jasperFiles = new HashMap<String,String>(); 
    	String jasperCommercial = paramBean.getProperty("jasper.invoiceTemplate.commercial","invoice.jasper");
    	String jasperAdjustment = paramBean.getProperty("jasper.invoiceTemplate.adjustment","invoice.jasper");
    	//check jaspers files
    	File jasperDir= new File(paramBean.getProperty("providers.rootDir","/tmp/meveo/")+ File.separator+ getCurrentProvider().getCode() + File.separator+"jasper");
    	if(!jasperDir.exists()){
    		jasperDir.mkdirs();
    	}
    	log.info("Jaspers template used :"+jasperDir.getPath());
    	File[] foldersList = jasperDir.listFiles(); 
    	String commercialRep=null;
    	String adjustRep=null;
    	File commercialInvoice=null;
    	File adjustInvoice=null;
    	if (foldersList != null && foldersList.length >0) {
		for(File f:foldersList){
			adjustRep= f.getCanonicalPath()+File.separator+"invoiceAdjustmentPdf";
			adjustInvoice= new File(adjustRep+File.separator+jasperCommercial); 
			if(!adjustInvoice.exists()){
              jasperFiles.put(adjustRep, jasperAdjustment);
			}
			commercialRep= f.getCanonicalPath()+File.separator+"pdf";
			commercialInvoice= new File(commercialRep+File.separator+jasperCommercial); 
			if(!commercialInvoice.exists()){
              jasperFiles.put(commercialRep, jasperCommercial);
			}  
			}
    	}
    	return jasperFiles;
    }

    public void getJasperFilesNotFound(TabChangeEvent event) throws IOException {
        jasperFilesList =getJasperFiles();
        if (jasperFilesList != null && jasperFilesList.size() > 0) {
            jaspers = new ArrayList<>(jasperFilesList.entrySet());
        }
    }

    ConfigIssuesReportingDTO reportConfigDto;

    @PostConstruct
    public void init() throws IOException {
        reportConfigDto = new ConfigIssuesReportingDTO();
        reportConfigDto.setNbrWalletOpOpen(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.OPEN, currentProvider).intValue());
        reportConfigDto.setNbrWalletOpRerated(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.RERATED, currentProvider).intValue());
        reportConfigDto.setNbrWalletOpReserved(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.RESERVED, currentProvider).intValue());
        reportConfigDto.setNbrWalletOpCancled(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.CANCELED, currentProvider).intValue());
        reportConfigDto.setNbrWalletOpTorerate(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.TO_RERATE, currentProvider).intValue());
        reportConfigDto.setNbrWalletOpTreated(walletOperationService.getNbrWalletOperationByStatus(WalletOperationStatusEnum.TREATED, currentProvider).intValue());
        reportConfigDto.setNbrEdrOpen(walletOperationService.getNbrEdrByStatus(EDRStatusEnum.OPEN, currentProvider).intValue());
        reportConfigDto.setNbrEdrRated(walletOperationService.getNbrEdrByStatus(EDRStatusEnum.RATED, currentProvider).intValue());
        reportConfigDto.setNbrEdrRejected(walletOperationService.getNbrEdrByStatus(EDRStatusEnum.REJECTED, currentProvider).intValue());
        reportConfigDto.setNbrJasperDir(getJasperFiles().size());
    }

    public Integer getNbrChargesWithNotPricePlan() {
        return getNbrUsagesWithNotPricePlan() + getNbrRecurringWithNotPricePlan() + getNbrOneShotWithNotPricePlan();
    }

    public Integer getNbTaxesNotAssociated() {
        return taxService.getNbTaxesNotAssociated(currentProvider);
    }

    public Integer getNbLanguageNotAssociated() {
        return tradingLanguageService.getNbLanguageNotAssociated(currentProvider);
    }

    public Integer getNbInvCatNotAssociated() {
        return invoiceCategoryService.getNbInvCatNotAssociated(currentProvider);
    }

    public Integer getNbInvSubCatNotAssociated() {
        return invoiceSubCategoryService.getNbInvSubCatNotAssociated(currentProvider);
    }

    public Integer getNbServiceWithNotOffer() {
        return serviceTemplateService.getNbServiceWithNotOffer(currentProvider);
    }

    public Integer getNbrUsagesChrgNotAssociated() {
        return usageChargeTemplateService.getNbrUsagesChrgNotAssociated(currentProvider);
    }

    public Integer getNbrCounterWithNotService() {
        return counterTemplateService.getNbrCounterWithNotService(currentProvider);
    }

    public Integer getNbrRecurringChrgNotAssociated() {
        return recurringChargeTemplateService.getNbrRecurringChrgNotAssociated(currentProvider);
    }

    public Integer getNbrTerminationChrgNotAssociated() {
        return oneShotChargeTemplateService.getNbrTerminationChrgNotAssociated(currentProvider);
    }

    public Integer getNbrSubscriptionChrgNotAssociated() {
        return oneShotChargeTemplateService.getNbrSubscriptionChrgNotAssociated(currentProvider);
    }

    public long getNbrScriptInstanceWithError() {
        return scriptInstanceService.countScriptInstancesWithError(currentProvider);
    }

    public ConfigIssuesReportingDTO getReportConfigDto() {
        return reportConfigDto;
    }

    public List<Tax> getTaxesNotAssociatedList() {
        return taxesNotAssociatedList;
    }

    public List<UsageChargeTemplate> getUsagesWithNotPricePlList() {
        return usagesWithNotPricePlList;
    }

    public List<RecurringChargeTemplate> getRecurringWithNotPricePlanList() {
        return recurringWithNotPricePlanList;
    }

    public List<OneShotChargeTemplate> getOneShotChrgWithNotPricePlanList() {
        return oneShotChrgWithNotPricePlanList;
    }

    public List<TradingLanguage> getLanguagesNotAssociatedList() {
        return languagesNotAssociatedList;
    }

    public List<InvoiceCategory> getInvoiceCatNotAssociatedList() {
        return invoiceCatNotAssociatedList;
    }

    public List<InvoiceSubCategory> getInvoiceSubCatNotAssociatedList() {
        return invoiceSubCatNotAssociatedList;
    }

    public List<ServiceTemplate> getServicesWithNotOfferList() {
        return servicesWithNotOfferList;
    }

    public List<UsageChargeTemplate> getUsagesChrgNotAssociatedList() {
        return usagesChrgNotAssociatedList;
    }

    public List<CounterTemplate> getCounterWithNotServicList() {
        return counterWithNotServicList;
    }

    public List<RecurringChargeTemplate> getRecurringNotAssociatedList() {
        return recurringNotAssociatedList;
    }

    public List<OneShotChargeTemplate> getTerminationNotAssociatedList() {
        return terminationNotAssociatedList;
    }

    public List<OneShotChargeTemplate> getSubNotAssociatedList() {
        return subNotAssociatedList;
    }

    public List<CustomScript> getScriptInstanceWithErrorList() {
        return scriptInstanceWithErrorList;
    }

    @Override
    public IPersistenceService<BaseEntity> getPersistenceService() {
        return getPersistenceService();
    }

    @Override
    public String getEditViewName() {
        return "";
    }

	public List<Entry<String, String>> getJaspers() {
		return jaspers;
	}

	public void setJaspers(List<Entry<String, String>> jaspers) {
		this.jaspers = jaspers;
	}


}