package org.meveo.model.catalog;

/**
 * @author Edward P. Legaspi
 **/
public enum OfferTemplateStatusEnum {
	DRAFT, PUBLISHED, NOT_PUBLISHED;

	public String getLabel() {
		return "offerTemplate.status." + name();
	}
}