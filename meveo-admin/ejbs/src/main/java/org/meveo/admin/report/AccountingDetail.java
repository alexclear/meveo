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
package org.meveo.admin.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.bi.OutputFormatEnum;
import org.meveo.model.bi.Report;
import org.meveo.model.crm.Provider;
import org.meveo.model.datawarehouse.DWHAccountOperation;
import org.meveo.model.payments.AccountOperation;
import org.meveo.model.payments.CustomerAccount;
import org.meveo.service.payments.impl.AccountOperationService;
import org.meveo.service.payments.impl.CustomerAccountService;
import org.meveo.service.reporting.impl.DWHAccountOperationService;
import org.slf4j.Logger;

@Named
public class AccountingDetail extends FileProducer implements Reporting {
	@Inject
	protected Logger log;

	@Inject
	private CustomerAccountService customerAccountService;
	
	@Inject
	private DWHAccountOperationService accountOperationTransformationService;
	
	@Inject
	private AccountOperationService accountOperationService;

	private String reportsFolder;
	private String templateFilename;
	public Map<String, Object> parameters = new HashMap<String, Object>();
	public HashMap<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
	public HashMap<String, String> customerNames = new HashMap<String, String>();

	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

	public void generateAccountingDetailFile(String providerCode, Date startDate, Date endDate,
			OutputFormatEnum outputFormat) {
		try {
			File file = null;
			if (outputFormat == OutputFormatEnum.PDF) {
				file = File.createTempFile("tempAccountingDetail", ".csv");
			} else if (outputFormat == OutputFormatEnum.CSV) {
				StringBuilder sb = new StringBuilder(getFilename(providerCode));
				sb.append(".csv");
				file = new File(sb.toString());
			}
			FileWriter writer = new FileWriter(file);
			writer.append("N° compte client;Nom du compte client;Code operation;Référence comptable;Date de l'opération;Date d'exigibilité;Débit;Credit;Solde client");
			writer.append('\n');
			List<DWHAccountOperation> list = accountOperationTransformationService
					.getAccountingDetailRecords(providerCode, new Date());
			Iterator<DWHAccountOperation> itr = list.iterator();
			String previousAccountCode = null;
			BigDecimal solde = BigDecimal.ZERO;
			BigDecimal amount = BigDecimal.ZERO;
			while (itr.hasNext()) {
				DWHAccountOperation accountOperationTransformation = itr.next();
				if (previousAccountCode != null) {
					if (!previousAccountCode
							.equals(accountOperationTransformation.getAccountCode())) {
						writer.append(String.valueOf(solde).replace('.', ','));
						solde = BigDecimal.ZERO;
					}
					writer.append('\n');
				}
				if (accountOperationTransformation.getStatus() == 2) {
					AccountOperation accountOperation = accountOperationService
							.findByIdNoCheck(accountOperationTransformation.getId());
					amount = accountOperation.getUnMatchingAmount();
				} else {
					amount = accountOperationTransformation.getAmount();
				}

				amount = accountOperationTransformation.getAmount();
				if (accountOperationTransformation.getCategory() == 1) {
					solde = solde.subtract(amount);
				} else {
					solde = solde.add(amount);
				}
				previousAccountCode = accountOperationTransformation.getAccountCode();
				writer.append(accountOperationTransformation.getAccountCode() + ";"); // Num
																						// compte
																						// client
				writer.append(accountOperationTransformation.getAccountDescription() + ";");
				writer.append(accountOperationTransformation.getOccCode() + ";");
				writer.append(accountOperationTransformation.getReference() + ";");
				writer.append(sdf.format(accountOperationTransformation.getTransactionDate()) + ";");
				writer.append(sdf.format(accountOperationTransformation.getDueDate()) + ";");
				if (accountOperationTransformation.getCategory() == 0)
					writer.append((amount + ";").replace('.', ','));
				else
					writer.append("0;");
				if (accountOperationTransformation.getCategory() == 1)
					writer.append((amount + ";").replace('.', ','));
				else
					writer.append("0;");

			}
			writer.append(String.valueOf(solde).replace('.', ','));
			writer.append('\n');

			writer.flush();
			writer.close();
			if (outputFormat == OutputFormatEnum.PDF) {
				parameters.put("startDate", startDate);
				parameters.put("endDate", endDate);
				parameters.put("provider", providerCode);

				StringBuilder sb = new StringBuilder(getFilename(providerCode));
				sb.append(".pdf");
				generatePDFfile(file, sb.toString(), templateFilename, parameters);
			}
		} catch (IOException e) {
			log.error("failed to generate accounting detail File",e);
		}

	}

	public String getCustomerName(String customerAccountCode,Provider provider) {
		String result = "";
		if (customerNames.containsKey(customerAccountCode)) {
			result = customerNames.get(customerAccountCode);
		} else {
			CustomerAccount account = customerAccountService.findByCode(customerAccountCode,provider);
			if (account.getName() != null) {
				result = account.getName().getTitle().getCode();
				if (account.getName().getFirstName() != null) {
					result += " " + account.getName().getFirstName();
				}
				if (account.getName().getLastName() != null) {
					result += " " + account.getName().getLastName();
				}
			}
		}
		return result;
	}

	public BigDecimal getCustomerBalanceDue(String customerAccountCode, Date atDate) {
		BigDecimal result = BigDecimal.ZERO;
		if (balances.containsKey(customerAccountCode)) {
			result = balances.get(customerAccountCode);
		} else {
			try {
				result = customerAccountService.customerAccountBalanceDue(null,
						customerAccountCode, atDate);
				balances.put(customerAccountCode, result);
			} catch (BusinessException e) {
				log.error("Error while getting balance dues", e);
			}
		}
		return result;
	}

	public String getFilename(String providerName) {

		String DATE_FORMAT = "dd-MM-yyyy";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		StringBuilder sb = new StringBuilder();
		sb.append(reportsFolder);
		sb.append(providerName + "_");
		sb.append("INVENTAIRE_CCLIENT_");
		sb.append(sdf.format(new Date()).toString());
		return sb.toString();
	}

	public void export(Report report) {
		ParamBean param = ParamBean.getInstance();
		reportsFolder = param.getProperty("reportsURL","/opt/jboss/files/reports/");
		String jasperTemplatesFolder = param.getProperty("reports.jasperTemplatesFolder","/opt/jboss/files/reports/JasperTemplates/");
		templateFilename = jasperTemplatesFolder + "accountingDetail.jasper";
		accountOperationTransformationService = null;
		customerAccountService = null; 
		accountOperationService = null; 

		generateAccountingDetailFile(report.getProvider() == null ? null : report.getProvider()
				.getCode(), report.getStartDate(), report.getEndDate(), report.getOutputFormat());

	}

}
