package org.meveo.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.infinispan.api.BasicCache;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.mediation.Access;
import org.meveo.model.rating.EDR;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.medina.impl.AccessService;
import org.slf4j.Logger;

/**
 * Provides cache related services (loading, update) for CDR and EDR processing related operations
 * 
 * @author Andrius Karpavicius
 * 
 */
@Startup
@Singleton
public class CdrEdrProcessingCacheContainerProvider {

    @Inject
    protected Logger log;

    @EJB
    private AccessService accessService;

    @EJB
    private EdrService edrService;

    private ParamBean paramBean = ParamBean.getInstance();

    /**
     * Contains association between access code and accesses sharing this code. Key format: <provider id>_<Access.accessUserId>
     */
    @Resource(lookup = "java:jboss/infinispan/cache/meveo/meveo-access-cache")
    private BasicCache<String, List<Access>> accessCache;

    // @Resource(name = "java:jboss/infinispan/container/meveo")
    // private CacheContainer meveoContainer;

    /**
     * Stores a list of processed EDR's. Key format: <provider id>_<originBatch>_<originRecord>
     */
    @Resource(lookup = "java:jboss/infinispan/cache/meveo/meveo-edr-cache")
    private BasicCache<String, Integer> edrCache;

    @PostConstruct
    private void init() {
        try {
            log.debug("CdrEdrProcessingCacheContainerProvider initializing...");
            // accessCache = meveoContainer.getCache("meveo-access-cache");
            // edrCache = meveoContainer.getCache("meveo-edr-cache");

            populateAccessCache();
            populateEdrCache();

            log.info("CdrEdrProcessingCacheContainerProvider initialized");

        } catch (Exception e) {
            log.error("CdrEdrProcessingCacheContainerProvider init() error", e);
            throw e;
        }
    }

    /**
     * Populate access cache from db
     */
    private void populateAccessCache() {

        log.debug("Start to populate access cache");
        accessCache.clear();
        List<Access> activeAccesses = accessService.getAccessesForCache();

        for (Access access : activeAccesses) {
            addAccessToCache(access);
        }

        log.info("Access cache populated with {} accesses", activeAccesses.size());
    }

    /**
     * Add access to a cache
     * 
     * @param access Access to add
     */
    public void addAccessToCache(Access access) {
        String cacheKey = access.getProvider().getId() + "_" + access.getAccessUserId();
        accessCache.putIfAbsent(cacheKey, new ArrayList<Access>());
        // because acccessed later, to avoid lazy init
        access.getSubscription().getId();
        accessCache.get(cacheKey).add(access);
        log.trace("Added access {} to access cache", access);
    }

    /**
     * Remove access from cache
     * 
     * @param access Access to remove
     */
    public void removeAccessFromCache(Access access) {

        // Case when AccessUserId value has not changed
        if (accessCache.containsKey(access.getProvider().getId() + "_" + access.getAccessUserId())
                && accessCache.get(access.getProvider().getId() + "_" + access.getAccessUserId()).contains(access)) {
            accessCache.get(access.getProvider().getId() + "_" + access.getAccessUserId()).remove(access);
            log.trace("Removed access {} from access cache", access);
            return;

            // Case when AccessUserId values has changed
        } else {
            for (List<Access> accesses : accessCache.values()) {
                if (accesses.contains(access)) {
                    accesses.remove(access);
                    log.trace("Removed access {} from access cache", access);
                    return;
                }
            }
        }
        log.error("Failed to find and remove access {} from access cache", access);
    }

    /**
     * Update access in cache
     * 
     * @param access Access to update
     */
    public void updateAccessInCache(Access access) {
        removeAccessFromCache(access);
        addAccessToCache(access);
    }

    /**
     * Get a list of accesses for a given provider and access user id
     * 
     * @param providerId Provider id
     * @param accessUserId Access user id
     * @return A list of accesses
     */
    public List<Access> getAccessesByAccessUserId(Long providerId, String accessUserId) {
        log.trace("lookup access {}_{}",providerId,accessUserId);
    	return accessCache.get(providerId + "_" + accessUserId);
    }

    /**
     * Populate EDR cache from db
     */
    private void populateEdrCache() {

        boolean useInMemoryDeduplication = paramBean.getProperty("mediation.deduplicateInMemory", "true").equals("true");
        if (!useInMemoryDeduplication) {
            log.info("EDR cache population will be skipped");
            return;
        }

        log.debug("Start to populate EDR cache");

        edrCache.clear();
        int maxDuplicateRecords = Integer.parseInt(paramBean.getProperty("mediation.deduplicateCacheSize", "100000"));
        List<String> edrs = edrService.getUnprocessedEdrsForCache(maxDuplicateRecords);
        for (String edrHash : edrs) {
            edrCache.put(edrHash, 0);
        }

        log.info("EDR cache populated with {} EDRs", edrs.size());
    }

    /**
     * Check if EDR is cached for a given provider, originBatch and originRecord
     * 
     * @param providerId Provider id
     * @param originBatch Origin batch
     * @param originRecord Origin record
     * @return True if EDR is cached
     */
    public boolean isEDRCached(Long providerId, String originBatch, String originRecord) {

        return edrCache.containsKey(providerId + "_" + originBatch + '_' + originRecord);
    }

    /**
     * Add EDR to cache
     * 
     * @param edr EDR to add to cache
     */
    public void addEdrToCache(EDR edr) {
        edrCache.putIfAbsent(edr.getProvider().getId() + "_" + edr.getOriginBatch() + "_" + edr.getOriginRecord(), 0);
    }

    /**
     * Get a summary of cached information
     * 
     * @return A list of a map containing cache information with cache name as a key and cache as a value
     */
    @SuppressWarnings("rawtypes")
    public Map<String, BasicCache> getCaches() {
        Map<String, BasicCache> summaryOfCaches = new HashMap<String, BasicCache>();
        summaryOfCaches.put(accessCache.getName(), accessCache);
        summaryOfCaches.put(edrCache.getName(), edrCache);

        return summaryOfCaches;
    }

    /**
     * Refresh cache by name
     * 
     * @param cacheName Name of cache to refresh or null to refresh all caches
     */
    @Asynchronous
    public void refreshCache(String cacheName) {

        if (cacheName == null || cacheName.equals(accessCache.getName())) {
            populateAccessCache();

        }
        if (cacheName == null || cacheName.equals(edrCache.getName())) {
            populateEdrCache();
        }
    }
}