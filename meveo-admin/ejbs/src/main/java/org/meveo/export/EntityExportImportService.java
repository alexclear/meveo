package org.meveo.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Conversation;
import javax.faces.model.DataModel;
import javax.inject.Inject;
import javax.persistence.CascadeType;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.Version;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.proxy.HibernateProxy;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.meveo.api.MeveoApiErrorCode;
import org.meveo.api.dto.response.utilities.ImportExportResponseDto;
import org.meveo.cache.CdrEdrProcessingCacheContainerProvider;
//import org.meveo.cache.CustomFieldsCacheContainerProvider;
import org.meveo.cache.NotificationCacheContainerProvider;
import org.meveo.cache.RatingCacheContainerProvider;
import org.meveo.cache.WalletCacheContainerProvider;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.Auditable;
import org.meveo.model.ExportIdentifier;
import org.meveo.model.IEntity;
import org.meveo.model.IVersionedEntity;
import org.meveo.model.communication.MeveoInstance;
import org.meveo.model.crm.Provider;
import org.meveo.model.shared.DateUtils;
import org.meveo.util.MeveoJpa;
import org.meveo.util.MeveoJpaForJobs;
import org.primefaces.model.LazyDataModel;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;

import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.core.ReferenceByIdMarshaller;
import com.thoughtworks.xstream.core.ReferenceByIdUnmarshaller;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentCollectionConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedSetConverter;
import com.thoughtworks.xstream.hibernate.mapper.HibernateMapper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

@Lock(LockType.READ)
@Singleton
public class EntityExportImportService implements Serializable {

    private static final long serialVersionUID = 5141462881249084547L;

    public static String EXPORT_PARAM_DELETE = "delete";
    public static String EXPORT_PARAM_ZIP = "zip";
    public static String EXPORT_PARAM_REMOTE_INSTANCE = "remoteInstance";

    // How may records to retrieve from DB at a time
    private static final int PAGE_SIZE = 200;
    // How many pages of PAGE_SIZE to group into one export chunk
    private static final int EXPORT_PAGE_SIZE = 5;
    protected static final String REFERENCE_ID_ATTRIBUTE = "xsId";

    @Inject
    @MeveoJpa
    private EntityManager em;

    @Inject
    @MeveoJpaForJobs
    private EntityManager emfForJobs;

    @Inject
    private Conversation conversation;

    private ParamBean param = ParamBean.getInstance();

    @Inject
    private Logger log;

    @Inject
    private WalletCacheContainerProvider walletCacheContainerProvider;

    @Inject
    private CdrEdrProcessingCacheContainerProvider cdrEdrProcessingCacheContainerProvider;

    @Inject
    private NotificationCacheContainerProvider notificationCacheContainerProvider;

    @Inject
    private RatingCacheContainerProvider ratingCacheContainerProvider;

    // @Inject
    // private CustomFieldsCacheContainerProvider customFieldsCacheContainerProvider;

    private Map<Class<? extends IEntity>, String[]> exportIdMapping;

    private Map<String, Object[]> attributesToOmit;

    private LinkedHashMap<String, String> exportModelVersionChangesets;

    private String currentExportModelVersionChangeset;

    @SuppressWarnings("rawtypes")
    private Map<Class, List<Field>> nonCascadableFields;

    @EJB
    private EntityExportImportService entityExportImportService;

    @PostConstruct
    private void init() {
        loadExportIdentifierMappings();
        loadAtributesToOmit();
        loadNonCascadableFields();
        loadExportModelVersionChangesets();
    }

    /**
     * Obtain entity manager for export operations
     * 
     * @return
     */
    private EntityManager getEntityManager() {
        EntityManager result = emfForJobs;
        if (conversation != null) {
            try {
                conversation.isTransient();
                result = em;
            } catch (Exception e) {
            }
        }

        return result;
    }

    /**
     * Obtain entity manager for import operations in case want to import to another DB
     * 
     * @return
     */
    private EntityManager getEntityManagerForImport() {
        return getEntityManager();
    }

    /**
     * Export entities matching given export templates
     * 
     * @param exportTemplates A list of export templates
     * @param parameters Entity export (select) criteria
     * @return Export statistics
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<ExportImportStatistics> exportEntities(Collection<ExportTemplate> exportTemplates, Map<String, Object> parameters) {
        ExportImportStatistics exportStats = new ExportImportStatistics();
        for (ExportTemplate exportTemplate : exportTemplates) {
            ExportImportStatistics exportStatsSingle = exportEntitiesInternal(exportTemplate, parameters, null);
            exportStats.mergeStatistics(exportStatsSingle);
        }
        return new AsyncResult<ExportImportStatistics>(exportStats);
    }

    /**
     * Export entities matching a given export template
     * 
     * @param exportTemplates Export template
     * @param parameters Entity export (select) criteria
     * @param dataModelToExport Entities to export that are already filtered in a data model. Supports export of non-grouped export templates only.
     * @return Export statistics
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<ExportImportStatistics> exportEntities(ExportTemplate exportTemplate, Map<String, Object> parameters, DataModel<? extends IEntity> dataModelToExport) {

        ExportImportStatistics exportStats = exportEntitiesInternal(exportTemplate, parameters, dataModelToExport);

        return new AsyncResult<ExportImportStatistics>(exportStats);
    }

    /**
     * Export entities matching a given export template
     * 
     * @param exportTemplates Export template
     * @param parameters Entity export (select) criteria
     * @param dataModelToExport Entities to export that are already filtered in a data model. Supports export of non-grouped export templates only.
     * @return Export statistics
     */
    private ExportImportStatistics exportEntitiesInternal(ExportTemplate exportTemplate, Map<String, Object> parameters, DataModel<? extends IEntity> dataModelToExport) {

        if (parameters == null) {
            parameters = new HashMap<String, Object>();
        }

        ExportImportStatistics exportStats = new ExportImportStatistics();

        // When exporting to a remote meveo instance - always export to zip
        if (parameters.get(EXPORT_PARAM_REMOTE_INSTANCE) != null) {
            parameters.put(EXPORT_PARAM_ZIP, true);

            // Check that authentication username and password are provided
            if (((MeveoInstance) parameters.get(EXPORT_PARAM_REMOTE_INSTANCE)).getAuthUsername() == null
                    || ((MeveoInstance) parameters.get(EXPORT_PARAM_REMOTE_INSTANCE)).getAuthPassword() == null) {
                exportStats.setErrorMessageKey("export.remoteImportNoAuth");
                return exportStats;
            }
        }

        String shortFilename = exportTemplate.getName() + DateUtils.formatDateWithPattern(new Date(), "_yyyy-MM-dd_HH-mm-ss");
        boolean asZip = (parameters.get(EXPORT_PARAM_ZIP) != null && ((boolean) parameters.get(EXPORT_PARAM_ZIP)));

        String path = param.getProperty("providers.rootDir", "/tmp/meveo/");
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }

        if (parameters.get("provider") != null) {
            path = path + ((Provider) parameters.get("provider")).getCode() + File.separator;
        }

        path = path + "exports";
        String filename = path + File.separator + shortFilename + (asZip ? ".zip" : ".xml");
        Writer fileWriter = null;
        ZipOutputStream zos = null;
        try {
            log.info("Exporting data to a file {}", filename);
            FileUtils.forceMkdir(new File(path));

            if (asZip) {
                zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
                zos.putNextEntry(new ZipEntry(shortFilename + ".xml"));
                fileWriter = new OutputStreamWriter(zos);

            } else {
                fileWriter = new FileWriter(filename);
            }

            HierarchicalStreamWriter writer = new EntityExportWriter(fileWriter);
            // Open root node
            writer.startNode("meveoExport");
            writer.addAttribute("version", this.currentExportModelVersionChangeset);

            // Export from a provided data model applies only in cases on non-grouped templates as it has a single entity type
            if (exportTemplate.getGroupedTemplates() == null || exportTemplate.getGroupedTemplates().isEmpty()) {
                entityExportImportService.serializeEntities(exportTemplate, parameters, dataModelToExport, exportStats, writer);
            } else {
                for (ExportTemplate groupedExportTemplate : exportTemplate.getGroupedTemplates()) {
                    entityExportImportService.serializeEntities(groupedExportTemplate, parameters, null, exportStats, writer);
                }
            }

            // Close root node
            writer.endNode();
            writer.flush();
            if (asZip) {
                zos.closeEntry();
            }
            writer.close();

            // Upload file to a remote meveo instance if was requested so
            if (parameters.get(EXPORT_PARAM_REMOTE_INSTANCE) != null) {
                String remoteExecutionId = uploadFileToRemoteMeveoInstance(filename, (MeveoInstance) parameters.get(EXPORT_PARAM_REMOTE_INSTANCE));
                exportStats.setRemoteImportExecutionId(remoteExecutionId);
            }

        } catch (RemoteAuthenticationException e) {
            log.error("Failed to authenticate to a remote Meveo instance {}: {}", ((MeveoInstance) parameters.get(EXPORT_PARAM_REMOTE_INSTANCE)).getCode(), e.getMessage());
            exportStats.setErrorMessageKey("export.remoteImportFailedAuth");

        } catch (RemoteImportException e) {
            log.error("Failed to communicate or process data in a remote Meveo instance {}: {}", ((MeveoInstance) parameters.get(EXPORT_PARAM_REMOTE_INSTANCE)).getCode(),
                e.getMessage());
            exportStats.setErrorMessageKey("export.remoteImportFailedOther");

        } catch (Exception e) {
            log.error("Failed to export data to a file {}", filename, e);
            exportStats.setException(e);

        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    log.error("Failed to export data to a file {}", filename, e);
                }
            }
        }
        log.info("Entities for export template {} saved to a file {}", exportTemplate.getName(), filename);

        // Remove entities if was requested so
        if (parameters.containsKey(EXPORT_PARAM_DELETE) && (Boolean) parameters.get(EXPORT_PARAM_DELETE)) {
            entityExportImportService.removeEntitiesAfterExport(exportStats);
        }

        return exportStats;
    }

    /**
     * Remove entities after an export
     * 
     * @param exportStats Export statistics, including entities to remove
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeEntitiesAfterExport(ExportImportStatistics exportStats) {

        EntityManager emForRemove = getEntityManager();

        for (Entry<Class, List<Long>> removeInfo : exportStats.getEntitiesToRemove().entrySet()) {
            for (Long id : removeInfo.getValue()) {
                try {
                    emForRemove.remove(emForRemove.getReference(removeInfo.getKey(), id));
                    exportStats.updateDeleteSummary(removeInfo.getKey(), 1);
                    log.trace("Removed entity {} id {}", removeInfo.getKey().getName(), id);

                } catch (Exception e) {
                    log.error("Failed to remove entity {} id {}", removeInfo.getKey().getName(), id);
                }
            }
        }
        exportStats.getEntitiesToRemove().clear();

    }

    /**
     * Export entities matching a given export template
     * 
     * @param exportTemplate Export template
     * @param parameters Entity export (select) criteria
     * @param dataModelToExport Entities to export that are already filtered in a data model. Supports export of non-grouped export templates only.
     * @param exportStats Export statistics
     * @param writer Writer for serialized entity output
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void serializeEntities(ExportTemplate exportTemplate, Map<String, Object> parameters, DataModel<? extends IEntity> dataModelToExport,
            ExportImportStatistics exportStats, HierarchicalStreamWriter writer) {

        log.info("Serializing entities from export template {} and data model {}", exportTemplate.getName(), dataModelToExport != null);

        List<? extends IEntity> entities = getEntitiesToExport(exportTemplate, parameters, dataModelToExport, 0, PAGE_SIZE);
        if (entities.isEmpty()) {
            log.info("No entities to serialize from export template {}", exportTemplate.getName());
            return;
        }

        ExportImportConfig exportImportConfig = new ExportImportConfig(exportTemplate, exportIdMapping);

        int totalEntityCount = 0;
        int from = PAGE_SIZE;
        int pagesProcessedByXstream = -1;
        XStream xstream = null;

        // Serialize entities to XML
        while (!entities.isEmpty()) {

            // Start a new "data" node every X pages of export
            if (pagesProcessedByXstream == -1 || pagesProcessedByXstream >= EXPORT_PAGE_SIZE) {

                xstream = new XStream() {
                    @Override
                    protected MapperWrapper wrapMapper(MapperWrapper next) {
                        return new HibernateMapper(next);
                    }
                };

                xstream.alias("exportTemplate", ExportTemplate.class);
                xstream.useAttributeFor(ExportTemplate.class, "name");
                xstream.useAttributeFor(ExportTemplate.class, "entityToExport");
                xstream.useAttributeFor(ExportTemplate.class, "canDeleteAfterExport");
                // Add custom converters
                xstream.registerConverter(new IEntityHibernateProxyConverter(exportImportConfig), XStream.PRIORITY_VERY_HIGH);
                xstream.registerConverter(new IEntityExportIdentifierConverter(exportImportConfig), XStream.PRIORITY_NORMAL);
                xstream.registerConverter(new HibernatePersistentCollectionConverter(xstream.getMapper()));
                xstream.registerConverter(new HibernatePersistentMapConverter(xstream.getMapper()));
                xstream.registerConverter(new HibernatePersistentSortedMapConverter(xstream.getMapper()));
                xstream.registerConverter(new HibernatePersistentSortedSetConverter(xstream.getMapper()));
                xstream.registerConverter(new IEntityClassConverter(xstream.getMapper(), xstream.getReflectionProvider(), true), XStream.PRIORITY_LOW);

                // Indicate XStream to omit certain attributes except ones matching the classes to be exported fully (except the root class)
                applyAttributesToOmit(xstream, exportTemplate.getClassesToExportAsFull());

                // Indicate marshaling strategy to use - maintains references even when marshaling one object at a time
                xstream.setMarshallingStrategy(new ReusingReferenceByIdMarshallingStrategy());
                xstream.aliasSystemAttribute(REFERENCE_ID_ATTRIBUTE, "id");

                if (pagesProcessedByXstream == -1) {
                    // Write out an export template node
                    xstream.marshal(exportTemplate, writer);
                } else {
                    writer.endNode();
                    writer.flush();
                    log.trace("Serialized {} records from export template {}", totalEntityCount, exportTemplate.getName());
                }
                writer.startNode("data");
                pagesProcessedByXstream = 0;
            }

            for (IEntity entity : entities) {
                xstream.marshal(entity, writer);
            }
            exportStats.updateSummary(exportTemplate.getEntityToExport(), entities.size());
            if (parameters.containsKey(EXPORT_PARAM_DELETE) && (Boolean) parameters.get(EXPORT_PARAM_DELETE)) {
                exportStats.trackEntitiesToDelete(entities);
            }
            totalEntityCount += entities.size();

            // Exit if less records than a page size were found in last iteration
            if (entities.size() < PAGE_SIZE) {
                break;
            }
            writer.flush();
            entities = getEntitiesToExport(exportTemplate, parameters, dataModelToExport, from, PAGE_SIZE);
            from += PAGE_SIZE;
            pagesProcessedByXstream++;
        }

        writer.endNode();
        writer.flush();
        log.info("Serialized {} entities from export template {}", totalEntityCount, exportTemplate.getName());
    }

    /**
     * Import entities from xml stream.
     * 
     * @param fileToImport File contains contains a template that was used to export data and serialized data. Can be in a ziped or unzipped format
     * @param filename A name of a file being imported
     * @param preserveId Should Ids of entities be preserved when importing instead of using sequence values for ID generation (DOES NOT WORK)
     * @param ignoreNotFoundFK Should import fail if any FK was not found
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     * @return Import statistics
     */
    @Asynchronous
    @SuppressWarnings({ "deprecation" })
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<ExportImportStatistics> importEntities(File fileToImport, String filename, boolean preserveId, boolean ignoreNotFoundFK, Provider forceToProvider) {

        log.info("Importing file {} and forcing to provider {}", filename, forceToProvider);
        ExportImportStatistics importStatsTotal = new ExportImportStatistics();

        try {

            @SuppressWarnings("resource")
            InputStream inputStream = new FileInputStream(fileToImport);

            // Handle zip file
            ZipInputStream zis = null;
            if (filename.toLowerCase().endsWith(".zip")) {
                zis = new ZipInputStream(inputStream);
                zis.getNextEntry();
            }

            HierarchicalStreamReader reader = new XppReader(new InputStreamReader(zis != null ? zis : inputStream));

            // Determine if it is a new or old format
            String rootNode = reader.getNodeName();

            String version = null;
            // If it is a new format
            if (rootNode.equals("meveoExport")) {
                version = reader.getAttribute("version");

                // Conversion is required when version from a file and the current model changset version does not match
                boolean conversionRequired = !this.currentExportModelVersionChangeset.equals(version);

                log.debug("Importing a file from a {} version. Current version is {}. Conversion is required {}", version, this.currentExportModelVersionChangeset,
                    conversionRequired);

                // Convert the file and initiate import again
                if (conversionRequired) {
                    reader.close();
                    inputStream.close();
                    File convertedFile = actualizeVersionOfExportFile(fileToImport, filename, version);
                    return importEntities(convertedFile, convertedFile.getName(), preserveId, ignoreNotFoundFK, forceToProvider);
                }

                if (forceToProvider != null) {
                    forceToProvider = getEntityManagerForImport().createQuery("select p from Provider p where p.code=:code", Provider.class)
                        .setParameter("code", forceToProvider.getCode()).getSingleResult();
                }

                XStream xstream = new XStream();
                xstream.alias("exportInfo", ExportInfo.class);
                xstream.alias("exportTemplate", ExportTemplate.class);
                xstream.useAttributeFor(ExportTemplate.class, "name");
                xstream.useAttributeFor(ExportTemplate.class, "entityToExport");
                xstream.useAttributeFor(ExportTemplate.class, "canDeleteAfterExport");
                ExportTemplate importTemplate = null;
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    String nodeName = reader.getNodeName();
                    if (nodeName.equals("exportTemplate")) {
                        importTemplate = (ExportTemplate) xstream.unmarshal(reader);

                    } else if (nodeName.equals("data")) {
                        try {
                            ExportImportStatistics importStats = entityExportImportService.importEntities(importTemplate, reader, preserveId, ignoreNotFoundFK, forceToProvider);
                            importStatsTotal.mergeStatistics(importStats);
                        } catch (Exception e) {
                            importStatsTotal.setException(e);
                            break;
                        }
                    }
                    reader.moveUp();
                }

                // If it is an old, 4.0.3 format
            } else {

                log.debug("Importing a file from a 4.0.3 or older version export. Conversion will be performed.");

                if (forceToProvider != null) {
                    forceToProvider = getEntityManagerForImport().createQuery("select p from Provider p where p.code=:code", Provider.class)
                        .setParameter("code", forceToProvider.getCode()).getSingleResult();
                }
                XStream xstream = new XStream();
                xstream.alias("exportInfo", ExportInfo.class);
                xstream.alias("exportTemplate", ExportTemplate.class);
                xstream.useAttributeFor(ExportTemplate.class, "name");
                xstream.useAttributeFor(ExportTemplate.class, "entityToExport");
                xstream.useAttributeFor(ExportTemplate.class, "canDeleteAfterExport");

                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    ExportInfo exportInfo = (ExportInfo) xstream.unmarshal(reader);
                    ExportImportStatistics importStats = entityExportImportService.importEntities403FileVersion(exportInfo.exportTemplate, exportInfo.serializedData, preserveId,
                        ignoreNotFoundFK, forceToProvider);
                    importStatsTotal.mergeStatistics(importStats);
                    reader.moveUp();
                }
            }

            reader.close();
            refreshCaches();

            log.info("Finished importing file {} ", filename);

        } catch (Exception e) {
            log.error("Failed to import a file {} ", filename, e);
            importStatsTotal.setException(e);
        }

        return new AsyncResult<ExportImportStatistics>(importStatsTotal);
    }

    /**
     * Import entities
     * 
     * @param exportTemplate Export template used to export data
     * @param serializedData Serialized data
     * @param preserveId Should Ids of entities be preserved when importing instead of using sequence values for ID generation (DOES NOT WORK)
     * @param ignoreNotFoundFK Should import fail if any FK was not found
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     * @return Import statistics
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExportImportStatistics importEntities403FileVersion(ExportTemplate exportTemplate, String serializedData, boolean preserveId, boolean ignoreNotFoundFK,
            Provider forceToProvider) {

        if (serializedData == null) {
            log.info("No entities to import from {} export template ", exportTemplate.getName());
            return null;
        }
        log.info("Importing entities from template {} ignore not found FK={}, forcing import to a provider {}", exportTemplate.getName(), ignoreNotFoundFK, forceToProvider);
        log.trace("Importing entities from xml {}", serializedData);

        serializedData = actualizeVersionOfExportFile403FileVersion(serializedData);

        ExportImportStatistics importStats = null;
        final Set<String> ignoredFields = new HashSet<String>();
        XStream xstream = new XStream() {
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {

                    @SuppressWarnings("rawtypes")
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        if (getImplicitCollectionDefForFieldName(definedIn, fieldName) != null) {
                            return true;
                        }
                        if (definedIn != Object.class) {
                            return super.shouldSerializeMember(definedIn, fieldName);
                        } else {
                            // Remember what field was not processed as no corresponding definition was found
                            ignoredFields.add(fieldName);
                            return false;
                        }
                    }
                };
            }

        };

        ExportImportConfig exportImportConfig = new ExportImportConfig(exportTemplate, exportIdMapping);
        IEntityClassConverter iEntityClassConverter = new IEntityClassConverter(xstream.getMapper(), xstream.getReflectionProvider(), preserveId);
        xstream.registerConverter(new IEntityExportIdentifierConverter(exportImportConfig, getEntityManagerForImport(), preserveId, ignoreNotFoundFK, forceToProvider,
            iEntityClassConverter), XStream.PRIORITY_NORMAL);
        xstream.registerConverter(iEntityClassConverter, XStream.PRIORITY_LOW);

        // This was a solution to large data amount processing with JPA transaction on each entity deserialisation, but it gives issues with references between the objects
        // // Pass entity manager to converters
        // DataHolder dataHolder = xstream.newDataHolder();
        // dataHolder.put("em", getEntityManagerForImport());

        List<? extends IEntity> entities = null;
        try {
            HierarchicalStreamReader reader = new XppReader(new StringReader(serializedData));
            entities = (List<? extends IEntity>) xstream.unmarshal(reader);// , null, dataHolder);
            importStats = saveEntitiesToTarget(entities, preserveId, forceToProvider);
            if (!ignoredFields.isEmpty()) {
                importStats.addFieldsNotImported(exportTemplate.getName(), ignoredFields);
            }

        } catch (Exception e) {
            log.error("Failed to import XML contents. Template {}, serialized data {}: ", exportTemplate.getName(), serializedData, e);
            throw new RuntimeException("Failed to import XML contents. Template " + exportTemplate.getName() + ". " + e.getMessage(), e);
        }

        log.info("Imported {} entities from {} export template ", entities.size(), exportTemplate.getName());

        return importStats;

    }

    /**
     * Import entities
     * 
     * @param exportTemplate Export template used to export data
     * @param reader Reader of serialized data input stream
     * @param preserveId Should Ids of entities be preserved when importing instead of using sequence values for ID generation (DOES NOT WORK)
     * @param ignoreNotFoundFK Should import fail if any FK was not found
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     * @return Import statistics
     */
    // This should not be here if want to deserialize each entity in its own transaction
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ExportImportStatistics importEntities(ExportTemplate exportTemplate, HierarchicalStreamReader reader, boolean preserveId, boolean ignoreNotFoundFK,
            Provider forceToProvider) {

        log.info("Importing entities from template {} ignore not found FK={}, forcing import to a provider {}", exportTemplate.getName(), ignoreNotFoundFK, forceToProvider);

        final Set<String> ignoredFields = new HashSet<String>();

        XStream xstream = new XStream() {
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {

                    @SuppressWarnings("rawtypes")
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        if (getImplicitCollectionDefForFieldName(definedIn, fieldName) != null) {
                            return true;
                        }
                        if (definedIn != Object.class) {
                            return super.shouldSerializeMember(definedIn, fieldName);
                        } else {
                            // Remember what field was not processed as no corresponding definition was found
                            ignoredFields.add(fieldName);
                            return false;
                        }
                    }
                };
            }
        };

        xstream.setMarshallingStrategy(new ReusingReferenceByIdMarshallingStrategy());
        xstream.aliasSystemAttribute(REFERENCE_ID_ATTRIBUTE, "id");

        ExportImportConfig exportImportConfig = new ExportImportConfig(exportTemplate, exportIdMapping);
        IEntityClassConverter iEntityClassConverter = new IEntityClassConverter(xstream.getMapper(), xstream.getReflectionProvider(), preserveId);

        xstream.registerConverter(new IEntityExportIdentifierConverter(exportImportConfig, getEntityManagerForImport(), preserveId, ignoreNotFoundFK, forceToProvider,
            iEntityClassConverter), XStream.PRIORITY_NORMAL);
        xstream.registerConverter(iEntityClassConverter, XStream.PRIORITY_LOW);

        ExportImportStatistics importStats = new ExportImportStatistics();
        int totalEntitiesCount = 0;
        try {
            while (reader.hasMoreChildren()) {
                reader.moveDown();

                // This was a solution to large data amount processing with JPA transaction on each entity deserialisation, but it gives issues with references between the objects
                // entityExportImportService.deserializeEntity(xstream, reader, preserveId, importStats, false, forceToProvider);
                deserializeEntity(xstream, reader, preserveId, importStats, false, forceToProvider);
                totalEntitiesCount++;

                reader.moveUp();
            }

        } catch (Exception e) {
            log.error("Failed to import entities from {} export emplate. Imported {} entities", exportTemplate.getName(), totalEntitiesCount, e);
            throw new RuntimeException("Failed to import entities from " + exportTemplate.getName() + " export template. " + e.getMessage(), e);
        }

        if (!ignoredFields.isEmpty()) {
            importStats.addFieldsNotImported(exportTemplate.getName(), ignoredFields);
        }

        log.info("Imported {} entities from {} export template ", totalEntitiesCount, exportTemplate.getName());

        return importStats;
    }

    /**
     * Save entities to a target DB
     * 
     * @param entities Entities to save
     * @param lookupById Should a lookup of existing entity in DB be done by ID or by attributes
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     */
    private ExportImportStatistics saveEntitiesToTarget(List<? extends IEntity> entities, boolean lookupById, Provider forceToProvider) {

        ExportImportStatistics importStats = new ExportImportStatistics();

        for (IEntity entityToSave : entities) {

            saveEntityToTarget(entityToSave, lookupById, importStats, false, forceToProvider);
        }
        return importStats;
    }

    /**
     * Deserialize an entity and save it to a target DB in a new transaction
     * 
     * @param xstream Xstream instance
     * @param reader Reader
     * @param lookupById Should a lookup of existing entity in DB be done by ID or by attributes
     * @param importStats Import statistics
     * @param updateExistingOnly Should only existing entity be saved
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     */
    // This was a solution to large data amount processing with JPA transaction on each entity deserialisation, but it gives issues with references between the objects
    // @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void deserializeEntity(XStream xstream, HierarchicalStreamReader reader, boolean lookupById, ExportImportStatistics importStats, boolean updateExistingOnly,
            Provider forceToProvider) {

        // This was a solution to large data amount processing with JPA transaction on each entity deserialisation, but it gives issues with references between the objects
        // //Pass entity manager to converters
        // DataHolder dataHolder = xstream.newDataHolder();
        // dataHolder.put("em", getEntityManagerForImport());

        IEntity entityToSave = (IEntity) xstream.unmarshal(reader);// , null, dataHolder);
        saveEntityToTarget(entityToSave, lookupById, importStats, updateExistingOnly, forceToProvider);
    }

    /**
     * Save entity to a target DB
     * 
     * @param entityToSave Entity to save
     * @param lookupById Should a lookup of existing entity in DB be done by ID or by attributes
     * @param importStats Import statistics
     * @param updateExistingOnly Should only existing entity be saved - True in case of cascaded entities. Value "false" can be only in case when entity is related by ManyToOne
     *        relationhip (see saveNonManagedField method)
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IEntity saveEntityToTarget(IEntity entityToSave, boolean lookupById, ExportImportStatistics importStats, boolean updateExistingOnly, Provider forceToProvider) {

        log.debug("Saving with preserveId={} entity {} ", lookupById, entityToSave);

        // Check if entity to be saved is a provider entity but were told to force importing to a given provider - just replace a code
        if (forceToProvider != null && entityToSave instanceof Provider) {
            ((Provider) entityToSave).setCode(forceToProvider.getCode());
        }

        // Check that entity does not exist yet
        IEntity entityFound = null;

        // Check by id
        if (lookupById && entityToSave.getId() != null) {
            entityFound = getEntityManagerForImport().find(entityToSave.getClass(), entityToSave.getId());
        } else {
            entityFound = findEntityByAttributes(entityToSave);
        }

        if (entityFound == null && updateExistingOnly) {
            log.debug("No existing entity was found. Entity will be saved by other means (cascading probably).");
            return entityToSave;
        }

        if (entityFound == null) {
            // Clear version field
            if (IVersionedEntity.class.isAssignableFrom(entityToSave.getClass())) {
                ((IVersionedEntity) entityToSave).setVersion(null);
            }

            saveNotManagedFields(entityToSave, lookupById, importStats, forceToProvider);
            getEntityManagerForImport().persist(entityToSave);

            log.debug("Entity saved: {}", entityToSave);

        } else {
            log.debug("Existing entity found with ID {}. Entity will be updated.", entityFound.getId());
            updateEntityFoundInDB(entityFound, entityToSave, lookupById, importStats, forceToProvider);

            log.debug("Entity saved: {}", entityFound);
        }

        List extractedRelatedEntities = extractNonCascadedEntities(entityToSave);
        if (extractedRelatedEntities != null && !extractedRelatedEntities.isEmpty()) {
            ExportImportStatistics importStatsRelated = saveEntitiesToTarget(extractedRelatedEntities, lookupById, null);
            importStats.mergeStatistics(importStatsRelated);
        }

        // Update statistics
        importStats.updateSummary(entityToSave.getClass(), 1);

        return entityFound == null ? entityToSave : entityFound;
    }

    /**
     * Extract entities referred from a given entity that would not be persisted when a given entity is saved
     * 
     * @param entityToSave Entity to analyse
     * @return A list of entities to save
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List extractNonCascadedEntities(IEntity entityToSave) {

        List<Field> fields = nonCascadableFields.get(entityToSave.getClass());
        if (fields == null) {
            return null;
        }

        List nonCascadedEntities = new ArrayList<>();
        for (Field field : fields) {
            try {
                Object fieldValue = FieldUtils.readField(field, entityToSave, true);
                if (fieldValue == null) {
                    continue;
                }
                if (Map.class.isAssignableFrom(field.getType())) {
                    Map mapValue = (Map) fieldValue;
                    if (!mapValue.isEmpty()) {
                        nonCascadedEntities.addAll(mapValue.values());
                        log.trace("Extracted non-cascaded fields {} from {}", mapValue.values(), entityToSave.getClass().getName() + "." + field.getName());
                    }
                } else if (Set.class.isAssignableFrom(field.getType())) {
                    Set setValue = (Set) fieldValue;
                    if (!setValue.isEmpty()) {
                        nonCascadedEntities.addAll(setValue);
                        log.trace("Extracted non-cascaded fields {} from {}", setValue, entityToSave.getClass().getName() + "." + field.getName());
                    }
                } else if (List.class.isAssignableFrom(field.getType())) {
                    List listValue = (List) fieldValue;
                    if (!listValue.isEmpty()) {
                        nonCascadedEntities.addAll(listValue);
                        log.trace("Extracted non-cascaded fields {} from {}", listValue, entityToSave.getClass().getName() + "." + field.getName());
                    }
                    // A single value
                } else {
                    nonCascadedEntities.add(fieldValue);
                    log.trace("Extracted non-cascaded fields {} from {}", fieldValue, entityToSave.getClass().getName() + "." + field.getName());
                }

            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException("Failed to access field " + field.getName() + " in class " + entityToSave.getClass().getName(), e);
            }

        }
        return nonCascadedEntities;
    }

    /**
     * Copy data from deserialized entity to an entity from DB
     * 
     * @param entityFromDB Entity found in DB
     * @param entityDeserialized Entity deserialised
     * @param lookupById Should a lookup of existing entity in DB be done by ID or by attributes
     * @param importStats Import statistics
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     * @return A updated
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateEntityFoundInDB(IEntity entityFromDB, IEntity entityDeserialized, boolean lookupById, ExportImportStatistics importStats, Provider forceToProvider) {

        if (HibernateProxy.class.isAssignableFrom(entityFromDB.getClass())) {
            entityFromDB = (IEntity) ((HibernateProxy) entityFromDB).getHibernateLazyInitializer().getImplementation();
        }

        // Update id and version fields, so if entity was referred from other importing entities, it would be referring to a newly saved entity
        entityDeserialized.setId((Long) entityFromDB.getId());
        log.trace("Deserialized entity updated with id {}", entityFromDB.getId());

        if (IVersionedEntity.class.isAssignableFrom(entityDeserialized.getClass())) {
            ((IVersionedEntity) entityDeserialized).setVersion(((IVersionedEntity) entityFromDB).getVersion());
            log.trace("Deserialized entity updated with version {}", ((IVersionedEntity) entityFromDB).getVersion());
        }

        Class clazz = entityDeserialized.getClass();
        Class cls = clazz;
        while (!Object.class.equals(cls) && cls != null) {

            for (Field field : cls.getDeclaredFields()) {
                try {
                    // Do not overwrite id, version and static fields
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Version.class) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    Object sourceValue = FieldUtils.readField(field, entityDeserialized, true);

                    // Do not overwrite fields that should have been omitted during export, unless they are not empty
                    if (sourceValue == null && attributesToOmit.containsKey(clazz.getName() + "." + field.getName())) {
                        // log.error("AKK value is to be ommited {} target value is {}", clazz.getName() + "." + field.getName(), FieldUtils.readField(field, entityFromDB, true));
                        continue;
                    }

                    // Do not overwrite @oneToMany and @oneToOne fields THAT DO NOT CASCADE as they wont be saved anyway - that is handled apart in saveEntityToTarget()
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToManyAnotation = field.getAnnotation(OneToMany.class);
                        if (!(ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.MERGE) || ArrayUtils
                            .contains(oneToManyAnotation.cascade(), CascadeType.PERSIST))) {
                            continue;
                        }

                        // Extract @oneToOne fields that do not cascade
                    } else if (field.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOneAnotation = field.getAnnotation(OneToOne.class);
                        if (!(ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.MERGE) || ArrayUtils
                            .contains(oneToOneAnotation.cascade(), CascadeType.PERSIST))) {
                            continue;
                        }
                    }

                    // Save related entities that were not saved during main entity saving
                    sourceValue = saveNotManagedField(sourceValue, entityDeserialized, field, lookupById, importStats, clazz, forceToProvider);

                    // Populate existing Map, List and Set type fields by modifying field contents instead of rewriting a whole field
                    if (Map.class.isAssignableFrom(field.getType())) {
                        Map targetValue = (Map) FieldUtils.readField(field, entityFromDB, true);
                        if (targetValue != null) {
                            targetValue.clear();
                            targetValue.putAll((Map) sourceValue);
                        } else {
                            FieldUtils.writeField(field, entityFromDB, sourceValue, true);
                        }
                        log.trace("Populating field {} with {}", field.getName(), sourceValue);

                    } else if (Set.class.isAssignableFrom(field.getType())) {
                        Set targetValue = (Set) FieldUtils.readField(field, entityFromDB, true);
                        if (targetValue != null) {
                            targetValue.clear();
                            targetValue.addAll((Set) sourceValue);
                        } else {
                            FieldUtils.writeField(field, entityFromDB, sourceValue, true);
                        }
                        log.trace("Populating field {} with {}", field.getName(), sourceValue);

                    } else if (List.class.isAssignableFrom(field.getType())) {
                        List targetValue = (List) FieldUtils.readField(field, entityFromDB, true);
                        if (targetValue != null) {
                            targetValue.clear();
                            targetValue.addAll((List) sourceValue);
                        } else {
                            FieldUtils.writeField(field, entityFromDB, sourceValue, true);
                        }

                        log.trace("Populating field {} with {}", field.getName(), sourceValue);

                    } else {

                        log.trace("Setting field {} to {} ", field.getName(), sourceValue);
                        FieldUtils.writeField(field, entityFromDB, sourceValue, true);
                    }

                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new RuntimeException("Failed to access field " + field.getName() + " in class " + clazz.getName(), e);
                }
            }
            cls = cls.getSuperclass();
        }

        // entityFromDB = emTarget.merge(entityFromDB);

    }

    /**
     * Determine if fields that are entity type fields are managed, and if they are not managed yet - save them first
     * 
     * @param entityDeserialized Entity deserialised
     * @param lookupById Should a lookup of existing entity in DB be done by ID or by attributes
     * @param importStats Import statistics
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     */
    @SuppressWarnings({ "rawtypes" })
    private void saveNotManagedFields(IEntity entityDeserialized, boolean lookupById, ExportImportStatistics importStats, Provider forceToProvider) {

        Class clazz = entityDeserialized.getClass();
        Class cls = clazz;
        while (!Object.class.equals(cls) && cls != null) {

            for (Field field : cls.getDeclaredFields()) {
                try {
                    // Do not overwrite id, version and static fields
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Version.class) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    saveNotManagedField(null, entityDeserialized, field, lookupById, importStats, clazz, forceToProvider);

                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new RuntimeException("Failed to access field " + clazz.getSimpleName() + "." + field.getName(), e);
                }
            }
            cls = cls.getSuperclass();
        }
    }

    /**
     * Determine if field is an entity type field, and if it is not managed yet - save it first. TODO fix here as when lookupById, id value would be filled already
     * 
     * @param fieldValue Field value
     * @param entity Entity to obtain field value from if one is not provided. Also used to update the value if it was saved (after merge)
     * @param field Field definition
     * @param lookupById Is a lookup for FK or entity duplication performed by ID or attributes
     * @param importStats Import statistics
     * @param clazz A class of an entity that this field belongs to
     * @param forceToProvider Ignore provider specified in an entity and force provider value to this value
     * @throws IllegalAccessException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object saveNotManagedField(Object fieldValue, IEntity entity, Field field, boolean lookupById, ExportImportStatistics importStats, Class clazz, Provider forceToProvider)
            throws IllegalAccessException {

        // If field value was not passed - get it from an entity
        if (fieldValue == null) {
            fieldValue = FieldUtils.readField(field, entity, true);
            if (fieldValue == null) {
                return fieldValue;
            }
        }

        // Do not care about @oneToMany and @OneToOne fields that do not cascade they will be ignored anyway and their saving is handled in saveEntityToTarget()
        boolean isCascadedField = false;
        if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany oneToManyAnotation = field.getAnnotation(OneToMany.class);
            if (!(ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.MERGE) || ArrayUtils
                .contains(oneToManyAnotation.cascade(), CascadeType.PERSIST))) {
                return fieldValue;
            } else {
                isCascadedField = true;
            }

            // Extract @oneToOne fields that do not cascade
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne oneToOneAnotation = field.getAnnotation(OneToOne.class);
            if (!(ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.MERGE) || ArrayUtils.contains(
                oneToOneAnotation.cascade(), CascadeType.PERSIST))) {
                return fieldValue;
            } else {
                isCascadedField = true;
            }
        }

        // Do not care about non-entity type fields
        if (!checkIfFieldIsOfType(field, IEntity.class)) {
            return fieldValue;
        }

        // Ensure that field value is managed (or saved) before continuing. It calls saveEntityToTarget with updateExistingOnly = true for cascaded fields. That means that new
        // cascaded field values will be created with main entity saving.
        boolean isManaged = false;

        // Examine Map, List and Set type fields to see if they are persisted, and if not - persist them
        if (Map.class.isAssignableFrom(field.getType())) {
            Map mapValue = (Map) fieldValue;
            for (Object entry : mapValue.entrySet()) {
                Object key = ((Entry) entry).getKey();
                Object singleValue = ((Entry) entry).getValue();
                if (singleValue == null) {
                    continue;
                }
                // If entity is managed, then continue on unless detached. Update value in a map with a new value. TODO fix here as when lookupById, id value would be filled
                // already
                isManaged = ((IEntity) singleValue).getId() != null; // emTarget.contains(singleValue);
                if (!isManaged) {
                    log.debug("Persisting child field {}.{}'s (cascaded={}) value {}", clazz.getSimpleName(), field.getName(), isCascadedField, singleValue);
                    mapValue.put(key, saveEntityToTarget((IEntity) singleValue, lookupById, importStats, isCascadedField, forceToProvider));

                    // // Is managed, but detached - need to detach it again
                    // // Don't know why it fails on permission class only. Problem arises when converter in another iEntityIdentifierConverter finds an entity, but it as it runs
                    // in a
                    // // separate session, it gets detached for a next entity. It happens for all entities,but throws an error for Permission class only.
                    // } else if (!getEntityManagerForImport().contains(singleValue) && !(fieldValue instanceof Provider)) {
                    // log.trace("Persisting child field {}.{} is managed BUT detached. Object id={} will be refreshed.", clazz.getSimpleName(), field.getName(),
                    // ((IEntity) singleValue).getId());
                    // singleValue = getEntityManagerForImport().merge(singleValue);
                    // mapValue.put(key, singleValue);

                    // } else {
                    // log.trace("Persisting child field {}.{} is managed", clazz.getSimpleName(), field.getName());
                }
            }

        } else if (Collection.class.isAssignableFrom(field.getType())) {
            Collection collectionValue = (Collection) fieldValue;
            Object[] collectionValues = collectionValue.toArray();

            // Clear and construct collection again with updated values (if were not managed before)
            collectionValue.clear();
            for (Object singleValue : collectionValues) {
                if (singleValue == null) {
                    continue;
                }
                // If entity is not managed, then save it. TODO fix here as when lookupById, id value would be filled already
                isManaged = ((IEntity) singleValue).getId() != null; // emTarget.contains(singleValue);
                if (!isManaged) {
                    log.debug("Persisting child field {}.{}'s (cascaded={}) value {}", clazz.getSimpleName(), field.getName(), isCascadedField, singleValue);
                    collectionValue.add(saveEntityToTarget((IEntity) singleValue, lookupById, importStats, isCascadedField, forceToProvider));

                } else {
                    // // Is managed, but detached - need to detach it again
                    // // Don't know why it fails on permission class only. Problem arises when converter in another iEntityIdentifierConverter finds an entity, but it as it runs
                    // in a
                    // // separate session, it gets detached for a next entity. It happens for all entities,but throws an error for Permission class only.
                    // if (!getEntityManagerForImport().contains(singleValue) && !(fieldValue instanceof Provider)) {
                    // log.trace("Persisting child field {}.{} is managed BUT detached. Object id={} will be refreshed.", clazz.getSimpleName(), field.getName(),
                    // ((IEntity) singleValue).getId());
                    // singleValue = getEntityManagerForImport().merge(singleValue);
                    // }
                    collectionValue.add(singleValue);
                    // log.trace("Persisting child field {}.{} is managed", clazz.getSimpleName(), field.getName());
                }
            }

        } else {

            // If entity is not managed, then save it.
            // TODO emTarget.contains(fieldValue) fails - need to fix this temporary solution as when lookupById is used, id value would be
            // filled already for an entity this this .getId() != null would always be true
            isManaged = ((IEntity) fieldValue).getId() != null; // emTarget.contains(fieldValue);
            if (!isManaged) {
                log.debug("Persisting child field {}.{}'s (cascaded={}) value {}", clazz.getSimpleName(), field.getName(), isCascadedField, fieldValue);
                fieldValue = saveEntityToTarget((IEntity) fieldValue, lookupById, importStats, isCascadedField, forceToProvider);
                // Update field value in an entity with a new value
                FieldUtils.writeField(field, entity, fieldValue, true);

                // // Is managed, but detached - need to detach it again
                // // Don't know why it fails on permission class only. Problem arises when converter in another iEntityIdentifierConverter finds an entity, but it as it runs in a
                // // separate session, it gets detached for a next entity. It happens for all entities,but throws an error for Permission class only.
                // } else if (!getEntityManagerForImport().contains(fieldValue) && !(fieldValue instanceof Provider)) {
                // log.trace("Persisting child field {}.{} is managed BUT detached. Object id={} will be refreshed.", clazz.getSimpleName(), field.getName(),
                // ((IEntity) fieldValue).getId());
                // fieldValue = getEntityManagerForImport().merge(fieldValue);

                // } else {
                // log.trace("Persisting field {}.{} is managed", clazz.getSimpleName(), field.getName());
            }
        }

        return fieldValue;
    }

    /**
     * Find an entity in target db by attributes
     * 
     * @param entityToSave Entity to match
     * @return Entity found in target DB
     */
    private IEntity findEntityByAttributes(IEntity entityToSave) {
        String[] attributes = exportIdMapping.get(entityToSave.getClass());
        if (attributes == null) {
            return null;
        }

        Map<String, Object> parameters = new HashMap<String, Object>();

        for (String attributeName : attributes) {

            Object attrValue;
            try {
                attrValue = getAttributeValue(entityToSave, attributeName);
                if (attrValue != null) {
                    // Can not search by an entity which was not saved yet. Happens when creating an entity hierarchy and child field is cascadable and one of it's attributes for
                    // search is parent entity, which does not exist yet.
                    if (attrValue instanceof IEntity && ((IEntity) attrValue).isTransient()) {
                        return null;
                    }
                    parameters.put(attributeName, attrValue);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + entityToSave.getClass().getName() + "." + attributeName + "field", e);
            }
        }

        // Construct a query to retrieve an entity by the attributes
        StringBuilder sql = new StringBuilder("select o from " + entityToSave.getClass().getName() + " o where ");
        boolean firstWhere = true;
        for (Entry<String, Object> param : parameters.entrySet()) {
            if (!firstWhere) {
                sql.append(" and ");
            }
            sql.append(String.format(" %s=:%s", param.getKey(), param.getKey().replace('.', '_')));
            firstWhere = false;
        }
        Query query = getEntityManagerForImport().createQuery(sql.toString());
        for (Entry<String, Object> param : parameters.entrySet()) {
            query.setParameter(param.getKey().replace('.', '_'), param.getValue());
        }
        try {
            IEntity entity = (IEntity) query.getSingleResult();
            log.trace("Found entity {} id={} with attributes {}. Entity will be updated.", entity.getClass().getName(), entity.getId(), parameters);
            return entity;

        } catch (NoResultException | NonUniqueResultException e) {
            log.debug("Entity {} not found matching attributes: {}, sql {}. Reason:{} Entity will be inserted.", entityToSave.getClass().getName(), parameters, sql, e.getClass()
                .getName());
            return null;

        } catch (Exception e) {
            log.error("Failed to search for entity {} with attributes: {}, sql {}", entityToSave.getClass().getName(), parameters, sql, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an attribute value. Handles composed attribute cases (e.g. provider.code)
     * 
     * @param object Object to get attribute value from
     * @param attributeName Attribute name. Can be a composed attribute name
     * @return Attribute value
     * @throws IllegalAccessException
     */
    private Object getAttributeValue(Object object, String attributeName) throws IllegalAccessException {

        Object value = object;
        StringTokenizer tokenizer = new StringTokenizer(attributeName, ".");
        while (tokenizer.hasMoreElements()) {
            String attrName = tokenizer.nextToken();
            value = FieldUtils.readField(value, attrName, true);
            if (value instanceof HibernateProxy) {
                value = ((HibernateProxy) value).getHibernateLazyInitializer().getImplementation();
            } else if (value == null) {
                return null;
            }
        }
        return value;
    }

    /**
     * Determine what attributes are treated as identifiers for export for an entity. Such information is provided by @ExportIdentifier annotation on an entity.
     * 
     * @return A map with such format: <Entity class, an array of entity attribute names>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void loadExportIdentifierMappings() {
        Map<Class<? extends IEntity>, String[]> exportIdMap = new HashMap<Class<? extends IEntity>, String[]>();

        Reflections reflections = new Reflections("org.meveo.model");
        Set<Class<? extends IEntity>> classes = reflections.getSubTypesOf(IEntity.class);

        for (Class clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isAnnotation() || !IEntity.class.isAssignableFrom(clazz)) {
                continue;
            }

            if (clazz.isAnnotationPresent(ExportIdentifier.class)) {
                exportIdMap.put(clazz, ((ExportIdentifier) clazz.getAnnotation(ExportIdentifier.class)).value());
            }

        }
        exportIdMapping = exportIdMap;
    }

    /**
     * Determine what attributes should be omitted for export for an entity. Attributes annotated with @OneToMany annotation should be omitted.
     * 
     * @return A map of <classname.fieldname,array of [Class, attribute name]>
     */
    @SuppressWarnings({ "rawtypes" })
    private void loadAtributesToOmit() {
        Map<String, Object[]> attributesToOmitLocal = new HashMap<String, Object[]>();

        Reflections reflections = new Reflections("org.meveo.model");
        Set<Class<? extends IEntity>> classes = reflections.getSubTypesOf(IEntity.class);

        for (Class clazz : classes) {

            if (clazz.isInterface() || clazz.isAnnotation() || !IEntity.class.isAssignableFrom(clazz)) {
                continue;
            }

            Class cls = clazz;
            while (!Object.class.equals(cls) && cls != null) {

                for (Field field : cls.getDeclaredFields()) {

                    if (field.isAnnotationPresent(Transient.class)) {
                        attributesToOmitLocal.put(clazz.getName() + "." + field.getName(), new Object[] { clazz, field });

                    } else if (field.isAnnotationPresent(OneToMany.class)) {

                        // Omit attribute only if backward relationship is set
                        // boolean hasBackwardRelationship = checkIfClassContainsFieldOfType(field.getGenericType(), clazz);
                        // if (hasBackwardRelationship) {
                        attributesToOmitLocal.put(clazz.getName() + "." + field.getName(), new Object[] { clazz, field });
                        // } else {
                        // log.error("AKK field " + field.getName() + " of generic type " + field.getGenericType() + "will not be omitted from " + clazz.getSimpleName());
                        // }
                    }
                }

                cls = cls.getSuperclass();
            }
        }
        attributesToOmit = attributesToOmitLocal;
    }

    /**
     * Identify fields in classes that contain a list of related entities (@OneToMany and @OneToOne), but are not cascaded
     * 
     * @return A map of <Class, List of non-cascaded fields>
     */
    @SuppressWarnings("rawtypes")
    private void loadNonCascadableFields() {

        Map<Class, List<Field>> nonCascadableFieldsLocal = new HashMap<Class, List<Field>>();

        Reflections reflections = new Reflections("org.meveo.model");
        Set<Class<? extends IEntity>> classes = reflections.getSubTypesOf(IEntity.class);

        for (Class clazz : classes) {
            if (clazz.isInterface() || clazz.isAnnotation() || !IEntity.class.isAssignableFrom(clazz)) {
                continue;
            }
            List<Field> classNonCascadableFields = new ArrayList<Field>();

            Class cls = clazz;
            while (!Object.class.equals(cls) && cls != null) {
                for (Field field : cls.getDeclaredFields()) {

                    // Skip id, version and static fields
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Version.class) || Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    // Extract @oneToMany fields that do not cascade
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        OneToMany oneToManyAnotation = field.getAnnotation(OneToMany.class);
                        if (!(ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToManyAnotation.cascade(), CascadeType.MERGE) || ArrayUtils
                            .contains(oneToManyAnotation.cascade(), CascadeType.PERSIST))) {
                            classNonCascadableFields.add(field);
                        }

                        // Extract @oneToOne fields that do not cascade
                    } else if (field.isAnnotationPresent(OneToOne.class)) {
                        OneToOne oneToOneAnotation = field.getAnnotation(OneToOne.class);
                        if (!(ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.ALL) || ArrayUtils.contains(oneToOneAnotation.cascade(), CascadeType.MERGE) || ArrayUtils
                            .contains(oneToOneAnotation.cascade(), CascadeType.PERSIST))) {
                            classNonCascadableFields.add(field);
                        }
                    }
                }

                cls = cls.getSuperclass();
            }
            if (!classNonCascadableFields.isEmpty()) {
                nonCascadableFieldsLocal.put(clazz, classNonCascadableFields);
            }
        }
        nonCascadableFields = nonCascadableFieldsLocal;
    }

    /**
     * Check if parameterized class contains a non-transient field of given type
     * 
     * @param type Parameterized type to examine
     * @param classToMatch Class type to match
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unused" })
    private boolean checkIfClassContainsFieldOfType(Type type, Class classToMatch) {
        Class classToCheck = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) type;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            for (Type fieldArgType : fieldArgTypes) {
                Class fieldArgClass = (Class) fieldArgType;
                if (IEntity.class.isAssignableFrom(fieldArgClass)) {
                    classToCheck = fieldArgClass;
                    break;
                }
            }
        }

        for (Field field : classToCheck.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Transient.class) && IEntity.class.isAssignableFrom(field.getDeclaringClass()) && field.getType().isAssignableFrom(classToMatch)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class field is of type - including List<>, Map<>, Set<> and potentially other parameterized classes
     * 
     * @param field Field to analyse
     * @param typesToCheck Class to match
     * @return True is field is of type classToMatch or is parameterized with classToMatch class (e.g. List<classToMatch>
     */
    private boolean checkIfFieldIsOfType(Field field, Collection<Class<? extends IEntity>> typesToCheck) {
        for (Class<? extends IEntity> typeToCheck : typesToCheck) {
            boolean isOfType = checkIfFieldIsOfType(field, typeToCheck);
            if (isOfType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class field is of type - including List<>, Map<>, Set<> and potentially other parameterized classes
     * 
     * @param field Field to analyse
     * @param typeToCheck Class to match
     * @return True is field is of type classToMatch or is parameterized with classToMatch class (e.g. List<classToMatch>
     */
    @SuppressWarnings("rawtypes")
    private boolean checkIfFieldIsOfType(Field field, Class<? extends IEntity> typeToCheck) {
        if (typeToCheck.isAssignableFrom(field.getType())) {
            return true;
        } else if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) field.getGenericType();
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            for (Type fieldArgType : fieldArgTypes) {
                Class fieldArgClass = (Class) fieldArgType;
                if (typeToCheck.isAssignableFrom(fieldArgClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Specify Xstream what attributes to omit.
     * 
     * @param xstream Instance to apply to
     * @param typesNotToOmit Types not to omit
     * @param typesNotToOmit
     */
    @SuppressWarnings("rawtypes")
    private void applyAttributesToOmit(XStream xstream, Collection<Class<? extends IEntity>> typesNotToOmit) {
        for (Object[] classFieldInfo : attributesToOmit.values()) {
            if (typesNotToOmit != null && checkIfFieldIsOfType((Field) classFieldInfo[1], typesNotToOmit)) {
                log.trace("Explicitly not omitting {}.{} attribute from export", classFieldInfo[0], ((Field) classFieldInfo[1]).getName());
                continue;
            }
            log.trace("Will ommit {}.{} attribute from export", classFieldInfo[0], ((Field) classFieldInfo[1]).getName());
            xstream.omitField((Class) classFieldInfo[0], ((Field) classFieldInfo[1]).getName());
        }
        xstream.omitField(Auditable.class, "creator");
        xstream.omitField(Auditable.class, "updater");
    }

    /**
     * Obtain a list of entities to export in paginated form to be able to handle large amount of data
     * 
     * @param exportTemplate Export template
     * @param from Starting record index
     * @param pageSize Page size
     * @param parameters Filter parameters to retrieve entities from DB
     * @param dataModelToExport Entities to export that are already filtered in a data model. Supports export of non-grouped export templates only.
     * @return A list of entities corresponding to a page
     */
    @SuppressWarnings({ "unchecked" })
    private List<? extends IEntity> getEntitiesToExport(ExportTemplate exportTemplate, Map<String, Object> parameters, DataModel<? extends IEntity> dataModelToExport, int from,
            int pageSize) {

        // Retrieve next pageSize number of entities from iterator
        if (dataModelToExport != null) {
            if (from >= dataModelToExport.getRowCount()) {
                return new ArrayList<IEntity>();
            }

            if (dataModelToExport instanceof LazyDataModel) {
                return ((LazyDataModel<? extends IEntity>) dataModelToExport).load(from, pageSize, null, null, null);

            } else {
                List<? extends IEntity> modelData = (List<? extends IEntity>) dataModelToExport.getWrappedData();
                return modelData.subList(from, Math.min(from + pageSize, modelData.size()));

            }

        } else {

            // Construct a query to retrieve entities to export by selection criteria. OR examine selection criteria - could be that top export entity matches search criteria for
            // related entities (e.g. exporting provider and related info and some provider is search criteria, but also it matches the top entity)
            StringBuilder sql = new StringBuilder("select e from " + exportTemplate.getEntityToExport().getName() + " e  ");
            boolean firstWhere = true;
            Map<String, Object> parametersToApply = new HashMap<String, Object>();
            for (Entry<String, Object> param : parameters.entrySet()) {
                String paramName = param.getKey();
                Object paramValue = param.getValue();

                if (paramValue == null) {
                    continue;

                    // Handle the case when top export entity matches search criteria for related entities (e.g. exporting provider and related info. If search criteria is Provider
                    // -
                    // for related entities it is just search criteria, but for top entity (provider) it has to match it)
                } else if (exportTemplate.getEntityToExport().isAssignableFrom(paramValue.getClass())) {
                    sql.append(firstWhere ? " where " : " and ").append(" id=:id");
                    firstWhere = false;
                    parametersToApply.put("id", ((IEntity) paramValue).getId());

                } else {
                    String fieldName = paramName;
                    String fieldCondition = "=";
                    if (fieldName.contains("_")) {
                        String[] paramInfo = fieldName.split("_");
                        fieldName = paramInfo[0];
                        fieldCondition = "from".equals(paramInfo[1]) ? ">" : "to".equals(paramInfo[1]) ? "<" : "=";
                    }

                    Field field = FieldUtils.getField(exportTemplate.getEntityToExport(), fieldName, true);
                    if (field == null) {
                        continue;
                    }

                    sql.append(firstWhere ? " where " : " and ").append(String.format(" %s%s:%s", fieldName, fieldCondition, paramName));
                    firstWhere = false;
                    parametersToApply.put(paramName, paramValue);
                }
            }

            // Do a search

            TypedQuery<IEntity> query = getEntityManager().createQuery(sql.toString(), IEntity.class).setFirstResult(from).setMaxResults(pageSize);
            for (Entry<String, Object> param : parametersToApply.entrySet()) {
                if (param.getValue() != null) {
                    query.setParameter(param.getKey(), param.getValue());
                }
            }
            List<IEntity> entities = query.getResultList();
            return entities;
        }
    }

    public static class ExportInfo {

        public ExportInfo(ExportTemplate exportTemplate, String serializedData) {
            this.exportTemplate = exportTemplate;
            this.serializedData = serializedData;
        }

        ExportTemplate exportTemplate;
        String serializedData;
    }

    // /**
    // * Extend a default XppWriter just to extend PrettyPrintWriter
    // *
    // * @author Andrius Karpavicius
    // *
    // */
    // private class ExtityExportXppDriver extends XppDriver {
    //
    // @Override
    // public HierarchicalStreamWriter createWriter(Writer out) {
    // return new EntityExportWriter(out, getNameCoder());
    // }
    //
    // }

    /**
     * A writer extending PrettyPrintWriter to handle issue when "class" attribute is added twice - once by Xstream's AbstractReflectionConverter.doMarshal() and second time by
     * IEntityExportIdentifierConverter
     * 
     * @author Andrius Karpavicius
     * 
     */
    private class EntityExportWriter extends PrettyPrintWriter {

        private boolean attributeClassAdded = false;

        public EntityExportWriter(Writer out) {
            super(out);
        }

        public EntityExportWriter(Writer out, NameCoder nameCoder) {
            super(out, nameCoder);
        }

        @Override
        public void addAttribute(String key, String value) {
            if (key.equals("class")) {
                if (attributeClassAdded) {
                    return;
                }
                attributeClassAdded = true;
            }
            super.addAttribute(key, value);
        }

        @Override
        public void endNode() {
            super.endNode();
            attributeClassAdded = false;
        }
    }

    public static class ReusingReferenceByIdMarshallingStrategy implements MarshallingStrategy {

        private ReferenceByIdMarshaller marshaller;
        private ReferenceByIdUnmarshaller unmarshaller;

        public void marshal(HierarchicalStreamWriter writer, Object obj, ConverterLookup converterLookup, Mapper mapper, DataHolder dataHolder) {
            if (marshaller == null) {
                marshaller = new ReferenceByIdMarshaller(writer, converterLookup, mapper);
            }
            marshaller.start(obj, dataHolder);
        }

        public Object unmarshal(Object root, HierarchicalStreamReader reader, DataHolder dataHolder, ConverterLookup converterLookup, Mapper mapper) {
            if (unmarshaller == null) {
                unmarshaller = new ReferenceByIdUnmarshaller(root, reader, converterLookup, mapper);
            }
            return unmarshaller.start(dataHolder);
        }
    }

    private void refreshCaches() {
        log.info("Initiating cache reload after import ");
        walletCacheContainerProvider.refreshCache(null);
        cdrEdrProcessingCacheContainerProvider.refreshCache(null);
        notificationCacheContainerProvider.refreshCache(null);
        ratingCacheContainerProvider.refreshCache(null);
        // customFieldsCacheContainerProvider.refreshCache(null);
    }

    /**
     * Actualize contents of export file to a current version of data model. Contents are actualized by xslt transformation.
     * 
     * @param sourceFile File to actualize
     * @param sourceFilename A name of a file to actualize - passes separately as file might be saved as temp files along the way
     * @param sourceVersion Version in a source file
     * @return A converted file
     * @throws IOException
     * @throws TransformerException
     */
    private File actualizeVersionOfExportFile(File sourceFile, String sourceFilename, String sourceVersion) throws IOException, TransformerException {
        log.debug("Actualizing the version of export file {}. Current version is {}", sourceFilename, sourceVersion);

        Source source = null;
        // Handle zip file
        if (sourceFilename.endsWith(".zip")) {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile));
            zis.getNextEntry();
            source = new StreamSource(zis);

        } else {
            source = new StreamSource(sourceFile);
        }

        File finalFile = null;
        String finalVersion = null;
        List<File> tempFiles = new ArrayList<File>();
        TransformerFactory factory = TransformerFactory.newInstance();
        for (Entry<String, String> changesetInfo : getApplicableExportModelVersionChangesets(sourceVersion).entrySet()) {
            String changesetVersion = changesetInfo.getKey();
            String changesetFile = changesetInfo.getValue();
            File tempFile = File.createTempFile(FilenameUtils.getBaseName(sourceFilename) + "_" + changesetVersion, ".xml");
            tempFiles.add(tempFile);
            log.trace("Transforming {} to version {}, targetFileName {}", sourceFilename, changesetVersion, tempFile.getAbsolutePath());
            try {
                Transformer transformer = factory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/" + changesetFile)));
                transformer.setParameter("version", changesetVersion);
                transformer.transform(source, new StreamResult(tempFile));
            } catch (TransformerException e) {
                log.error("Failed to transform {} to version {}, targetFileName {}", sourceFilename, changesetVersion, tempFile.getAbsolutePath(), e);
                throw e;
            }
            source = new StreamSource(tempFile);
            finalFile = tempFile;
            finalVersion = changesetVersion;
        }

        // Remove intermediary temp files except the final one
        tempFiles.remove(finalFile);
        for (File file : tempFiles) {
            try {
                file.delete();
            } catch (Exception e) {
                log.error("Failed to delete a temp file {}", file.getAbsolutePath(), e);
            }
        }
        log.info("Actualized the version of export file {} from {} to {} version", sourceFilename, sourceVersion, finalVersion);

        return finalFile;
    }

    /**
     * Actualize contents of export file to a current version of data model. Contents are actualized by xslt transformation.
     * 
     * @param sourceData XML data to actualize
     * @return A converted data
     * @throws IOException
     * @throws TransformerException
     */
    private String actualizeVersionOfExportFile403FileVersion(String sourceData) {
        log.debug("Actualizing the version of export data from 4.0.3 version.");

        String finalVersion = null;
        String dataToTransform = sourceData;
        TransformerFactory factory = TransformerFactory.newInstance();
        for (Entry<String, String> changesetInfo : exportModelVersionChangesets.entrySet()) {
            String changesetVersion = changesetInfo.getKey();
            String changesetFile = changesetInfo.getValue();
            log.trace("Transforming data to version {}", changesetVersion);
            try {
                Transformer transformer = factory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/" + changesetFile)));
                StringWriter writer = new StringWriter();
                transformer.transform(new StreamSource(new StringReader(dataToTransform)), new StreamResult(writer));
                dataToTransform = writer.toString();
                finalVersion = changesetVersion;

            } catch (TransformerException e) {
                log.error("Failed to transform data to version {} data {}", changesetVersion, dataToTransform, e);
                throw new RuntimeException(e);
            }
        }

        log.info("Actualized the version of export data from 4.0.3 to {} version", finalVersion);
        log.trace("Converted data {}", dataToTransform);

        return dataToTransform;
    }

    /**
     * Get applicable export model version upgrade changesets. Changeset is applicable when it's version is higher than the sourceVersion value
     * 
     * @param sourceVersion Version to upgrade
     * @return A map of changesets with version changeset number as a key and changeset file as a value
     */
    private LinkedHashMap<String, String> getApplicableExportModelVersionChangesets(String sourceVersion) {

        LinkedHashMap<String, String> applicableChangesets = new LinkedHashMap<String, String>();
        for (Entry<String, String> changesetInfo : exportModelVersionChangesets.entrySet()) {
            if (changesetInfo.getKey().compareTo(sourceVersion) > 0) {
                applicableChangesets.put(changesetInfo.getKey(), changesetInfo.getValue());
            }
        }
        return applicableChangesets;

    }

    /**
     * Load export model version update changesets
     */
    private void loadExportModelVersionChangesets() {
        Set<String> changesets = new Reflections("exportVersions", new ResourcesScanner()).getResources(Pattern.compile("changeSet_.*\\.xslt"));
        ArrayList<String> sortedChangesets = new ArrayList<String>();
        sortedChangesets.addAll(changesets);
        Collections.sort(sortedChangesets);

        exportModelVersionChangesets = new LinkedHashMap<String, String>();
        for (String changesetFile : sortedChangesets) {
            String version = changesetFile.substring(changesetFile.indexOf("_") + 1, changesetFile.indexOf(".xslt"));
            exportModelVersionChangesets.put(version, changesetFile);
            currentExportModelVersionChangeset = version;
        }
    }

    /**
     * Upload file to a remote meveo instance
     * 
     * @param filename Path to a file to upload
     * @param remoteInstance Remote meveo instance
     * @throws Exception
     */
    private String uploadFileToRemoteMeveoInstance(String filename, MeveoInstance remoteInstance) throws Exception {
        try {

            log.debug("Uplading {} file to a remote meveo instance {}", filename, remoteInstance.getCode());

            ResteasyClient client = new ResteasyClientBuilder().build();
            ResteasyWebTarget target = client.target(remoteInstance.getUrl() + (remoteInstance.getUrl().endsWith("/") ? "" : "/") + "api/rest/importExport/importData");

            BasicAuthentication basicAuthentication = new BasicAuthentication(remoteInstance.getAuthUsername(), remoteInstance.getAuthPassword());
            target.register(basicAuthentication);

            MultipartFormDataOutput mdo = new MultipartFormDataOutput();
            mdo.addFormData("file", new FileInputStream(new File(filename)), MediaType.APPLICATION_OCTET_STREAM_TYPE, filename);
            GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
            };

            Response response = target.request().post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
            if (response.getStatus() != HttpURLConnection.HTTP_OK) {
                if (response.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED || response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new RemoteAuthenticationException(response.getStatusInfo().getReasonPhrase());
                } else {
                    throw new RemoteImportException("Failed to communicate or process data in remote meveo instance. Http status " + response.getStatus() + " "
                            + response.getStatusInfo().getReasonPhrase());
                }
            }
            ImportExportResponseDto resultDto = response.readEntity(ImportExportResponseDto.class);
            if (resultDto.isFailed()) {
                if (MeveoApiErrorCode.AUTHENTICATION_AUTHORIZATION_EXCEPTION.equals(resultDto.getActionStatus().getErrorCode())) {
                    throw new RemoteAuthenticationException(resultDto.getFailureMessage());
                }
                throw new RemoteImportException(resultDto.getFailureMessage());
            }

            String executionId = resultDto.getExecutionId();
            log.info("Export file {} uploaded to a remote meveo instance {} with execution id {}", filename, remoteInstance.getCode(), executionId);

            return executionId;

        } catch (Exception e) {
            log.error("Failed to upload a file {} to a remote meveo instance {}", filename, remoteInstance.getUrl());
            throw e;
        }
    }

    /**
     * Check status and get results of file upload to a remote meveo instance
     * 
     * @param executionId Import in remote meveo instance execution id
     * @param remoteInstance Remote meveo instance
     * @throws RemoteAuthenticationException
     * @throws RemoteImportException
     * @throws Exception
     */
    public ImportExportResponseDto checkRemoteMeveoInstanceImportStatus(String executionId, MeveoInstance remoteInstance) throws RemoteAuthenticationException,
            RemoteImportException {

        log.debug("Checking status of import in remote meveo instance {} with execution id {}", remoteInstance.getCode(), executionId);

        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(remoteInstance.getUrl() + (remoteInstance.getUrl().endsWith("/") ? "" : "/")
                + "api/rest/importExport/checkImportDataResult?executionId=" + executionId);

        BasicAuthentication basicAuthentication = new BasicAuthentication(remoteInstance.getAuthUsername(), remoteInstance.getAuthPassword());
        target.register(basicAuthentication);

        Response response = target.request().get();// post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() != HttpURLConnection.HTTP_OK) {
            if (response.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED || response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new RemoteAuthenticationException(response.getStatusInfo().getReasonPhrase());
            } else {
                throw new RemoteImportException("Failed to communicate to remote meveo instance. Http status " + response.getStatus() + " "
                        + response.getStatusInfo().getReasonPhrase());
            }
        }
        ImportExportResponseDto resultDto = response.readEntity(ImportExportResponseDto.class);
        log.debug("The status of import in remote meveo instance {} with execution id {} is {}", remoteInstance.getCode(), executionId, resultDto);

        return resultDto;

    }
}