package org.meveo.service.crm.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.CustomFieldEntity;
import org.meveo.model.ICustomFieldEntity;
import org.meveo.model.IEntity;
import org.meveo.model.IProvider;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.crm.custom.CustomFieldStorageTypeEnum;
import org.meveo.model.crm.custom.CustomFieldTypeEnum;
import org.meveo.service.base.BusinessService;
import org.meveo.service.index.ElasticClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class CustomFieldTemplateService extends BusinessService<CustomFieldTemplate> {

    @Inject
    private CustomFieldsCacheContainerProvider customFieldsCache;

    @Inject
    private ElasticClient elasticClient;

    private ParamBean paramBean = ParamBean.getInstance();

    /**
     * Find a list of custom field templates corresponding to a given entity
     * 
     * @param entity Entity that custom field templates apply to
     * @param provider Provider
     * @return A list of custom field templates mapped by a template key
     */
    public Map<String, CustomFieldTemplate> findByAppliesTo(ICustomFieldEntity entity, Provider provider) {
        try {
            if (entity instanceof Provider) {
                return findByAppliesTo(CustomFieldTemplateService.calculateAppliesToValue(entity), (Provider) entity);
            } else if (entity instanceof IProvider && ((IProvider) entity).getProvider() != null) {
                return findByAppliesTo(CustomFieldTemplateService.calculateAppliesToValue(entity), ((IProvider) entity).getProvider());
            } else {
                return findByAppliesTo(CustomFieldTemplateService.calculateAppliesToValue(entity), provider);
            }

        } catch (CustomFieldException e) {
            // Its ok, handles cases when value that is part of CFT.AppliesTo calculation is not set yet on entity
            return new HashMap<String, CustomFieldTemplate>();
        }
    }

    /**
     * Find a list of custom field templates corresponding to a given entity
     * 
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param provider Provider
     * @return A list of custom field templates mapped by a template key
     */
    public Map<String, CustomFieldTemplate> findByAppliesTo(String appliesTo, Provider provider) {

        // Handles cases when creating a new provider
        if (provider.getId() == null) {
            return new HashMap<String, CustomFieldTemplate>();
        }

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFT", "true"));
        if (useCache) {
            return customFieldsCache.getCustomFieldTemplates(appliesTo, provider);

        } else {
            return findByAppliesToNoCache(appliesTo, provider);
        }
    }

    /**
     * Find a list of custom field templates corresponding to a given entity - always do a lookup in DB
     * 
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param provider Provider
     * @return A list of custom field templates mapped by a template key
     */
    @SuppressWarnings("unchecked")
    public Map<String, CustomFieldTemplate> findByAppliesToNoCache(String appliesTo, Provider provider) {

        QueryBuilder qb = new QueryBuilder(CustomFieldTemplate.class, "c", Arrays.asList("calendar"), provider);
        qb.addCriterion("c.appliesTo", "=", appliesTo, true);

        List<CustomFieldTemplate> values = (List<CustomFieldTemplate>) qb.getQuery(getEntityManager()).getResultList();

        Map<String, CustomFieldTemplate> cftMap = new HashMap<String, CustomFieldTemplate>();
        for (CustomFieldTemplate cft : values) {
            cftMap.put(cft.getCode(), cft);
        }

        return cftMap;
    }

    /**
     * Find a specific custom field template by a code
     * 
     * @param code Custom field template code
     * @param entity Entity that custom field templates apply to
     * @return Custom field template
     */
    public CustomFieldTemplate findByCodeAndAppliesTo(String code, ICustomFieldEntity entity) {
        try {
            return findByCodeAndAppliesTo(code, CustomFieldTemplateService.calculateAppliesToValue(entity), ProviderService.getProvider((IEntity) entity));
        } catch (CustomFieldException e) {
            log.error("Can not determine applicable CFT type for entity of {} class.", entity.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Find a specific custom field template by a code - do a lookup in cache if it is enabled
     * 
     * @param code Custom field template code
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param provider Provider
     * @return Custom field template
     */
    public CustomFieldTemplate findByCodeAndAppliesTo(String code, String appliesTo, Provider provider) {

        boolean useCache = Boolean.parseBoolean(paramBean.getProperty("cache.cacheCFT", "true"));
        if (useCache) {
            return customFieldsCache.getCustomFieldTemplate(code, appliesTo, provider);
        } else {
            return findByCodeAndAppliesToNoCache(code, appliesTo, provider);
        }
    }

    /**
     * Find a specific custom field template by a code bypassing cache - always do a lookup in DB
     * 
     * @param code Custom field template code
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param provider Provider
     * @return Custom field template
     */
    public CustomFieldTemplate findByCodeAndAppliesToNoCache(String code, String appliesTo, Provider provider) {

        QueryBuilder qb = new QueryBuilder(CustomFieldTemplate.class, "c", null, provider);
        qb.addCriterion("code", "=", code, true);
        qb.addCriterion("appliesTo", "=", appliesTo, true);
        try {
            return (CustomFieldTemplate) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void create(CustomFieldTemplate cft, User creator) throws BusinessException {

        if ("INVOICE_SEQUENCE".equals(cft.getCode())
                && (cft.getFieldType() != CustomFieldTypeEnum.LONG || cft.getStorageType() != CustomFieldStorageTypeEnum.SINGLE || !cft.isVersionable() || cft.getCalendar() == null)) {
            throw new BusinessException("invoice_sequence CF must be versionnable,Long,Single value and must have a Calendar");
        }
        if ("INVOICE_ADJUSTMENT_SEQUENCE".equals(cft.getCode())
                && (cft.getFieldType() != CustomFieldTypeEnum.LONG || cft.getStorageType() != CustomFieldStorageTypeEnum.SINGLE || !cft.isVersionable() || cft.getCalendar() == null)) {
            throw new BusinessException("invoice_adjustement_sequence CF must be versionnable,Long,Single value and must have a Calendar");
        }
        super.create(cft, creator);
        customFieldsCache.addUpdateCustomFieldTemplate(cft);
        elasticClient.updateCFMapping(cft);
    }

    @Override
    public CustomFieldTemplate update(CustomFieldTemplate cft, User updater) throws BusinessException {

        if ("INVOICE_SEQUENCE".equals(cft.getCode())
                && (cft.getFieldType() != CustomFieldTypeEnum.LONG || cft.getStorageType() != CustomFieldStorageTypeEnum.SINGLE || !cft.isVersionable() || cft.getCalendar() == null)) {
            throw new BusinessException("invoice_sequence CF must be versionnable,Long,Single value and must have a Calendar");
        }
        if ("INVOICE_ADJUSTMENT_SEQUENCE".equals(cft.getCode())
                && (cft.getFieldType() != CustomFieldTypeEnum.LONG || cft.getStorageType() != CustomFieldStorageTypeEnum.SINGLE || !cft.isVersionable() || cft.getCalendar() == null)) {
            throw new BusinessException("invoice_adjustement_sequence CF must be versionnable,Long,Single value and must have a Calendar");
        }
        CustomFieldTemplate cftUpdated = super.update(cft, updater);
        customFieldsCache.addUpdateCustomFieldTemplate(cftUpdated);
        elasticClient.updateCFMapping(cftUpdated);

        return cftUpdated;
    }

    @Override
    public void remove(CustomFieldTemplate cft, User currentUser) throws BusinessException {
        customFieldsCache.removeCustomFieldTemplate(cft);
        super.remove(cft, currentUser);
    }

    @Override
    public CustomFieldTemplate enable(CustomFieldTemplate cft, User currentUser) throws BusinessException {
        cft = super.enable(cft, currentUser);
        customFieldsCache.addUpdateCustomFieldTemplate(cft);
        return cft;
    }

    @Override
    public CustomFieldTemplate disable(CustomFieldTemplate cft, User currentUser) throws BusinessException {
        cft = super.disable(cft, currentUser);
        customFieldsCache.removeCustomFieldTemplate(cft);
        return cft;
    }

    /**
     * Get a list of custom field templates for cache
     * 
     * @return A list of custom field templates
     */
    public List<CustomFieldTemplate> getCFTForCache() {
        List<CustomFieldTemplate> cfts = getEntityManager().createNamedQuery("CustomFieldTemplate.getCFTForCache", CustomFieldTemplate.class).getResultList();
        return cfts;
    }

    /**
     * Get a list of custom field templates for index
     * 
     * @param provider Provider
     * @return A list of custom field templates
     */
    public List<CustomFieldTemplate> getCFTForIndex(Provider provider) {
        List<CustomFieldTemplate> cfts = getEntityManager().createNamedQuery("CustomFieldTemplate.getCFTForIndex", CustomFieldTemplate.class).setParameter("provider", provider)
            .getResultList();
        return cfts;
    }

    /**
     * Calculate custom field template AppliesTo value for a given entity. AppliesTo consist of a prefix and optionally one or more entity fields. e.g. JOB_<jobTemplate>
     * 
     * @param entity Entity
     * @return A appliesTo value
     * @throws CustomFieldException An exception when AppliesTo value can not be calculated. Occurs when value that is part of CFT.AppliesTo calculation is not set yet on entity
     */
    public static String calculateAppliesToValue(ICustomFieldEntity entity) throws CustomFieldException {
        CustomFieldEntity cfeAnnotation = entity.getClass().getAnnotation(CustomFieldEntity.class);

        String appliesTo = cfeAnnotation.cftCodePrefix();
        if (cfeAnnotation.cftCodeFields().length > 0) {
            for (String fieldName : cfeAnnotation.cftCodeFields()) {
                try {
                    Object fieldValue = FieldUtils.getField(entity.getClass(), fieldName, true).get(entity);
                    if (fieldValue == null) {
                        throw new CustomFieldException("Can not calculate AppliesTo value");
                    }
                    appliesTo = appliesTo + "_" + fieldValue;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    Logger log = LoggerFactory.getLogger(CustomFieldTemplateService.class);
                    log.error("Unable to access field {}.{}", entity.getClass().getSimpleName(), fieldName);
                    throw new RuntimeException("Unable to access field " + entity.getClass().getSimpleName() + "." + fieldName);
                }
            }
        }
        return appliesTo;
    }

    /**
     * Check and create missing templates given a list of templates
     * 
     * @param entity Entity that custom field templates apply to
     * @param templates A list of templates to check * @param currentUser The current User object
     * @return A complete list of templates for a given entity and provider. Mapped by a custom field template key.
     * @throws BusinessException
     */
    public Map<String, CustomFieldTemplate> createMissingTemplates(ICustomFieldEntity entity, Collection<CustomFieldTemplate> templates, User currentUser) throws BusinessException {
        try {
            return createMissingTemplates(calculateAppliesToValue(entity), templates, currentUser, false, false);

        } catch (CustomFieldException e) {
            // Its OK, handles cases when value that is part of CFT.AppliesTo calculation is not set yet on entity
            return new HashMap<String, CustomFieldTemplate>();
        }
    }

    /**
     * Check and create missing templates given a list of templates
     * 
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param templates A list of templates to check
     * @param currentUser The current User object
     * @return A complete list of templates for a given entity and provider. Mapped by a custom field template key.
     * @throws BusinessException
     */
    public Map<String, CustomFieldTemplate> createMissingTemplates(String appliesTo, Collection<CustomFieldTemplate> templates, User currentUser) throws BusinessException {
        return createMissingTemplates(appliesTo, templates, currentUser, false, false);
    }

    /**
     * Check and create missing templates given a list of templates
     * 
     * @param entity Entity that custom field templates apply to
     * @param templates A list of templates to check
     * @param currentUser The current User object
     * @param removeOrphans When set to true, this will remove custom field templates that are not included in the templates collection.
     * @return A complete list of templates for a given entity and provider. Mapped by a custom field template key.
     * @throws BusinessException
     */
    public Map<String, CustomFieldTemplate> createMissingTemplates(ICustomFieldEntity entity, Collection<CustomFieldTemplate> templates, User currentUser, boolean updateExisting,
            boolean removeOrphans) throws BusinessException {
        try {
            return createMissingTemplates(calculateAppliesToValue(entity), templates, currentUser, updateExisting, removeOrphans);
        } catch (CustomFieldException e) {
            // Its OK, handles cases when value that is part of CFT.AppliesTo calculation is not set yet on entity
            return new HashMap<String, CustomFieldTemplate>();
        }
    }

    /**
     * Check and create missing templates given a list of templates
     * 
     * @param appliesTo Entity (CFT appliesTo code) that custom field templates apply to
     * @param templates A list of templates to check
     * @param currentUser The current User object
     * @param removeOrphans When set to true, this will remove custom field templates that are not included in the templates collection.
     * @return A complete list of templates for a given entity and provider. Mapped by a custom field template key.
     * @throws BusinessException
     */
    public Map<String, CustomFieldTemplate> createMissingTemplates(String appliesTo, Collection<CustomFieldTemplate> templates, User currentUser, boolean updateExisting,
            boolean removeOrphans) throws BusinessException {

        // Get templates corresponding to an entity type
        Map<String, CustomFieldTemplate> allTemplates = findByAppliesToNoCache(appliesTo, currentUser.getProvider());

        if (templates != null) {
            CustomFieldTemplate existingCustomField = null;
            for (CustomFieldTemplate cft : templates) {
                if (!allTemplates.containsKey(cft.getCode())) {
                    log.debug("Create a missing CFT {} for {} entity", cft.getCode(), appliesTo);
                    create(cft, currentUser);
                    allTemplates.put(cft.getCode(), cft);
                } else if (updateExisting) {
                    existingCustomField = allTemplates.get(cft.getCode());
                    existingCustomField.setDescription(cft.getDescription());
                    existingCustomField.setStorageType(cft.getStorageType());
                    existingCustomField.setAllowEdit(cft.isAllowEdit());
                    existingCustomField.setDefaultValue(cft.getDefaultValue());
                    existingCustomField.setFieldType(cft.getFieldType());
                    existingCustomField.setEntityClazz(cft.getEntityClazz());
                    existingCustomField.setListValues(cft.getListValues());
                    existingCustomField.setGuiPosition(cft.getGuiPosition());
                    log.debug("Update existing CFT {} for {} entity", cft.getCode(), appliesTo);
                    update(existingCustomField, currentUser);
                }
            }
            if (removeOrphans) {
                CustomFieldTemplate customFieldTemplate = null;
                List<CustomFieldTemplate> forRemoval = new ArrayList<>();
                for (Map.Entry<String, CustomFieldTemplate> customFieldTemplateEntry : allTemplates.entrySet()) {
                    customFieldTemplate = customFieldTemplateEntry.getValue();
                    if (!templates.contains(customFieldTemplate)) {
                        // add to separate list to avoid ConcurrentModificationException
                        forRemoval.add(customFieldTemplate);
                    }
                }
                for (CustomFieldTemplate fieldTemplate : forRemoval) {
                    remove(fieldTemplate, currentUser);
                }
            }
        }
        return allTemplates;
    }
}
