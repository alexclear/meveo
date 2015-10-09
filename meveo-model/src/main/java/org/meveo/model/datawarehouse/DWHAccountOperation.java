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
package org.meveo.model.datawarehouse;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.meveo.model.IEntity;
import org.meveo.model.IVersionedEntity;

@Entity
@Table(name = "DWH_ACCOUNT_OPERATION")
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "DWH_ACCOUNT_OPERATION_SEQ")
public class DWHAccountOperation implements Serializable, IEntity, IVersionedEntity {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(generator = "ID_GENERATOR")
	@Column(name = "ID")
	private Long id;

	@Version
	@Column(name = "VERSION")
	private Integer version;

	@Column(name = "PROVIDER_CODE", length = 20)
	private String providerCode;

	@Column(name = "ACCOUNT_CODE", length = 50)
	private String accountCode;

	@Column(name = "ACCOUNT_DESCRIPTION", length = 255)
	private String accountDescription;

	@Column(name = "ACCOUNTING_CODE")
	private String accountingCode;

	@Column(name = "ACCOUNTING_CODE_CLIENT_SIDE")
	private String accountingCodeClientSide;

	@Column(name = "TRANSACTION_DATE")
	@Temporal(TemporalType.DATE)
	private Date transactionDate;

	@Column(name = "DUE_DATE")
	@Temporal(TemporalType.DATE)
	private Date dueDate;

	@Column(name = "DUE_MONTH")
	private Integer dueMonth;

	@Column(name = "CATEGORY")
	private byte category;

	@Column(name = "TYPE")
	private byte type;

	@Column(name = "OCC_CODE", length = 10)
	private String occCode;

	@Column(name = "OCC_DESCRIPTION", length = 255)
	private String occDescription;

	@Column(name = "REFERENCE", length = 50)
	private String reference;

	@Column(name = "AMOUNT")
	private BigDecimal amount;

	@Column(name = "STATUS")
	private byte status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getProviderCode() {
		return providerCode;
	}

	public void setProviderCode(String providerCode) {
		this.providerCode = providerCode;
	}

	public boolean isTransient() {
		return id == null;
	}

	public String getAccountCode() {
		return accountCode;
	}

	public void setAccountCode(String accountCode) {
		this.accountCode = accountCode;
	}

	public String getAccountDescription() {
		return accountDescription;
	}

	public void setAccountDescription(String accountDescription) {
		this.accountDescription = accountDescription;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public String getAccountingCodeClientSide() {
		return accountingCodeClientSide;
	}

	public void setAccountingCodeClientSide(String accountingCodeClientSide) {
		this.accountingCodeClientSide = accountingCodeClientSide;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Integer getDueMonth() {
		return dueMonth;
	}

	public void setDueMonth(Integer dueMonth) {
		this.dueMonth = dueMonth;
	}

	public byte getCategory() {
		return category;
	}

	public void setCategory(byte category) {
		this.category = category;
	}

	public byte getType() {
		return type;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public String getOccCode() {
		return occCode;
	}

	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	public String getOccDescription() {
		return occDescription;
	}

	public void setOccDescription(String occDescription) {
		this.occDescription = occDescription;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}

}
