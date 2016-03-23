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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.03 at 11:45:33 PM WET 
//


package org.meveo.model.jaxb.subscription;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.meveo.model.billing.ChargeInstance;
import org.meveo.model.shared.DateUtils;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}subscriptionDate"/>
 *         &lt;element ref="{}status"/>
 *         &lt;element ref="{}quantity" minOccurs="0"/>
 *         &lt;element ref="{}recurringCharges" minOccurs="0"/>
 *         &lt;element ref="{}subscriptionCharges" minOccurs="0"/>
 *         &lt;element ref="{}terminationCharges" minOccurs="0"/>
 *         &lt;element ref="{}usageCharges" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="code" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "subscriptionDate",
    "status",
    "quantity",
    "recurringCharges",
    "subscriptionCharges",
    "terminationCharges",
    "usageCharges"
})
@XmlRootElement(name = "serviceInstance")
public class ServiceInstance {

    @XmlElement(required = true)
    protected String subscriptionDate;
    @XmlElement(required = true)
    protected Status status;
    protected String quantity;
    protected List<Charge> recurringCharges;
    protected List<Charge> subscriptionCharges;
    protected List<Charge> terminationCharges;
    protected List<Charge> usageCharges;
    @XmlAttribute(name = "code")
    protected String code;

	public ServiceInstance() {}

    public ServiceInstance(org.meveo.model.billing.ServiceInstance serv,String dateFormat) {
    	if(serv!=null){
			subscriptionDate=serv.getSubscriptionDate()==null?null:
	    		DateUtils.formatDateWithPattern(serv.getSubscriptionDate(), dateFormat);
			status = new Status(serv,dateFormat);
			quantity= serv.getQuantity()==null?null:(""+serv.getQuantity());
			code = serv.getCode();
			if(serv.getRecurringChargeInstances()!=null && serv.getRecurringChargeInstances().size()>0){
				recurringCharges = new ArrayList<Charge>(serv.getRecurringChargeInstances().size());
				for(ChargeInstance charge:serv.getRecurringChargeInstances()){
					recurringCharges.add(new Charge(charge));
				}
			}
			if(serv.getSubscriptionChargeInstances()!=null && serv.getSubscriptionChargeInstances().size()>0){
				subscriptionCharges = new ArrayList<Charge>(serv.getSubscriptionChargeInstances().size());
				for(ChargeInstance charge:serv.getSubscriptionChargeInstances()){
					subscriptionCharges.add(new Charge(charge));
				}
			}
			if(serv.getTerminationChargeInstances()!=null && serv.getTerminationChargeInstances().size()>0){
				terminationCharges = new ArrayList<Charge>(serv.getTerminationChargeInstances().size());
				for(ChargeInstance charge:serv.getTerminationChargeInstances()){
					terminationCharges.add(new Charge(charge));
				}
			}
			if(serv.getUsageChargeInstances()!=null && serv.getUsageChargeInstances().size()>0){
				usageCharges = new ArrayList<Charge>(serv.getUsageChargeInstances().size());
				for(ChargeInstance charge:serv.getUsageChargeInstances()){
					usageCharges.add(new Charge(charge));
				}
			}
			
    	}
	}

	/**
     * Gets the value of the subscriptionDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubscriptionDate() {
        return subscriptionDate;
    }

    /**
     * Sets the value of the subscriptionDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubscriptionDate(String value) {
        this.subscriptionDate = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link Status }
     *     
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link Status }
     *     
     */
    public void setStatus(Status value) {
        this.status = value;
    }

    /**
     * Gets the value of the quantity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQuantity() {
        return quantity;
    }

    /**
     * Sets the value of the quantity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQuantity(String value) {
        this.quantity = value;
    }

	/**
     * Gets the value of the code property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCode(String value) {
        this.code = value;
    }

	public List<Charge> getRecurringCharges() {
		return recurringCharges;
	}

	public void setRecurringCharges(List<Charge> recurringCharges) {
		this.recurringCharges = recurringCharges;
	}

	public List<Charge> getSubscriptionCharges() {
		return subscriptionCharges;
	}

	public void setSubscriptionCharges(List<Charge> subscriptionCharges) {
		this.subscriptionCharges = subscriptionCharges;
	}

	public List<Charge> getTerminationCharges() {
		return terminationCharges;
	}

	public void setTerminationCharges(List<Charge> terminationCharges) {
		this.terminationCharges = terminationCharges;
	}

	public List<Charge> getUsageCharges() {
		return usageCharges;
	}

	public void setUsageCharges(List<Charge> usageCharges) {
		this.usageCharges = usageCharges;
	}

}
