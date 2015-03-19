/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.service.billing.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.IncorrectSusbcriptionException;
import org.meveo.admin.exception.UnrolledbackBusinessException;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.User;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.billing.BillingRun;
import org.meveo.model.billing.BillingRunStatusEnum;
import org.meveo.model.billing.CategoryInvoiceAgregate;
import org.meveo.model.billing.Invoice;
import org.meveo.model.billing.InvoiceAgregate;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.model.billing.InvoiceSubcategoryCountry;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.RatedTransactionStatusEnum;
import org.meveo.model.billing.SubCategoryInvoiceAgregate;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.Tax;
import org.meveo.model.billing.TaxInvoiceAgregate;
import org.meveo.model.billing.UserAccount;
import org.meveo.model.billing.WalletInstance;
import org.meveo.service.api.dto.ConsumptionDTO;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.catalog.impl.InvoiceSubCategoryService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.slf4j.Logger;

@Stateless
public class RatedTransactionService extends PersistenceService<RatedTransaction> {

	@Inject
	private Logger log;

	@Inject
	private InvoiceAgregateService invoiceAgregateService;

	@Inject
	private InvoiceSubCategoryService invoiceSubCategoryService;

	@Inject
	private CustomerAccountService customerAccountService;

	public List<RatedTransaction> getRatedTransactionsInvoiced(UserAccount userAccount) {
		if (userAccount == null || userAccount.getWallet() == null) {
			return null;
		}
		return (List<RatedTransaction>) getEntityManager()
				.createNamedQuery("RatedTransaction.listInvoiced", RatedTransaction.class)
				.setParameter("wallet", userAccount.getWallet()).getResultList();
	}

	@SuppressWarnings("unchecked")
	// FIXME: edward please use Named queries
	public ConsumptionDTO getConsumption(Subscription subscription, String infoType, Integer billingCycle,
			boolean sumarizeConsumption) throws IncorrectSusbcriptionException {

		Date lastBilledDate = null;
		ConsumptionDTO consumptionDTO = new ConsumptionDTO();

		// If billing has been run already, use last billing date plus a day as
		// filtering FROM value
		// Otherwise leave it null, so it wont be included in a query
		if (subscription.getUserAccount().getBillingAccount().getBillingRun() != null) {
			lastBilledDate = subscription.getUserAccount().getBillingAccount().getBillingRun().getEndDate();
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(lastBilledDate);
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			lastBilledDate = calendar.getTime();

		}

		if (sumarizeConsumption) {

			QueryBuilder qb = new QueryBuilder("select sum(amount1WithTax), sum(usageAmount) from "
					+ RatedTransaction.class.getSimpleName());
			qb.addCriterionEntity("subscription", subscription);
			qb.addCriterion("subUsageCode1", "=", infoType, false);
			qb.addCriterionDateRangeFromTruncatedToDay("usageDate", lastBilledDate);
			String baseSql = qb.getSqlString();

			// Summarize invoiced transactions
			String sql = baseSql + " and status='BILLED'";

			Query query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			Object[] results = (Object[]) query.getSingleResult();

			consumptionDTO.setAmountCharged((BigDecimal) results[0]);
			consumptionDTO.setConsumptionCharged(((Long) results[1]).intValue());

			// Summarize not invoiced transactions
			sql = baseSql + " and status<>'BILLED'";

			query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			results = (Object[]) query.getSingleResult();

			consumptionDTO.setAmountUncharged((BigDecimal) results[0]);
			consumptionDTO.setConsumptionUncharged(((Long) results[1]).intValue());

		} else {

			QueryBuilder qb = new QueryBuilder(
					"select sum(amount1WithTax), sum(usageAmount), groupingId, case when status='BILLED' then 'true' else 'false' end from "
							+ RatedTransaction.class.getSimpleName());
			qb.addCriterionEntity("subscription", subscription);
			qb.addCriterion("subUsageCode1", "=", infoType, false);
			qb.addCriterionDateRangeFromTruncatedToDay("usageDate", lastBilledDate);
			qb.addSql("groupingId is not null");
			String sql = qb.getSqlString()
					+ " group by groupingId, case when status='BILLED' then 'true' else 'false' end";

			Query query = getEntityManager().createQuery(sql);

			for (Entry<String, Object> param : qb.getParams().entrySet()) {
				query.setParameter(param.getKey(), param.getValue());
			}

			List<Object[]> results = (List<Object[]>) query.getResultList();

			for (Object[] result : results) {

				BigDecimal amount = (BigDecimal) result[0];
				int consumption = ((Long) result[1]).intValue();
				boolean charged = Boolean.parseBoolean((String) result[3]);
				// boolean roaming =
				// RatedTransaction.translateGroupIdToRoaming(groupId);
				// boolean upload =
				// RatedTransaction.translateGroupIdToUpload(groupId);

				if (charged) {

					// if (!roaming && !upload) {
					consumptionDTO.setIncomingNationalConsumptionCharged(consumption);
					// } else if (roaming && !upload) {
					// consumptionDTO.setIncomingRoamingConsumptionCharged(consumption);
					// } else if (!roaming && upload) {
					// consumptionDTO.setOutgoingNationalConsumptionCharged(consumption);
					// } else {
					// consumptionDTO.setOutgoingRoamingConsumptionCharged(consumption);
					// }

					consumptionDTO.setConsumptionCharged(consumptionDTO.getConsumptionCharged() + consumption);
					consumptionDTO.setAmountCharged(consumptionDTO.getAmountCharged().add(amount));

				} else {
					// if (!roaming && !upload) {
					consumptionDTO.setIncomingNationalConsumptionUncharged(consumption);
					// } else if (roaming && !upload) {
					// consumptionDTO.setIncomingRoamingConsumptionUncharged(consumption);
					// } else if (!roaming && upload) {
					// consumptionDTO.setOutgoingNationalConsumptionUncharged(consumption);
					// } else {
					// consumptionDTO.setOutgoingRoamingConsumptionUncharged(consumption);
					// }
					consumptionDTO.setConsumptionUncharged(consumptionDTO.getConsumptionUncharged() + consumption);
					consumptionDTO.setAmountUncharged(consumptionDTO.getAmountUncharged().add(amount));
				}
			}
		}

		return consumptionDTO;

	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public void createInvoiceAndAgregates(BillingAccount billingAccount, Invoice invoice, User currentUser)
			throws BusinessException {
		boolean entreprise = billingAccount.getProvider().isEntreprise();
		int rounding = billingAccount.getProvider().getRounding() == null ? 2 : billingAccount.getProvider()
				.getRounding();
		boolean exoneratedFromTaxes = billingAccount.getCustomerAccount().getCustomer().getCustomerCategory()
				.getExoneratedFromTaxes();
		BigDecimal nonEnterprisePriceWithTax = BigDecimal.ZERO;

		for (UserAccount userAccount : billingAccount.getUsersAccounts()) {
			WalletInstance wallet = userAccount.getWallet();

			// TODO : use Named queries
			CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
			Root from = cq.from(RatedTransaction.class);
			Path<Long> invoiceSubCategoryPath = from.get("invoiceSubCategory").get("id");

			Expression<BigDecimal> amountWithoutTax = cb.sum(from.get("amountWithoutTax"));
			Expression<BigDecimal> amountWithTax = cb.sum(from.get("amountWithTax"));
			Expression<BigDecimal> amountTax = cb.sum(from.get("amountTax"));
			Expression<BigDecimal> quantity = cb.sum(from.get("quantity"));

			CriteriaQuery<Object[]> select = cq.multiselect(invoiceSubCategoryPath, amountWithoutTax, amountWithTax,
					amountTax, quantity);
			// Grouping
			cq.groupBy(invoiceSubCategoryPath);
			// Restrictions (I don't really understand what you're querying)
			Predicate pStatus = cb.equal(from.get("status"), RatedTransactionStatusEnum.OPEN);
			Predicate pWallet = cb.equal(from.get("wallet"), wallet);
			Predicate pAmoutWithoutTax = null;
			if (!billingAccount.getProvider().isDisplayFreeTransacInInvoice()) {
				pAmoutWithoutTax = cb.notEqual(from.get("amountWithoutTax"), BigDecimal.ZERO);
			}

			Predicate pdoNotTriggerInvoicing = cb.isFalse(from.get("doNotTriggerInvoicing"));
			Predicate pInvoice = cb.isNull(from.get("invoice"));
			if (!billingAccount.getProvider().isDisplayFreeTransacInInvoice()) {
				cq.where(pStatus, pWallet, pAmoutWithoutTax, pdoNotTriggerInvoicing, pInvoice);
			} else {
				cq.where(pStatus, pWallet, pdoNotTriggerInvoicing, pInvoice);
			}

			List<InvoiceAgregate> invoiceAgregateSubcatList = new ArrayList<InvoiceAgregate>();
			List<Object[]> invoiceSubCats = getEntityManager().createQuery(cq).getResultList();

			Map<Long, CategoryInvoiceAgregate> catInvoiceAgregateMap = new HashMap<Long, CategoryInvoiceAgregate>();
			Map<Long, TaxInvoiceAgregate> taxInvoiceAgregateMap = new HashMap<Long, TaxInvoiceAgregate>();

			SubCategoryInvoiceAgregate biggestSubCat = null;
			BigDecimal biggestAmount = new BigDecimal("-100000000");

			for (Object[] object : invoiceSubCats) {
				log.info("amountWithoutTax=" + object[1] + "amountWithTax" + object[2] + "amountTax" + object[3]);
				Long invoiceSubCategoryId = (Long) object[0];
				InvoiceSubCategory invoiceSubCategory = invoiceSubCategoryService.findById(invoiceSubCategoryId);
				Tax tax = null;
				for (InvoiceSubcategoryCountry invoicesubcatCountry : invoiceSubCategory
						.getInvoiceSubcategoryCountries()) {
					if (invoicesubcatCountry.getTradingCountry().getCountryCode()
							.equalsIgnoreCase(invoice.getBillingAccount().getTradingCountry().getCountryCode())) {
						tax = invoicesubcatCountry.getTax();
					}
				}

				SubCategoryInvoiceAgregate invoiceAgregateSubcat = new SubCategoryInvoiceAgregate();
				invoiceAgregateSubcat.setAuditable(billingAccount.getAuditable());
				invoiceAgregateSubcat.setProvider(billingAccount.getProvider());
				invoiceAgregateSubcat.setInvoice(invoice);
				invoiceAgregateSubcat.setBillingRun(billingAccount.getBillingRun());
				invoiceAgregateSubcat.setWallet(wallet);
				invoiceAgregateSubcat.setAccountingCode(invoiceSubCategory.getAccountingCode());
				invoiceAgregateSubcat.setSubCategoryTax(tax);
				fillAgregates(invoiceAgregateSubcat, wallet);
				int itemNumber = invoiceAgregateSubcat.getItemNumber() != null ? invoiceAgregateSubcat.getItemNumber() + 1
						: 1;
				invoiceAgregateSubcat.setItemNumber(itemNumber);

				invoiceAgregateSubcat.setAmountWithoutTax((BigDecimal) object[1]);
				invoiceAgregateSubcat.setAmountWithTax((BigDecimal) object[2]);
				invoiceAgregateSubcat.setAmountTax((BigDecimal) object[3]);
				invoiceAgregateSubcat.setQuantity((BigDecimal) object[4]);
				invoiceAgregateSubcat.setProvider(billingAccount.getProvider());
				invoiceAgregateSubcatList.add(invoiceAgregateSubcat);
				// end agregate F

				if (!entreprise) {
					nonEnterprisePriceWithTax = nonEnterprisePriceWithTax.add((BigDecimal) object[2]);
				}

				// start agregate T
				TaxInvoiceAgregate invoiceAgregateTax = null;
				Long taxId = tax.getId();

				if (taxInvoiceAgregateMap.containsKey(taxId)) {
					invoiceAgregateTax = taxInvoiceAgregateMap.get(taxId);
				} else {
					invoiceAgregateTax = new TaxInvoiceAgregate();
					invoiceAgregateTax.setAuditable(billingAccount.getAuditable());
					invoiceAgregateTax.setProvider(billingAccount.getProvider());
					invoiceAgregateTax.setInvoice(invoice);
					invoiceAgregateTax.setBillingRun(billingAccount.getBillingRun());
					invoiceAgregateTax.setTax(tax);
					invoiceAgregateTax.setAccountingCode(tax.getAccountingCode());

					taxInvoiceAgregateMap.put(taxId, invoiceAgregateTax);
				}

				if (tax.getPercent().compareTo(BigDecimal.ZERO) == 0 || exoneratedFromTaxes) {
					invoiceAgregateTax.addAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax());
					invoiceAgregateTax.addAmountWithTax(invoiceAgregateSubcat.getAmountWithTax());
					invoiceAgregateTax.addAmountTax(invoiceAgregateSubcat.getAmountTax());
					invoiceAgregateTax.setTaxPercent(BigDecimal.ZERO);
				}

				fillAgregates(invoiceAgregateTax, wallet);
				if (invoiceAgregateSubcat.getSubCategoryTax().getPercent().compareTo(BigDecimal.ZERO) != 0
						&& !exoneratedFromTaxes) {
					invoiceAgregateTax.setTaxPercent(invoiceAgregateSubcat.getSubCategoryTax().getPercent());
				}
				invoiceAgregateTax.setProvider(billingAccount.getProvider());

				if (invoiceAgregateTax.getId() == null) {
					invoiceAgregateService.create(invoiceAgregateTax, currentUser, currentUser.getProvider());
				}

				invoiceAgregateSubcat.setSubCategoryTax(tax);
				invoiceAgregateSubcat.setInvoiceSubCategory(invoiceSubCategory);

				// start agregate R
				CategoryInvoiceAgregate invoiceAgregateCat = null;
				Long invoiceCategoryId = invoiceSubCategory.getInvoiceCategory().getId();
				if (catInvoiceAgregateMap.containsKey(invoiceCategoryId)) {
					invoiceAgregateCat = catInvoiceAgregateMap.get(invoiceCategoryId);
				} else {
					invoiceAgregateCat = new CategoryInvoiceAgregate();
					invoiceAgregateCat.setAuditable(billingAccount.getAuditable());
					invoiceAgregateCat.setProvider(billingAccount.getProvider());

					invoiceAgregateCat.setInvoice(invoice);
					invoiceAgregateCat.setBillingRun(billingAccount.getBillingRun());
					catInvoiceAgregateMap.put(invoiceCategoryId, invoiceAgregateCat);
				}

				fillAgregates(invoiceAgregateCat, wallet);
				if (invoiceAgregateCat.getId() == null) {
					invoiceAgregateService.create(invoiceAgregateCat, currentUser, currentUser.getProvider());
				}

				invoiceAgregateCat.setInvoiceCategory(invoiceSubCategory.getInvoiceCategory());
				invoiceAgregateCat.setProvider(billingAccount.getProvider());
				invoiceAgregateSubcat.setCategoryInvoiceAgregate(invoiceAgregateCat);
				invoiceAgregateSubcat.setTaxInvoiceAgregate(invoiceAgregateTax);
				// end agregate R

				// round the amount without Tax
				// compute the largest subcategory

				// first we round the amount without tax

				log.debug("subcat " + invoiceAgregateSubcat.getAccountingCode() + " ht="
						+ invoiceAgregateSubcat.getAmountWithoutTax() + " ->"
						+ invoiceAgregateSubcat.getAmountWithoutTax().setScale(rounding, RoundingMode.HALF_UP));
				invoiceAgregateSubcat.setAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax().setScale(
						rounding, RoundingMode.HALF_UP));
				// add it to taxAggregate and CategoryAggregate
				if (invoiceAgregateSubcat.getSubCategoryTax().getPercent().compareTo(BigDecimal.ZERO) != 0
						&& !exoneratedFromTaxes) {
					TaxInvoiceAgregate taxInvoiceAgregate = taxInvoiceAgregateMap.get(invoiceAgregateSubcat
							.getSubCategoryTax().getId());
					taxInvoiceAgregate.addAmountWithoutTax(invoiceAgregateSubcat.getAmountWithoutTax());
					log.info("  tax " + invoiceAgregateSubcat.getTaxInvoiceAgregate().getTaxPercent() + " ht ->"
							+ taxInvoiceAgregate.getAmountWithoutTax());
				}
				invoiceAgregateSubcat.getCategoryInvoiceAgregate().addAmountWithoutTax(
						invoiceAgregateSubcat.getAmountWithoutTax());
				log.debug("  cat " + invoiceAgregateSubcat.getCategoryInvoiceAgregate().getId() + " ht ->"

				+ invoiceAgregateSubcat.getCategoryInvoiceAgregate().getAmountWithoutTax());
				if (invoiceAgregateSubcat.getAmountWithoutTax().compareTo(biggestAmount) > 0) {
					biggestAmount = invoiceAgregateSubcat.getAmountWithoutTax();
					biggestSubCat = invoiceAgregateSubcat;
				}

				invoiceAgregateService.create(invoiceAgregateSubcat, currentUser, currentUser.getProvider());
			}

			// compute the tax
			if (!exoneratedFromTaxes) {
				for (Map.Entry<Long, TaxInvoiceAgregate> taxCatMap : taxInvoiceAgregateMap.entrySet()) {
					TaxInvoiceAgregate taxCat = taxCatMap.getValue();
					if (taxCat.getTax().getPercent().compareTo(BigDecimal.ZERO) != 0) {
						// then compute the tax
						taxCat.setAmountTax(taxCat.getAmountWithoutTax().multiply(taxCat.getTaxPercent())
								.divide(new BigDecimal("100")));
						// then round the tax
						taxCat.setAmountTax(taxCat.getAmountTax().setScale(rounding, RoundingMode.HALF_UP));

						// and compute amount with tax
						taxCat.setAmountWithTax(taxCat.getAmountWithoutTax().add(taxCat.getAmountTax())
								.setScale(rounding, RoundingMode.HALF_UP));
						log.debug("  tax2 ht ->" + taxCat.getAmountWithoutTax());
					} else {
						// compute the percent
						if (taxCat.getAmountTax() != null && taxCat.getAmount() != null
								&& taxCat.getAmount().compareTo(BigDecimal.ZERO) != 0) {
							taxCat.setTaxPercent(taxCat.getAmountTax()
									.divide(taxCat.getAmount(), rounding + 2, RoundingMode.HALF_UP)
									.multiply(new BigDecimal("100")).setScale(rounding, RoundingMode.HALF_UP));
						} else {
							taxCat.setTaxPercent(BigDecimal.ZERO);
						}
					}

				}
			}

			for (Map.Entry<Long, TaxInvoiceAgregate> tax : taxInvoiceAgregateMap.entrySet()) {
				TaxInvoiceAgregate taxInvoiceAgregate = tax.getValue();
				invoice.addAmountTax(taxInvoiceAgregate.getAmountTax().setScale(rounding, RoundingMode.HALF_UP));
				invoice.addAmountWithoutTax(taxInvoiceAgregate.getAmountWithoutTax().setScale(rounding,
						RoundingMode.HALF_UP));
				invoice.addAmountWithTax(taxInvoiceAgregate.getAmountWithTax().setScale(rounding, RoundingMode.HALF_UP));
			}
			BigDecimal balance = BigDecimal.ZERO;
			if (!entreprise && biggestSubCat != null) {
				// TODO log those steps
				BigDecimal delta = nonEnterprisePriceWithTax.subtract(invoice.getAmountWithTax());
				log.debug("delta= " + nonEnterprisePriceWithTax + " - " + invoice.getAmountWithTax() + "=" + delta);
				biggestSubCat.setAmountWithoutTax(biggestSubCat.getAmountWithoutTax().add(delta)
						.setScale(rounding, RoundingMode.HALF_UP));

				TaxInvoiceAgregate invoiceAgregateT = taxInvoiceAgregateMap.get(biggestSubCat.getSubCategoryTax()
						.getId());
				log.debug("  tax3 ht ->" + invoiceAgregateT.getAmountWithoutTax());
				invoiceAgregateT.setAmountWithoutTax(invoiceAgregateT.getAmountWithoutTax().add(delta)
						.setScale(rounding, RoundingMode.HALF_UP));
				log.debug("  tax4 ht ->" + invoiceAgregateT.getAmountWithoutTax());
				CategoryInvoiceAgregate invoiceAgregateR = biggestSubCat.getCategoryInvoiceAgregate();
				invoiceAgregateR.setAmountWithoutTax(invoiceAgregateR.getAmountWithoutTax().add(delta)
						.setScale(rounding, RoundingMode.HALF_UP));

				invoice.setAmountWithoutTax(invoice.getAmountWithoutTax().add(delta)
						.setScale(rounding, RoundingMode.HALF_UP));
				invoice.setAmountWithTax(nonEnterprisePriceWithTax.setScale(rounding, RoundingMode.HALF_UP));
				balance = customerAccountService.customerAccountBalanceDue(null, invoice.getBillingAccount()
						.getCustomerAccount().getCode(), invoice.getDueDate());

				if (balance == null) {
					throw new BusinessException("account balance calculation failed");
				}
			}
			BigDecimal netToPay = BigDecimal.ZERO;
			if (entreprise) {
				netToPay = invoice.getAmountWithTax();
			} else {
				netToPay = invoice.getAmountWithTax().add(balance);
			}
			invoice.setNetToPay(netToPay);
		}
	}

	private void fillAgregates(InvoiceAgregate invoiceAgregate, WalletInstance wallet) {
		invoiceAgregate.setBillingAccount(wallet.getUserAccount().getBillingAccount());
		invoiceAgregate.setUserAccount(wallet.getUserAccount());
		int itemNumber = invoiceAgregate.getItemNumber() != null ? invoiceAgregate.getItemNumber() + 1 : 1;
		invoiceAgregate.setItemNumber(itemNumber);
	}

	public Boolean isBillingAccountBillable(BillingAccount billingAccount) {
		TypedQuery<Long> q = null;

		if (billingAccount.getProvider().isDisplayFreeTransacInInvoice()) {
			q = getEntityManager().createNamedQuery("RatedTransaction.countNotInvoincedDisplayFree", Long.class);
		} else {
			q = getEntityManager().createNamedQuery("RatedTransaction.countNotInvoinced", Long.class);
		}

		long count = q.setParameter("billingAccount", billingAccount).getSingleResult();

		return count > 0 ? true : false;
	}

	public List<RatedTransaction> getRatedTransactions(WalletInstance wallet, Invoice invoice,
			InvoiceSubCategory invoiceSubCategory) {
		long startDate = System.currentTimeMillis();
		QueryBuilder qb = new QueryBuilder("from RatedTransaction c");
		qb.addCriterionEnum("c.status", RatedTransactionStatusEnum.BILLED);
		qb.addCriterionEntity("c.wallet", wallet);
		qb.addCriterionEntity("c.invoice", invoice);
		qb.addCriterionEntity("c.invoiceSubCategory", invoiceSubCategory);

		if (!invoice.getProvider().isDisplayFreeTransacInInvoice()) {
			qb.addCriterion("c.amountWithoutTax", "<>", BigDecimal.ZERO, false);
		}

		qb.addOrderCriterion("c.usageDate", true);

		@SuppressWarnings("unchecked")
		List<RatedTransaction> ratedTransactions = qb.getQuery(getEntityManager()).getResultList();

		log.debug("getRatedTransactions time: " + (System.currentTimeMillis() - startDate));

		return ratedTransactions;

	}

	public int reratedByWalletOperationId(Long id) throws UnrolledbackBusinessException {
		int result = 0;
		List<RatedTransaction> ratedTransactions = (List<RatedTransaction>) getEntityManager()
				.createNamedQuery("RatedTransaction.listByWalletOperationId", RatedTransaction.class)
				.setParameter("walletOperationId", id).getResultList();
		for (RatedTransaction ratedTransaction : ratedTransactions) {
			if (ratedTransaction.getBillingRun() != null
					&& ratedTransaction.getBillingRun().getStatus() != BillingRunStatusEnum.CANCELED) {
				throw new UnrolledbackBusinessException("A rated transaction " + ratedTransaction.getId()
						+ " forbid rerating of wallet operation " + id);
			}
			ratedTransaction.setStatus(RatedTransactionStatusEnum.RERATED);
			result++;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<RatedTransaction> getNotBilledRatedTransactions(Long walletOperationId) {
		QueryBuilder qb = new QueryBuilder("from RatedTransaction c");
		qb.addCriterionEntity("c.walletOperationId", walletOperationId);
		qb.addCriterion("c.status", "!=", RatedTransactionStatusEnum.BILLED, false);
		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			log.warn(e.getMessage());
			return null;
		}

	}

	@SuppressWarnings("unchecked")
	public List<RatedTransaction> getRatedTransactionsByBillingRun(BillingRun BillingRun) {
		QueryBuilder qb = new QueryBuilder("from RatedTransaction c");
		qb.addCriterionEntity("c.billingRun", BillingRun);
		try {
			return (List<RatedTransaction>) qb.getQuery(getEntityManager()).getResultList();
		} catch (NoResultException e) {
			log.warn(e.getMessage());
			return null;
		}

	}

}