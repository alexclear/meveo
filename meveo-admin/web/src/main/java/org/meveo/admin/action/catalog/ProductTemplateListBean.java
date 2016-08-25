package org.meveo.admin.action.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ConversationScoped;
import javax.inject.Named;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.catalog.ProductTemplate;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.service.base.local.IPersistenceService;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Named
@ConversationScoped
public class ProductTemplateListBean extends ProductTemplateBean {

	private static final long serialVersionUID = -7109673492144846741L;

	private MeveoInstance meveoInstanceToExport = new MeveoInstance();

	private List<String> bundledProducts = new ArrayList<String>();

	private List<ProductTemplate> ptToExport = new ArrayList<ProductTemplate>();

	private long activeProductCount = 0;

	private long inactiveProductCount = 0;

	private long almostExpiredCount = 0;

	@Override
	public void preRenderView() {
		activeProductCount = productTemplateService.productTemplateActiveCount(true);
		inactiveProductCount = productTemplateService.productTemplateActiveCount(false);
		almostExpiredCount = productTemplateService.productTemplateAlmostExpiredCount();
		super.preRenderView();
	}

	@Override
	protected IPersistenceService<ProductTemplate> getPersistenceService() {
		return productTemplateService;
	}

	public String newProductTemplate() {
		return "mmProductTemplateDetail";
	}

	public void updateProductTemplateStatus(ProductTemplate pt) throws BusinessException {
		productTemplateService.update(pt, getCurrentUser());
	}

	public void addProductTemplateToExport(ProductTemplate pt) {
		if (!ptToExport.contains(pt)) {
			ptToExport.add(pt);
		}
	}
	
	public void deleteForExport(ProductTemplate pt) {
		if (ptToExport.contains(pt)) {
			ptToExport.remove(pt);
		}
	}

	public StreamedContent getImage(ProductTemplate obj) throws IOException {

		return new DefaultStreamedContent(new ByteArrayInputStream(getImageArr(obj)));

	}

	public byte[] getImageArr(ProductTemplate obj) {
		if (obj.getImageAsByteArr() == null) {
			return downloadUrl(getClass().getClassLoader().getResource("img/no_picture.png"));
		}
		return obj.getImageAsByteArr();
	}

	protected byte[] downloadUrl(URL toDownload) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			byte[] chunk = new byte[4096];
			int bytesRead;
			InputStream stream = toDownload.openStream();

			while ((bytesRead = stream.read(chunk)) > 0) {
				outputStream.write(chunk, 0, bytesRead);
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return outputStream.toByteArray();
	}

	public MeveoInstance getMeveoInstanceToExport() {
		return meveoInstanceToExport;
	}

	public void setMeveoInstanceToExport(MeveoInstance meveoInstanceToExport) {
		this.meveoInstanceToExport = meveoInstanceToExport;
	}

	public List<String> getBundledProducts() {
		return bundledProducts;
	}

	public void setBundledProducts(List<String> bundledProducts) {
		this.bundledProducts = bundledProducts;
	}

	public long getActiveProductCount() {
		return activeProductCount;
	}

	public void setActiveProductCount(long activeProductCount) {
		this.activeProductCount = activeProductCount;
	}

	public long getInactiveProductCount() {
		return inactiveProductCount;
	}

	public void setInactiveProductCount(long inactiveProductCount) {
		this.inactiveProductCount = inactiveProductCount;
	}

	public long getAlmostExpiredCount() {
		return almostExpiredCount;
	}

	public void setAlmostExpiredCount(long almostExpiredCount) {
		this.almostExpiredCount = almostExpiredCount;
	}

	public List<ProductTemplate> getPtToExport() {
		return ptToExport;
	}

	public void setPtToExport(List<ProductTemplate> ptToExport) {
		this.ptToExport = ptToExport;
	}

}
