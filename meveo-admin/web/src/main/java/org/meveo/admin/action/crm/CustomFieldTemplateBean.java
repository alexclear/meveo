package org.meveo.admin.action.crm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.UpdateMapTypeFieldBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.custom.CustomFieldMapKeyEnum;
import org.meveo.model.crm.custom.CustomFieldMatrixColumn;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.CalendarService;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.custom.CustomizedEntity;
import org.meveo.service.custom.CustomizedEntityService;
import org.meveo.util.EntityCustomizationUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.model.DualListModel;

@Named
@ViewScoped
public class CustomFieldTemplateBean extends UpdateMapTypeFieldBean<CustomFieldTemplate> {

	private static final long serialVersionUID = 9099292371182275568L;

	@Inject
	private CustomFieldTemplateService customFieldTemplateService;

	@Inject
	private CalendarService calendarService;

	@Inject
	private CustomizedEntityService customizedEntityService;

	@Inject
	private transient ResourceBundle bundle;

	private DualListModel<CustomFieldMatrixColumn> childEntityFieldDM;

	public CustomFieldTemplateBean() {
		super(CustomFieldTemplate.class);
	}

	@Override
	public CustomFieldTemplate initEntity() {
		CustomFieldTemplate customFieldTemplate = super.initEntity();

		if (customFieldTemplate != null) {
			extractMapTypeFieldFromEntity(customFieldTemplate.getListValues(), "listValues");
		}

		return customFieldTemplate;
	}

	@Override
	@ActionMethod
	public String saveOrUpdate(boolean killConversation) throws BusinessException {

		updateMapTypeFieldInEntity(entity.getListValues(), "listValues");

		CustomFieldTemplate cfDuplicate = customFieldTemplateService.findByCodeAndAppliesTo(entity.getCode(), entity.getAppliesTo(), getCurrentProvider());
		if (cfDuplicate != null && !cfDuplicate.getId().equals(entity.getId())) {
			messages.error(new BundleKey("messages", "customFieldTemplate.alreadyExists"));
			return null;
		}

		// Update childEntityCcolums
		if (getEntity().getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY) {
			List<String> cheColumns = new ArrayList<>();
			for (CustomFieldMatrixColumn cheColumn : childEntityFieldDM.getTarget()) {
				cheColumns.add(cheColumn.getCode());
			}
			getEntity().setChildEntityFieldsAsList(cheColumns);
		} else {
			getEntity().setChildEntityFields(null);
		}

		if (entity.getCalendar() != null) {
			entity.setCalendar(calendarService.refreshOrRetrieve(entity.getCalendar()));
		}
		return super.saveOrUpdate(killConversation);
	}

	@Override
	protected IPersistenceService<CustomFieldTemplate> getPersistenceService() {
		return customFieldTemplateService;
	}

	@Override
	protected String getDefaultSort() {
		return "code";
	}

	@Override
	protected List<String> getFormFieldsToFetch() {
		return Arrays.asList("provider");
	}

	/**
	 * Autocomplete method for selecting a class/custom entity template for
	 * entity reference type Custom field template
	 * 
	 * @param query
	 *            Partial value entered
	 * @return A list of matching values
	 */
	public List<String> autocompleteClassNames(String query) {
		List<String> clazzNames = new ArrayList<String>();

		List<CustomizedEntity> entities = customizedEntityService.getCustomizedEntities(query, false, true, null, null, getCurrentProvider());

		for (CustomizedEntity customizedEntity : entities) {
			clazzNames.add(customizedEntity.getClassnameToDisplay());
		}

		return clazzNames;
	}

	/**
	 * Autocomplete method for selecting a custom entity template for child
	 * entity reference type Custom field template
	 * 
	 * @param query
	 *            Partial value entered
	 * @return A list of matching values
	 */
	public List<String> autocompleteClassNamesCEIOnly(String query) {
		List<String> clazzNames = new ArrayList<String>();

		List<CustomizedEntity> entities = customizedEntityService.getCustomizedEntities(query, true, false, null, null, getCurrentProvider());

		for (CustomizedEntity customizedEntity : entities) {
			clazzNames.add(customizedEntity.getClassnameToDisplay());
		}

		return clazzNames;
	}

	/**
	 * Autocomplete method for selecting a class that implement
	 * ICustomFieldEntity. Return a human readable class name. Used in
	 * conjunction with CustomFieldAppliesToConverter
	 * 
	 * @param query
	 *            Partial class name to match
	 * @return
	 */
	public List<String> autocompleteClassNamesHuman(String query) {
		List<String> clazzNames = new ArrayList<String>();

		List<CustomizedEntity> entities = customizedEntityService.getCustomizedEntities(query, false, true, null, null, getCurrentProvider());

		for (CustomizedEntity customizedEntity : entities) {
			clazzNames.add(customizedEntity.getClassnameToDisplayHuman());
		}

		return clazzNames;
	}

	public void updateDefaultValues() {

        if (entity.getFieldType() == CustomFieldTypeEnum.STRING && entity.getMaxValue() == null) {
            entity.setMaxValue(CustomFieldTemplate.DEFAULT_MAX_LENGTH_STRING);
        }
        if (entity.getFieldType() == CustomFieldTypeEnum.CHILD_ENTITY) {
            entity.setStorageType(CustomFieldStorageTypeEnum.LIST);
            entity.setVersionable(false);
        }
        if (entity.getStorageType() == CustomFieldStorageTypeEnum.MAP && entity.getMapKeyType() == null) {
            entity.setMapKeyType(CustomFieldMapKeyEnum.STRING);
        }
        if (entity.getFieldType() == CustomFieldTypeEnum.PRICE) {
        	entity.setStorageType(CustomFieldStorageTypeEnum.MATRIX);
			
			CustomFieldMatrixColumn cfMatrixColumn = new CustomFieldMatrixColumn();
			cfMatrixColumn.setCode(CustomFieldMatrixColumn.PRICE_CURRENCY_FIELD);
			cfMatrixColumn.setLabel(bundle.getString("currency.codeCurrency"));
			cfMatrixColumn.setKeyType(CustomFieldMapKeyEnum.STRING);
			cfMatrixColumn.setPosition(0);
			entity.setMatrixColumns(Arrays.asList(cfMatrixColumn));
        }
    }

	public void resetChildEntityFields() {
		childEntityFieldDM = null;
	}

	public DualListModel<CustomFieldMatrixColumn> getChildEntityFieldListModel() {
		if (childEntityFieldDM == null && CustomFieldTemplate.retrieveCetCode(entity.getEntityClazz()) != null) {

			List<CustomFieldMatrixColumn> perksSource = new ArrayList<>();
			perksSource.add(new CustomFieldMatrixColumn("code", "Code"));
			perksSource.add(new CustomFieldMatrixColumn("description", "Description"));

			Map<String, CustomFieldTemplate> cfts = customFieldTemplateService.findByAppliesTo(
					EntityCustomizationUtils.getAppliesTo(CustomEntityTemplate.class, CustomFieldTemplate.retrieveCetCode(entity.getEntityClazz())), getCurrentProvider());

			for (CustomFieldTemplate cft : cfts.values()) {
				perksSource.add(new CustomFieldMatrixColumn(cft.getCode(), cft.getDescription()));
			}

			// Custom field template stores selected fields as a comma separated
			// string of field codes.
			List<CustomFieldMatrixColumn> perksTarget = new ArrayList<CustomFieldMatrixColumn>();
			if (getEntity().getChildEntityFields() != null) {
				for (String fieldCode : getEntity().getChildEntityFieldsAsList()) {
					if (fieldCode.equals("code")) {
						perksTarget.add(new CustomFieldMatrixColumn("code", "Code"));
					} else if (fieldCode.equals("description")) {
						perksTarget.add(new CustomFieldMatrixColumn("description", "Description"));
					} else if (cfts.containsKey(fieldCode)) {
						CustomFieldTemplate cft = cfts.get(fieldCode);
						perksTarget.add(new CustomFieldMatrixColumn(cft.getCode(), cft.getDescription()));
					}
				}
			}
			perksSource.removeAll(perksTarget);
			childEntityFieldDM = new DualListModel<CustomFieldMatrixColumn>(perksSource, perksTarget);
		}
		return childEntityFieldDM;
	}

	public void setChildEntityFieldListModel(DualListModel<CustomFieldMatrixColumn> childEntityFieldDM) {
		this.childEntityFieldDM = childEntityFieldDM;
	}
}