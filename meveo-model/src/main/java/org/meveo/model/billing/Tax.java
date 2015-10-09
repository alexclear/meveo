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
package org.meveo.model.billing;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.meveo.model.BusinessEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.MultilanguageEntity;
import org.meveo.model.ObservableEntity;

@Entity
@ObservableEntity
@MultilanguageEntity
@ExportIdentifier({ "code", "provider" })
@Table(name = "BILLING_TAX", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE", "PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_TAX_SEQ")
@NamedQueries({			
@NamedQuery(name = "tax.getNbTaxesNotAssociated", 
	           query = "select count(*) from Tax t where t.id not in (select l.tax.id from TaxLanguage l where l.tax.id is not null)"
	         + " and t.id not in (select inv.tax.id from InvoiceSubcategoryCountry inv where inv.tax.id is not null) and t.provider=:provider"),
@NamedQuery(name = "tax.getTaxesNotAssociated", 
	           query = "from Tax t where t.id not in (select l.tax.id from TaxLanguage l where l.tax.id is not null ) "
				+ " and t.id not in (select inv.tax.id from InvoiceSubcategoryCountry inv where inv.tax.id is not null) and t.provider=:provider")	              
	         
	})
public class Tax extends BusinessEntity {
	private static final long serialVersionUID = 1L;

	@Column(name = "ACCOUNTING_CODE", length = 255)
	private String accountingCode;

	@Column(name = "TAX_PERCENTAGE", precision = NB_PRECISION, scale = NB_DECIMALS)
	private BigDecimal percent;
	
	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public BigDecimal getPercent() {
		return percent;
	}

	public void setPercent(BigDecimal percent) {
		this.percent = percent;
	}


	
	

}
