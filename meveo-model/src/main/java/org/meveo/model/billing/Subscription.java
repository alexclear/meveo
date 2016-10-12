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
package org.meveo.model.billing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.ObservableEntity;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.model.mediation.Access;

/**
 * Subscription
 */
@Entity
@ObservableEntity
@CustomFieldEntity(cftCodePrefix = "SUB")
@ExportIdentifier({ "code", "provider" })
@Table(name = "BILLING_SUBSCRIPTION", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE", "PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "BILLING_SUBSCRIPTION_SEQ")
public class Subscription extends BusinessCFEntity{

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "OFFER_ID")
	private OfferTemplate offer;

	@Enumerated(EnumType.STRING)
	@Column(name = "STATUS")
	private SubscriptionStatusEnum status = SubscriptionStatusEnum.CREATED;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "STATUS_DATE")
	private Date statusDate = new Date();;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "SUBSCRIPTION_DATE")
	private Date subscriptionDate = new Date();

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "TERMINATION_DATE")
	private Date terminationDate;

	@OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
	// TODO : Add orphanRemoval annotation.
	// @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();

	@OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
	// TODO : Add orphanRemoval annotation.
	// @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private List<ProductInstance> productInstances = new ArrayList<ProductInstance>();

	@OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY)
	private List<Access> accessPoints = new ArrayList<Access>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "USER_ACCOUNT_ID", nullable = false)
	@NotNull
	private UserAccount userAccount;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "END_AGREMENT_DATE")
	private Date endAgreementDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SUB_TERMIN_REASON_ID")
	private SubscriptionTerminationReason subscriptionTerminationReason;

	@Column(name = "DEFAULT_LEVEL")
	private Boolean defaultLevel = true;

	public Date getEndAgreementDate() {
		return endAgreementDate;
	}

	public void setEndAgreementDate(Date endAgreementDate) {
		this.endAgreementDate = endAgreementDate;
	}

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}

	public List<ServiceInstance> getServiceInstances() {
		return serviceInstances;
	}

	public void setServiceInstances(List<ServiceInstance> serviceInstances) {
		this.serviceInstances = serviceInstances;
	}

	public List<ProductInstance> getProductInstances() {
		return productInstances;
	}

	public void setProductInstances(List<ProductInstance> productInstances) {
		this.productInstances = productInstances;
	}

	public OfferTemplate getOffer() {
		return offer;
	}

	public void setOffer(OfferTemplate offer) {
		this.offer = offer;
	}

	public SubscriptionStatusEnum getStatus() {
		return status;
	}

	public void setStatus(SubscriptionStatusEnum status) {
		if(this.status!=status){
			this.statusDate = new Date();
		}
		this.status = status;
	}

	public Date getStatusDate() {
		return statusDate;
	}

	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	public Date getSubscriptionDate() {
		return subscriptionDate;
	}

	public void setSubscriptionDate(Date subscriptionDate) {
		this.subscriptionDate = subscriptionDate;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public SubscriptionTerminationReason getSubscriptionTerminationReason() {
		return subscriptionTerminationReason;
	}

	public void setSubscriptionTerminationReason(SubscriptionTerminationReason subscriptionTerminationReason) {
		this.subscriptionTerminationReason = subscriptionTerminationReason;
	}

	public List<Access> getAccessPoints() {
		return accessPoints;
	}

	public void setAccessPoints(List<Access> accessPoints) {
		this.accessPoints = accessPoints;
	}

	public Boolean getDefaultLevel() {
		return defaultLevel;
	}

	public void setDefaultLevel(Boolean defaultLevel) {
		this.defaultLevel = defaultLevel;
	}

    @Override
    public ICustomFieldEntity[] getParentCFEntities() {
        return new ICustomFieldEntity[]{offer,userAccount};
	}
}