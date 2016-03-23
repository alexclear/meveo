/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.payments;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@DiscriminatorValue(value = "I")
public class RecordedInvoice extends AccountOperation {

	private static final long serialVersionUID = 1L;

	@Column(name = "PRODUCTION_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date productionDate;

	@Column(name = "INVOICE_DATE")
	@Temporal(TemporalType.DATE)
	private Date invoiceDate;

	@Column(name = "AMOUNT_WITHOUT_TAX", precision = 23, scale = 12)
	private BigDecimal amountWithoutTax;

	@Column(name = "TAX_AMOUNT", precision = 23, scale = 12)
	private BigDecimal taxAmount;

	@Column(name = "NET_TO_PAY", precision = 23, scale = 12)
	private BigDecimal netToPay;

	@Column(name = "PAYMENT_METHOD")
	@Enumerated(EnumType.STRING)
	private PaymentMethodEnum paymentMethod;

	@Column(name = "PAYMENT_INFO")
	private String paymentInfo;// IBAN for direct debit

	@Column(name = "PAYMENT_INFO1")
	private String paymentInfo1;// bank code

	@Column(name = "PAYMENT_INFO2")
	private String paymentInfo2;// code guichet

	@Column(name = "PAYMENT_INFO3")
	private String paymentInfo3;// Num compte

	@Column(name = "PAYMENT_INFO4")
	private String paymentInfo4;// RIB

	@Column(name = "PAYMENT_INFO5")
	private String paymentInfo5;// bankName
	
	@Column(name = "PAYMENT_INFO6")
	private String paymentInfo6;// bic

	@ManyToOne(optional = true)
	@JoinColumn(name = "DDRequestLOT_ID")
	private DDRequestLOT ddRequestLOT;

	@ManyToOne(optional = true)
	@JoinColumn(name = "DDREQUEST_ITEM_ID")
	private DDRequestItem ddRequestItem;

	@Column(name = "BILLING_ACCOUNT_NAME")
	private String billingAccountName;

	public Date getProductionDate() {
		return productionDate;
	}

	public void setProductionDate(Date productionDate) {
		this.productionDate = productionDate;
	}

	public Date getInvoiceDate() {
		return invoiceDate;
	}

	public void setInvoiceDate(Date invoiceDate) {
		this.invoiceDate = invoiceDate;
	}

	public BigDecimal getAmountWithoutTax() {
		return amountWithoutTax;
	}

	public void setAmountWithoutTax(BigDecimal amountWithoutTax) {
		this.amountWithoutTax = amountWithoutTax;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	public BigDecimal getNetToPay() {
		return netToPay;
	}

	public void setNetToPay(BigDecimal netToPay) {
		this.netToPay = netToPay;
	}

	public PaymentMethodEnum getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethodEnum paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getPaymentInfo() {
		return paymentInfo;
	}

	public void setPaymentInfo(String paymentInfo) {
		this.paymentInfo = paymentInfo;
	}

	public String getPaymentInfo1() {
		return paymentInfo1;
	}

	public void setPaymentInfo1(String paymentInfo1) {
		this.paymentInfo1 = paymentInfo1;
	}

	public String getPaymentInfo2() {
		return paymentInfo2;
	}

	public void setPaymentInfo2(String paymentInfo2) {
		this.paymentInfo2 = paymentInfo2;
	}

	public String getPaymentInfo3() {
		return paymentInfo3;
	}

	public void setPaymentInfo3(String paymentInfo3) {
		this.paymentInfo3 = paymentInfo3;
	}

	public String getPaymentInfo4() {
		return paymentInfo4;
	}

	public void setPaymentInfo4(String paymentInfo4) {
		this.paymentInfo4 = paymentInfo4;
	}

	public void setDdRequestLOT(DDRequestLOT ddRequestLOT) {
		this.ddRequestLOT = ddRequestLOT;
	}

	public DDRequestLOT getDdRequestLOT() {
		return ddRequestLOT;
	}

	public void setBillingAccountName(String billingAccountName) {
		this.billingAccountName = billingAccountName;
	}

	public String getBillingAccountName() {
		return billingAccountName;
	}

	public void setPaymentInfo5(String paymentInfo5) {
		this.paymentInfo5 = paymentInfo5;
	}

	public String getPaymentInfo5() {
		return paymentInfo5;
	}

	public void setDdRequestItem(DDRequestItem ddRequestItem) {
		this.ddRequestItem = ddRequestItem;
	}

	public DDRequestItem getDdRequestItem() {
		return ddRequestItem;
	}

	public String getPaymentInfo6() {
		return paymentInfo6;
	}

	public void setPaymentInfo6(String paymentInfo6) {
		this.paymentInfo6 = paymentInfo6;
	}

	
}
