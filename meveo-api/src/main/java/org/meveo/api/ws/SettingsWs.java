package org.meveo.api.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.BillingCycleDto;
import org.meveo.api.dto.CalendarDto;
import org.meveo.api.dto.CatMessagesDto;
import org.meveo.api.dto.CountryDto;
import org.meveo.api.dto.CurrencyDto;
import org.meveo.api.dto.CustomFieldTemplateDto;
import org.meveo.api.dto.InvoiceCategoryDto;
import org.meveo.api.dto.InvoiceSubCategoryCountryDto;
import org.meveo.api.dto.InvoiceSubCategoryDto;
import org.meveo.api.dto.LanguageDto;
import org.meveo.api.dto.OccTemplateDto;
import org.meveo.api.dto.ProviderDto;
import org.meveo.api.dto.RoleDto;
import org.meveo.api.dto.SellerDto;
import org.meveo.api.dto.TaxDto;
import org.meveo.api.dto.TerminationReasonDto;
import org.meveo.api.dto.UserDto;
import org.meveo.api.dto.account.ProviderContactDto;
import org.meveo.api.dto.billing.InvoiceTypeDto;
import org.meveo.api.dto.communication.EmailTemplateDto;
import org.meveo.api.dto.communication.MeveoInstanceDto;
import org.meveo.api.dto.response.DescriptionsResponseDto;
import org.meveo.api.dto.response.GetBillingCycleResponse;
import org.meveo.api.dto.response.GetCalendarResponse;
import org.meveo.api.dto.response.GetCountryResponse;
import org.meveo.api.dto.response.GetCurrencyResponse;
import org.meveo.api.dto.response.GetCustomFieldTemplateReponseDto;
import org.meveo.api.dto.response.GetCustomerAccountConfigurationResponseDto;
import org.meveo.api.dto.response.GetCustomerConfigurationResponseDto;
import org.meveo.api.dto.response.GetDescriptionsResponse;
import org.meveo.api.dto.response.GetInvoiceCategoryResponse;
import org.meveo.api.dto.response.GetInvoiceSubCategoryCountryResponse;
import org.meveo.api.dto.response.GetInvoiceSubCategoryResponse;
import org.meveo.api.dto.response.GetInvoiceTypeResponse;
import org.meveo.api.dto.response.GetInvoiceTypesResponse;
import org.meveo.api.dto.response.GetInvoicingConfigurationResponseDto;
import org.meveo.api.dto.response.GetLanguageResponse;
import org.meveo.api.dto.response.GetOccTemplateResponseDto;
import org.meveo.api.dto.response.GetProviderResponse;
import org.meveo.api.dto.response.GetRoleResponse;
import org.meveo.api.dto.response.GetSellerResponse;
import org.meveo.api.dto.response.GetTaxResponse;
import org.meveo.api.dto.response.GetTaxesResponse;
import org.meveo.api.dto.response.GetTerminationReasonResponse;
import org.meveo.api.dto.response.GetTradingConfigurationResponseDto;
import org.meveo.api.dto.response.GetUserResponse;
import org.meveo.api.dto.response.ListCalendarResponse;
import org.meveo.api.dto.response.PermissionResponseDto;
import org.meveo.api.dto.response.SellerCodesResponseDto;
import org.meveo.api.dto.response.SellerResponseDto;
import org.meveo.api.dto.response.account.ProviderContactResponseDto;
import org.meveo.api.dto.response.account.ProviderContactsResponseDto;
import org.meveo.api.dto.response.communication.EmailTemplateResponseDto;
import org.meveo.api.dto.response.communication.EmailTemplatesResponseDto;
import org.meveo.api.dto.response.communication.MeveoInstanceResponseDto;
import org.meveo.api.dto.response.communication.MeveoInstancesResponseDto;

/**
 * @author Edward P. Legaspi
 **/
@SuppressWarnings("deprecation")
@WebService
public interface SettingsWs extends IBaseWs {

    // provider

    @Deprecated
    @WebMethod
    ActionStatus createProvider(@WebParam(name = "provider") ProviderDto postData);

    @Deprecated
    @WebMethod
    GetProviderResponse findProvider(@WebParam(name = "providerCode") String providerCode);

    @Deprecated
    @WebMethod
    ActionStatus updateProvider(@WebParam(name = "provider") ProviderDto postData);

    @Deprecated
    @WebMethod
    ActionStatus createOrUpdateProvider(@WebParam(name = "provider") ProviderDto postData);

    @WebMethod
    ActionStatus updateProviderCF(@WebParam(name = "provider") ProviderDto postData);

    @WebMethod
    GetProviderResponse findProviderCF(@WebParam(name = "providerCode") String providerCode);

    // configuration

    @WebMethod
    GetTradingConfigurationResponseDto findTradingConfiguration(@WebParam(name = "providerCode") String providerCode);

    @WebMethod
    GetInvoicingConfigurationResponseDto findInvoicingConfiguration(@WebParam(name = "providerCode") String providerCode);

    @WebMethod
    GetCustomerConfigurationResponseDto findCustomerConfiguration(@WebParam(name = "providerCode") String providerCode);

    @WebMethod
    GetCustomerAccountConfigurationResponseDto findCustomerAccountConfiguration(@WebParam(name = "providerCode") String providerCode);

    // user

    @WebMethod
    ActionStatus createUser(@WebParam(name = "user") UserDto postData);

    @WebMethod
    ActionStatus updateUser(@WebParam(name = "user") UserDto postData);

    @WebMethod
    ActionStatus removeUser(@WebParam(name = "username") String username);

    @WebMethod
    GetUserResponse findUser(@WebParam(name = "username") String username);

    @WebMethod
    ActionStatus createOrUpdateUser(@WebParam(name = "user") UserDto postData);

    // seller

    @WebMethod
    ActionStatus createSeller(@WebParam(name = "seller") SellerDto postData);

    @WebMethod
    ActionStatus updateSeller(@WebParam(name = "seller") SellerDto postData);

    @WebMethod
    GetSellerResponse findSeller(@WebParam(name = "sellerCode") String sellerCode);

    @WebMethod
    ActionStatus removeSeller(@WebParam(name = "sellerCode") String sellerCode);

    @WebMethod
    SellerResponseDto listSeller();

    @WebMethod
    SellerCodesResponseDto listSellerCodes();

    @WebMethod
    ActionStatus createOrUpdateSeller(@WebParam(name = "seller") SellerDto postData);

    // tradingLanguage

    @Deprecated
    @WebMethod
    ActionStatus createLanguage(@WebParam(name = "language") LanguageDto postData);

    @Deprecated
    @WebMethod
    GetLanguageResponse findLanguage(@WebParam(name = "languageCode") String languageCode);

    @Deprecated
    @WebMethod
    ActionStatus removeLanguage(@WebParam(name = "languageCode") String languageCode);

    @Deprecated
    @WebMethod
    ActionStatus updateLanguage(@WebParam(name = "language") LanguageDto postData);

    @Deprecated
    @WebMethod
    ActionStatus createOrUpdateLanguage(@WebParam(name = "language") LanguageDto postData);

    // tradingCountry
    @Deprecated
    @WebMethod
    ActionStatus createCountry(@WebParam(name = "country") CountryDto countryDto);

    @Deprecated
    @WebMethod
    GetCountryResponse findCountry(@WebParam(name = "countryCode") String countryCode);

    @Deprecated
    @WebMethod
    ActionStatus removeCountry(@WebParam(name = "countryCode") String countryCode, @WebParam(name = "currencyCode") String currencyCode);

    @Deprecated
    @WebMethod
    ActionStatus updateCountry(@WebParam(name = "country") CountryDto countryDto);

    @Deprecated
    @WebMethod
    ActionStatus createOrUpdateCountry(@WebParam(name = "country") CountryDto countryDto);

    // traingCurrency
    @Deprecated
    @WebMethod
    ActionStatus createCurrency(@WebParam(name = "currency") CurrencyDto postData);

    @Deprecated
    @WebMethod
    GetCurrencyResponse findCurrency(@WebParam(name = "currencyCode") String currencyCode);

    @Deprecated
    @WebMethod
    ActionStatus removeCurrency(@WebParam(name = "currencyCode") String currencyCode);

    @Deprecated
    @WebMethod
    ActionStatus updateCurrency(@WebParam(name = "currency") CurrencyDto postData);

    @Deprecated
    @WebMethod
    ActionStatus createOrUpdateCurrency(@WebParam(name = "currency") CurrencyDto postData);

    // tax

    @WebMethod
    ActionStatus createTax(@WebParam(name = "tax") TaxDto postData);

    @WebMethod
    ActionStatus updateTax(@WebParam(name = "tax") TaxDto postData);

    @WebMethod
    GetTaxResponse findTax(@WebParam(name = "taxCode") String taxCode);

    @WebMethod
    ActionStatus removeTax(@WebParam(name = "taxCode") String taxCode);

    @WebMethod
    ActionStatus createOrUpdateTax(@WebParam(name = "tax") TaxDto postData);

    @WebMethod
    GetTaxesResponse listTaxes();

    // invoice category

    @WebMethod
    ActionStatus createInvoiceCategory(@WebParam(name = "invoiceCategory") InvoiceCategoryDto postData);

    @WebMethod
    ActionStatus updateInvoiceCategory(@WebParam(name = "invoiceCategory") InvoiceCategoryDto postData);

    @WebMethod
    GetInvoiceCategoryResponse findInvoiceCategory(@WebParam(name = "invoiceCategoryCode") String invoiceCategoryCode);

    @WebMethod
    ActionStatus removeInvoiceCategory(@WebParam(name = "invoiceCategoryCode") String invoiceCategoryCode);

    @WebMethod
    ActionStatus createOrUpdateInvoiceCategory(@WebParam(name = "invoiceCategory") InvoiceCategoryDto postData);

    // invoice sub category

    @WebMethod
    ActionStatus createInvoiceSubCategory(@WebParam(name = "invoiceSubCategory") InvoiceSubCategoryDto postData);

    @WebMethod
    ActionStatus updateInvoiceSubCategory(@WebParam(name = "invoiceSubCategory") InvoiceSubCategoryDto postData);

    @WebMethod
    ActionStatus createOrUpdateInvoiceSubCategory(@WebParam(name = "invoiceSubCategory") InvoiceSubCategoryDto postData);

    @WebMethod
    GetInvoiceSubCategoryResponse findInvoiceSubCategory(@WebParam(name = "invoiceSubCategoryCode") String invoiceSubCategoryCode);

    @WebMethod
    ActionStatus removeInvoiceSubCategory(@WebParam(name = "invoiceSubCategoryCode") String invoiceSubCategoryCode);

    // invoice sub category country

    @WebMethod
    ActionStatus createInvoiceSubCategoryCountry(@WebParam(name = "invoiceSubCategoryCountry") InvoiceSubCategoryCountryDto postData);

    @WebMethod
    ActionStatus updateInvoiceSubCategoryCountry(@WebParam(name = "invoiceSubCategoryCountry") InvoiceSubCategoryCountryDto postData);

    @WebMethod
    GetInvoiceSubCategoryCountryResponse findInvoiceSubCategoryCountry(@WebParam(name = "invoiceSubCategoryCode") String invoiceSubCategoryCode,
            @WebParam(name = "country") String country);

    @WebMethod
    ActionStatus removeInvoiceSubCategoryCountry(@WebParam(name = "invoiceSubCategoryCode") String invoiceSubCategoryCode, @WebParam(name = "country") String country);

    @WebMethod
    ActionStatus createOrUpdateInvoiceSubCategoryCountry(@WebParam(name = "invoiceSubCategoryCountry") InvoiceSubCategoryCountryDto postData);

    // calendar

    @WebMethod
    ActionStatus createCalendar(@WebParam(name = "calendar") CalendarDto postData);

    @WebMethod
    ActionStatus updateCalendar(@WebParam(name = "calendar") CalendarDto postData);

    @WebMethod
    GetCalendarResponse findCalendar(@WebParam(name = "calendarCode") String calendarCode);

    @WebMethod
    ActionStatus removeCalendar(@WebParam(name = "calendarCode") String calendarCode);

    @WebMethod
    ActionStatus createOrUpdateCalendar(@WebParam(name = "calendar") CalendarDto postData);

    @WebMethod
    ListCalendarResponse listCalendars();

    // billing cycle

    @WebMethod
    ActionStatus createBillingCycle(@WebParam(name = "billingCycle") BillingCycleDto postData);

    @WebMethod
    ActionStatus updateBillingCycle(@WebParam(name = "billingCycle") BillingCycleDto postData);

    @WebMethod
    GetBillingCycleResponse findBillingCycle(@WebParam(name = "billingCycleCode") String billingCycleCode);

    @WebMethod
    ActionStatus removeBillingCycle(@WebParam(name = "billingCycleCode") String billingCycleCode);

    @WebMethod
    ActionStatus createOrUpdateBillingCycle(@WebParam(name = "billingCycle") BillingCycleDto postData);

    // occ template

    @WebMethod
    ActionStatus createOccTemplate(@WebParam(name = "occTemplate") OccTemplateDto postData);

    @WebMethod
    ActionStatus updateOccTemplate(@WebParam(name = "occTemplate") OccTemplateDto postData);

    @WebMethod
    GetOccTemplateResponseDto findOccTemplate(@WebParam(name = "occTemplateCode") String occTemplateCode);

    @WebMethod
    ActionStatus removeOccTemplate(@WebParam(name = "occTemplateCode") String occTemplateCode);

    @WebMethod
    ActionStatus createOrUpdateOccTemplate(@WebParam(name = "occTemplate") OccTemplateDto postData);

    // custom field

    @WebMethod
    ActionStatus createCustomFieldTemplate(@WebParam(name = "customField") CustomFieldTemplateDto postData);

    @WebMethod
    ActionStatus updateCustomFieldTemplate(@WebParam(name = "customField") CustomFieldTemplateDto postData);

    @WebMethod
    ActionStatus removeCustomFieldTemplate(@WebParam(name = "customFieldTemplateCode") String customFieldTemplateCode, @WebParam(name = "appliesTo") String appliesTo);

    @WebMethod
    GetCustomFieldTemplateReponseDto findCustomFieldTemplate(@WebParam(name = "customFieldTemplateCode") String customFieldTemplateCode,
            @WebParam(name = "appliesTo") String appliesTo);

    @WebMethod
    ActionStatus createOrUpdateCustomFieldTemplate(@WebParam(name = "customField") CustomFieldTemplateDto postData);

    // permission

    @WebMethod
    PermissionResponseDto listPermissions();

    // role

    @WebMethod
    ActionStatus createRole(@WebParam(name = "role") RoleDto postData);

    @WebMethod
    ActionStatus updateRole(@WebParam(name = "role") RoleDto postData);

    @WebMethod
    ActionStatus removeRole(@WebParam(name = "role") String name, @WebParam(name = "provider") String provider);

    @WebMethod
    GetRoleResponse findRole(@WebParam(name = "roleName") String name, @WebParam(name = "provider") String provider);

    @WebMethod
    GetRoleResponse findRole4_2(@WebParam(name = "roleName") String name);

    @WebMethod
    ActionStatus createOrUpdateRole(@WebParam(name = "role") RoleDto postData);

    // descriptions

    @WebMethod
    ActionStatus createDescriptions(@WebParam(name = "descriptions") CatMessagesDto postData);

    @WebMethod
    ActionStatus updateDescriptions(@WebParam(name = "descriptions") CatMessagesDto postData);

    @WebMethod
    GetDescriptionsResponse findDescriptions(@WebParam(name = "entityClass") String entityClass, @WebParam(name = "code") String code,
            @WebParam(name = "languageCode") String languageCode);

    @WebMethod
    ActionStatus removeDescriptions(@WebParam(name = "entityClass") String entityClass, @WebParam(name = "code") String code, @WebParam(name = "languageCode") String languageCode);

    @WebMethod
    ActionStatus createOrUpdateDescriptions(@WebParam(name = "descriptions") CatMessagesDto postData);

    @WebMethod
    DescriptionsResponseDto listDescriptions();

    /* termination reasons */

    @WebMethod
    ActionStatus createTerminationReason(@WebParam(name = "terminationReason") TerminationReasonDto postData);

    @WebMethod
    ActionStatus updateTerminationReason(@WebParam(name = "terminationReason") TerminationReasonDto postData);

    @WebMethod
    ActionStatus createOrUpdateTerminationReason(@WebParam(name = "terminationReason") TerminationReasonDto postData);

    @WebMethod
    ActionStatus removeTerminationReason(@WebParam(name = "terminationReasonCode") String code);

    @WebMethod
    GetTerminationReasonResponse findTerminationReason(@WebParam(name = "terminationReasonCode") String code);

    @WebMethod
    GetTerminationReasonResponse listTerminationReason();

    // InvoiceType
    @WebMethod
    ActionStatus createInvoiceType(@WebParam(name = "invoiceType") InvoiceTypeDto invoiceTypeDto);

    @WebMethod
    ActionStatus updateInvoiceType(@WebParam(name = "invoiceType") InvoiceTypeDto invoiceTypeDto);

    @WebMethod
    GetInvoiceTypeResponse findInvoiceType(@WebParam(name = "invoiceTypeCode") String invoiceTypeCode);

    @WebMethod
    ActionStatus removeInvoiceType(@WebParam(name = "invoiceTypeCode") String invoiceTypeCode);

    @WebMethod
    ActionStatus createOrUpdateInvoiceType(@WebParam(name = "invoiceType") InvoiceTypeDto invoiceTypeDto);

    @WebMethod
    GetInvoiceTypesResponse listInvoiceTypes();
    
    /**
     * create a providerContact by dto
     * @param providerContactDto
     * @return
     */
    @WebMethod
    ActionStatus createProviderContact(@WebParam(name="providerContact")ProviderContactDto providerContactDto);

    /**
     * update a providerContact by dto
     * @param providerContactDto
     * @return
     */
    @WebMethod
    ActionStatus updateProviderContact(@WebParam(name="providerContact")ProviderContactDto providerContactDto);

    /**
     * find a providerContact by code
     * @param providerContactCode
     * @return
     */
    @WebMethod
    ProviderContactResponseDto findProviderContact(@WebParam(name="providerContactCode") String providerContactCode);

    /**
     * remove a providerContact by code
     * @param providerContactCode
     * @return
     */
    @WebMethod
    ActionStatus removeProviderContact(@WebParam(name="providerContactCode") String providerContactCode);

    /**
     * list all providerContacts
     * @return
     */
    @WebMethod
    ProviderContactsResponseDto listProviderContacts();
    
    /**
     * createOrUpdate a providerContact by dto
     * @param providerContactDto
     * @return
     */
    @WebMethod
    ActionStatus createOrUpdateProviderContact(@WebParam(name="providerContact")ProviderContactDto providerContactDto);
    
    /**
     * create an emailTemplate by dto
     * @param emailTemplateDto
     * @return
     */
    @WebMethod
    ActionStatus createEmailTemplate(@WebParam(name="emailTemplate")EmailTemplateDto emailTemplateDto);

    /**
     * update an emailTemplate by dto
     * @param emailTemplateDto
     * @return
     */
    @WebMethod
    ActionStatus updateEmailTemplate(@WebParam(name="emailTemplate")EmailTemplateDto emailTemplateDto);

    /**
     * find an emailTemplate by code
     * @param emailTemplateCode
     * @return
     */
    @WebMethod
    EmailTemplateResponseDto findEmailTemplate(@WebParam(name="emailTemplateCode") String emailTemplateCode);

    /**
     * remove an emailTemplate by code
     * @param emailTemplateCode
     * @return
     */
    @WebMethod
    ActionStatus removeEmailTemplate(@WebParam(name="emailTemplateCode") String emailTemplateCode);

    /**
     * list emailTemplates
     * @return
     */
    @WebMethod
    EmailTemplatesResponseDto listEmailTemplates();

    /**
     * createOrUpdate an emailTemplate by dto
     * @param emailTemplateDto
     * @return
     */
    @WebMethod
    ActionStatus createOrUpdateEmailTemplate(@WebParam(name="emailTemplate")EmailTemplateDto emailTemplateDto);
    
    /**
     * create a meveoInstance by dto
     * @param meveoInstanceDto
     * @return
     */
    @WebMethod
    ActionStatus createMeveoInstance(@WebParam(name="meveoInstance")MeveoInstanceDto meveoInstanceDto);

    /**
     * update a mveoInstance by dto
     * @param meveoInstanceDto
     * @return
     */
    @WebMethod
    ActionStatus updateMeveoInstance(@WebParam(name="meveoInstance")MeveoInstanceDto meveoInstanceDto);

    /**
     * find a meveoInstance by code
     * @param meveoInstanceCode
     * @return
     */
    @WebMethod
    MeveoInstanceResponseDto findMeveoInstance(@WebParam(name="meveoInstanceCode") String meveoInstanceCode);

    /**
     * remove a meveoInstance by code
     * @param meveoInstanceCode
     * @return
     */
    @WebMethod
    ActionStatus removeMeveoInstance(@WebParam(name="meveoInstanceCode") String meveoInstanceCode);

    /**
     * list meveoInstances
     * @return
     */
    @WebMethod
    MeveoInstancesResponseDto listMeveoInstances();
    
    /**
     * createOrUpdate meveoInstance by dto
     * @param meveoInstanceDto
     * @return
     */
    @WebMethod
    ActionStatus createOrUpdateMeveoInstance(@WebParam(name="meveoInstance")MeveoInstanceDto meveoInstanceDto);
}
