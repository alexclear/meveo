package org.meveo.api.account;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.account.AddressDto;
import org.meveo.api.dto.account.ProviderContactDto;
import org.meveo.api.dto.account.ProviderContactsDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.ProviderContact;
import org.meveo.model.shared.Address;
import org.meveo.service.crm.impl.ProviderContactService;

/**
 * 
 * @author Tyshan　Shi(tyshan@manaty.net)
 * @date Jun 3, 2016 1:28:17 AM
 *
 */
@Stateless
public class ProviderContactApi extends BaseApi {

	@Inject
	private ProviderContactService providerContactService;
	
	public void create(ProviderContactDto providerContactDto,User currentUser) throws MeveoApiException,BusinessException{
		if (StringUtils.isNotEmpty(providerContactDto.getCode())){
			ProviderContact existedProviderContact=providerContactService.findByCode(providerContactDto.getCode(),currentUser.getProvider());
        	if(existedProviderContact!=null){
        		throw new EntityAlreadyExistsException(ProviderContact.class, providerContactDto.getCode());
        	}
        	
			ProviderContact providerContact=new ProviderContact();
			providerContact.setCode(providerContactDto.getCode());
			providerContact.setDescription(providerContactDto.getDescription());
			providerContact.setFirstName(providerContactDto.getFirstName());
			providerContact.setLastName(providerContactDto.getLastName());
			providerContact.setEmail(providerContactDto.getEmail());
			providerContact.setPhone(providerContactDto.getPhone());
			providerContact.setMobile(providerContactDto.getMobile());
			providerContact.setFax(providerContactDto.getFax());
			providerContact.setGenericMail(providerContactDto.getGenericMail());
			if(providerContactDto.getAddressDto()!=null){
				Address address=providerContact.getAddress();
				AddressDto addressDto=providerContactDto.getAddressDto();
				address.setAddress1(addressDto.getAddress1());
				address.setAddress2(addressDto.getAddress2());
				address.setAddress3(addressDto.getAddress3());
				address.setZipCode(addressDto.getZipCode());
				address.setCity(addressDto.getCity());
				address.setCountry(addressDto.getCountry());
				address.setState(addressDto.getState());
			}
			providerContactService.create(providerContact, currentUser);
		}else{
			 if (StringUtils.isBlank(providerContactDto.getCode())) {
	                missingParameters.add("code");
	            }
	            handleMissingParameters();
		}
	}
	public void update(ProviderContactDto providerContactDto,User currentUser) throws MeveoApiException, BusinessException{
		if (StringUtils.isBlank(providerContactDto.getCode())) {
			missingParameters.add("code");
		}
		handleMissingParameters();
		Provider provider = currentUser.getProvider();
		ProviderContact providerContact=providerContactService.findByCode(providerContactDto.getCode(), provider);
		if(providerContact==null){
			throw new EntityDoesNotExistsException(ProviderContact.class,providerContactDto.getCode());
		}
		providerContact.setDescription(providerContactDto.getDescription());
		providerContact.setFirstName(providerContactDto.getFirstName());
		providerContact.setLastName(providerContactDto.getLastName());
		providerContact.setEmail(providerContactDto.getEmail());
		providerContact.setPhone(providerContactDto.getPhone());
		providerContact.setMobile(providerContactDto.getMobile());
		providerContact.setFax(providerContactDto.getFax());
		providerContact.setGenericMail(providerContactDto.getGenericMail());
		if(providerContactDto.getAddressDto()!=null){
			Address address=providerContact.getAddress();
			AddressDto addressDto=providerContactDto.getAddressDto();
			address.setAddress1(addressDto.getAddress1());
			address.setAddress2(addressDto.getAddress2());
			address.setAddress3(addressDto.getAddress3());
			address.setZipCode(addressDto.getZipCode());
			address.setCity(addressDto.getCity());
			address.setCountry(addressDto.getCountry());
			address.setState(addressDto.getState());
		}
		providerContactService.update(providerContact, currentUser);
	}
	public ProviderContactDto find(String code,Provider provider) throws MeveoApiException{
		if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
        }
		handleMissingParameters();
		ProviderContact providerContact=providerContactService.findByCode(code, provider);
		if(providerContact==null){
			throw new EntityDoesNotExistsException(ProviderContact.class,code);
		}
		return new ProviderContactDto(providerContact);
	}
	public void remove(String code,Provider provider) throws MeveoApiException{
		if(StringUtils.isNotEmpty(code)){
			ProviderContact providerContact=providerContactService.findByCode(code, provider);
			if(providerContact==null){
				throw new EntityDoesNotExistsException(ProviderContact.class,code);
			}
			providerContactService.remove(providerContact);
		}else{
			missingParameters.add("code");
			handleMissingParameters();
		}
	}
	public ProviderContactsDto list(Provider provider) throws MeveoApiException{
		ProviderContactsDto result=new ProviderContactsDto();
		List<ProviderContact> providerContacts=providerContactService.list(provider);
		if(providerContacts!=null){
			for(ProviderContact providerContact:providerContacts){
				result.getProviderContacts().add(new ProviderContactDto(providerContact));
			}
		}
		return result;
	}
	public void createOrUpdate(ProviderContactDto providerContactDto,User currentUser) throws MeveoApiException,BusinessException{
		ProviderContact providerContact=providerContactService.findByCode(providerContactDto.getCode(),currentUser.getProvider());
		if(providerContact==null){
			create(providerContactDto,currentUser);
		}else{
			update(providerContactDto,currentUser);
		}
	}
}
