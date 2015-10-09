package org.meveo.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Edward P. Legaspi
 * @since Nov 4, 2013
 **/
@XmlRootElement(name = "UsageCharge")
@XmlAccessorType(XmlAccessType.FIELD)
public class UsageChargeDto implements Serializable {

	private static final long serialVersionUID = -2546246507412182368L;

	private Integer min;
	private Integer max;
	private String currencyCode;
	private Date startDate;
	private Date endDate;
	private BigDecimal price;
	private BigDecimal recommendedPrice;

	public Integer getMin() {
		return min;
	}

	public void setMin(Integer min) {
		this.min = min;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getRecommendedPrice() {
		return recommendedPrice;
	}

	public void setRecommendedPrice(BigDecimal recommendedPrice) {
		this.recommendedPrice = recommendedPrice;
	}

}
