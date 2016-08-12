package org.meveo.model.catalog;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.meveo.model.BusinessCFEntity;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.ObservableEntity;

/**
 * @author Edward P. Legaspi
 **/
@Entity
@ObservableEntity
@CustomFieldEntity(cftCodePrefix = "OFFER_CATEGORY")
@ExportIdentifier({ "code", "provider" })
@Table(name = "CAT_OFFER_TEMPLATE_CATEGORY", uniqueConstraints = @UniqueConstraint(columnNames = { "CODE",
		"PROVIDER_ID" }))
@SequenceGenerator(name = "ID_GENERATOR", sequenceName = "CAT_OFFER_TEMPLATE_CATEGORY_SEQ")
public class OfferTemplateCategory extends BusinessCFEntity {

	private static final long serialVersionUID = -5088201294684394309L;

	@Column(name = "NAME", nullable = false, length = 100)
	@Size(max = 100)
	@NotNull
	private String name;

	@Column(name = "IMAGE")
	@Basic(fetch = FetchType.LAZY)
	private byte[] image;

	@Column(name = "IMAGE_CONTENT_TYPE", length = 50)
	@Size(max = 50)
	private String imageContentType;

	@ManyToOne
	@JoinColumn(name = "OFFER_TEMPLATE_CATEGORY_ID")
	private OfferTemplateCategory offerTemplateCategory;

	@OneToMany(mappedBy = "offerTemplateCategory")
	private List<OfferTemplateCategory> children;

	@Column(name = "LEVEL")
	private int level = 1;

	@Override
	public ICustomFieldEntity[] getParentCFEntities() {
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	public OfferTemplateCategory getOfferTemplateCategory() {
		return offerTemplateCategory;
	}

	public void setOfferTemplateCategory(OfferTemplateCategory offerTemplateCategory) {
		this.offerTemplateCategory = offerTemplateCategory;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public void setImageContentType(String imageContentType) {
		this.imageContentType = imageContentType;
	}

	@Transient
	public String getCodeInLevel() {
		if (level == 2) {
			return "-" + code;
		} else if (level == 2) {
			return "--" + code;
		}

		return code;
	}

	public List<OfferTemplateCategory> getChildren() {
		return children;
	}

	public void setChildren(List<OfferTemplateCategory> children) {
		this.children = children;
	}

}
