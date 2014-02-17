/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.service.billing.impl;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.Seller;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.Invoice;
import org.meveo.model.crm.Provider;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.admin.impl.SellerService;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.crm.impl.ProviderService;

@Stateless @LocalBean
public class InvoiceService extends PersistenceService<Invoice> {

	@EJB
	private ProviderService providerService;
	
	@EJB
	private SellerService sellerService;

	public Invoice getInvoiceByNumber(String invoiceNumber, String providerCode)
			throws BusinessException {
		try {
			Query q = getEntityManager().createQuery(
					"from Invoice where invoiceNumber = :invoiceNumber and provider=:provider");
			q.setParameter("invoiceNumber", invoiceNumber).setParameter("provider",
					providerService.findByCode(providerCode));
			Object invoiceObject = q.getSingleResult();
			return (Invoice) invoiceObject;
		} catch (NoResultException e) {
			log.info("Invoice with invoice number #0 was not found. Returning null.", invoiceNumber);
			return null;
		} catch (NonUniqueResultException e) {
			log.info("Multiple invoices with invoice number #0 was found. Returning null.",
					invoiceNumber);
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public Invoice getInvoice(EntityManager em,String invoiceNumber, CustomerAccount customerAccount)
			throws BusinessException {
		try {
			Query q = em.createQuery(
					"from Invoice where invoiceNumber = :invoiceNumber and billingAccount.customerAccount=:customerAccount");
			q.setParameter("invoiceNumber", invoiceNumber).setParameter("customerAccount", customerAccount);
			Object invoiceObject = q.getSingleResult();
			return (Invoice) invoiceObject;
		} catch (NoResultException e) {
			log.info("Invoice with invoice number #0 was not found. Returning null.", invoiceNumber);
			return null;
		} catch (NonUniqueResultException e) {
			log.info("Multiple invoices with invoice number #0 was found. Returning null.",
					invoiceNumber);
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public Invoice getInvoiceByNumber(String invoiceNumber) throws BusinessException {
		try {
			Query q = getEntityManager().createQuery(
					"from Invoice where invoiceNumber = :invoiceNumber");
			q.setParameter("invoiceNumber", invoiceNumber);
			Object invoiceObject = q.getSingleResult();
			return (Invoice) invoiceObject;
		} catch (NoResultException e) {
			log.info("Invoice with invoice number #0 was not found. Returning null.", invoiceNumber);
			return null;
		} catch (NonUniqueResultException e) {
			log.info("Multiple invoices with invoice number #0 was found. Returning null.",
					invoiceNumber);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Invoice> getInvoices(BillingRun billingRun) throws BusinessException {
		try {
			Query q = getEntityManager().createQuery("from Invoice where billingRun = :billingRun");
			q.setParameter("billingRun", billingRun);
			List<Invoice> invoices = q.getResultList();
			return invoices;
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Invoice> getInvoices(BillingAccount billingAccount, String invoiceType)
			throws BusinessException {
		try {
			Query q = getEntityManager()
					.createQuery(
							"from Invoice where billingAccount = :billingAccount and invoiceType=:invoiceType");
			q.setParameter("billingAccount", billingAccount);
			q.setParameter("invoiceType", invoiceType);
			List<Invoice> invoices = q.getResultList();
			log.info("getInvoices: founds #0 invoices with BA_code={} and type=#2 ",
					invoices.size(), billingAccount.getCode(), invoiceType);
			return invoices;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setInvoiceNumber(Invoice invoice){
		Seller seller = invoice.getBillingAccount().getCustomerAccount().getCustomer().getSeller();
		String prefix = seller.getInvoicePrefix();
		if(prefix==null){
			prefix=seller.getProvider().getInvoicePrefix();
		}
		if(prefix==null){
			prefix="";
		}
        long nextInvoiceNb = getNextValue(seller);
        StringBuffer num1 = new StringBuffer("000000000");
        num1.append(nextInvoiceNb + "");
        String invoiceNumber = num1.substring(num1.length() - 9);
        invoice.setInvoiceNumber(prefix + invoiceNumber);
        update(invoice);
	}
	 

	  public synchronized long getNextValue(Seller seller) {
	        long result = 0;
	        if (seller != null) {
	        	if(seller.getCurrentInvoiceNb() != null){
	            long currentInvoiceNbre = seller.getCurrentInvoiceNb();
	            result = 1 + currentInvoiceNbre;
	            seller.setCurrentInvoiceNb(result);
	            sellerService.update(seller,seller.getAuditable().getCreator());
	        	} else {
	        		result=getNextValue(seller.getProvider());
	        	}
	        }
	        return result;
	  }
	  
	  public synchronized long getNextValue(Provider provider) {
	        long result = 0;
	        if (provider != null) {
	            long currentInvoiceNbre = provider.getCurrentInvoiceNb() != null ? provider.getCurrentInvoiceNb() : 0;
	            result = 1 + currentInvoiceNbre;
	            provider.setCurrentInvoiceNb(result);
	            providerService.update(provider);
	        }
	        return result;
	  }
	  
	  @SuppressWarnings("unchecked")
		public List<Invoice> getValidatedInvoicesWithNoPdf(BillingRun br) {
			try {
				QueryBuilder qb = new QueryBuilder(Invoice.class, "i");
				qb.addCriterionEntity("i.billingRun.status",BillingRunStatusEnum.VALIDATED);
				qb.addSql("i.pdf is null");
				if(br!=null){
					qb.addCriterionEntity("i.billingRun", br);
				}
				return (List<Invoice>)qb.getQuery(getEntityManager()).getResultList();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
	  @SuppressWarnings("unchecked")
		public List<Invoice> getInvoicesWithNoAccountOperation(BillingRun br) {
			try {
				QueryBuilder qb = new QueryBuilder(Invoice.class, "i");
				qb.addCriterionEntity("i.billingRun.status",BillingRunStatusEnum.VALIDATED);
				qb.addSql("i.recordedInvoice is null");
				if(br!=null){
					qb.addCriterionEntity("i.billingRun", br);
				}
				return (List<Invoice>)qb.getQuery(getEntityManager()).getResultList();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
}
