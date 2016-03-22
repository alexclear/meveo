package org.meveo.admin.action.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.catalog.BusinessOfferModel;
import org.meveo.model.catalog.OfferServiceTemplate;
import org.meveo.model.catalog.ServiceTemplate;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.BusinessOfferModelService;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DualListModel;

@Named
@ViewScoped
public class BusinessOfferModelBean extends BaseBean<BusinessOfferModel> {

	private static final long serialVersionUID = 8222060379099238520L;

	@Inject
	private BusinessOfferModelService businessOfferModelService;

	private Map<String, String> offerCFVs = new HashMap<>();
	private String serviceCodePrefix;
	private Map<String, String> serviceCFVs = new HashMap<>();
	DualListModel<ServiceTemplate> serviceDualListModel;

	private String bomOfferInstancePrefix;

	public BusinessOfferModelBean() {
		super(BusinessOfferModel.class);
	}

	@Override
	protected IPersistenceService<BusinessOfferModel> getPersistenceService() {
		return businessOfferModelService;
	}

	@Override
	protected String getListViewName() {
		return "businessOfferModels";
	}

	public void createOfferFromBOMPopup() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("resizable", false);
		options.put("draggable", false);
		options.put("scrollable", false);
		options.put("modal", true);
		options.put("width", 700);
		options.put("height", 400);

		Map<String, List<String>> params = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(getEntity().getId().toString());
		params.put("objectId", values);

		RequestContext.getCurrentInstance().openDialog("createOfferFromBOM", options, params);
	}

	public void createOfferFromBOM() throws BusinessException {
		List<String> serviceCodes = new ArrayList<String>();
		for (ServiceTemplate st : serviceDualListModel.getTarget()) {
			serviceCodes.add(st.getCode());
		}

		businessOfferModelService.createOfferFromBOM(getEntity(), bomOfferInstancePrefix, serviceCodes, currentUser);
		RequestContext.getCurrentInstance().closeDialog(getEntity());
	}

	public void onBOMOfferCreation(SelectEvent event) {
		messages.info(new BundleKey("messages", "message.bom.offerCreation.ok"));
	}

	public DualListModel<ServiceTemplate> getServiceDualListModel() {
		if (serviceDualListModel == null) {
			List<ServiceTemplate> perksSource = null;
			if (getEntity() != null) {
				List<ServiceTemplate> serviceTemplates = new ArrayList<>();
				for (OfferServiceTemplate ost : entity.getOfferTemplate().getOfferServiceTemplates()) {
					if (ost.getServiceTemplate() != null) {
						serviceTemplates.add(ost.getServiceTemplate());
					}
				}
				perksSource = serviceTemplates;
			}

			serviceDualListModel = new DualListModel<ServiceTemplate>(perksSource, new ArrayList<ServiceTemplate>());
		}

		return serviceDualListModel;
	}

	public void setServiceDualListModel(DualListModel<ServiceTemplate> stDM) {
		serviceDualListModel = stDM;
	}

	public void onCreateOfferFromBOM(SelectEvent event) {
		System.out.println("test");
	}

	public String getServiceCodePrefix() {
		return serviceCodePrefix;
	}

	public void setServiceCodePrefix(String serviceCodePrefix) {
		this.serviceCodePrefix = serviceCodePrefix;
	}

	public Map<String, String> getServiceCFVs() {
		return serviceCFVs;
	}

	public void setServiceCFVs(Map<String, String> serviceCFVs) {
		this.serviceCFVs = serviceCFVs;
	}

	public Map<String, String> getOfferCFVs() {
		return offerCFVs;
	}

	public void setOfferCFVs(Map<String, String> offerCFVs) {
		this.offerCFVs = offerCFVs;
	}

	public String getBomOfferInstancePrefix() {
		return bomOfferInstancePrefix;
	}

	public void setBomOfferInstancePrefix(String bomOfferInstancePrefix) {
		this.bomOfferInstancePrefix = bomOfferInstancePrefix;
	}

}