package org.meveo.admin.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.IEntity;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.filter.Filter;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.service.filter.FilterService;
import org.meveo.service.script.ScriptInterface;
import org.slf4j.Logger;

@Stateless
public class FilteringJobBean {

    @Inject
    private Logger log;

    @Inject
    private FilterService filterService;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, String parameter, String filterCode, ScriptInterface scriptInterface, Map<String, Object> variables,
            String recordVariableName, User currentUser) {
        log.debug("Running for user={}, parameter={}", currentUser, parameter);

        Provider provider = currentUser.getProvider();
        Filter filter = filterService.findByCode(filterCode, provider);
        if (filter == null) {
            return;
        }
        try {
            scriptInterface.init(variables, currentUser);

            List<? extends IEntity> xmlEntities = filterService.filteredListAsObjects(filter, currentUser);
            result.setNbItemsToProcess(xmlEntities.size());

            for (Object obj : xmlEntities) {
                Map<String, Object> context = new HashMap<String, Object>();
                context.put(recordVariableName, obj);
                try {
                    scriptInterface.execute(context, currentUser);
                    result.registerSucces();
                } catch (BusinessException ex) {
                    result.registerError(ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error on execute", e);
            result.setReport("error:" + e.getMessage());

        } finally {
            try {
                scriptInterface.finalize(variables, currentUser);

            } catch (Exception e) {
                log.error("Error on finally execute", e);
                result.setReport("finalize error:" + e.getMessage());
            }
        }
    }
}