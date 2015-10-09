package org.meveo.admin.job.importexport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.meveo.admin.async.ImportCustomersAsync;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.ResourceBundle;
import org.meveo.model.admin.User;
import org.meveo.model.crm.AccountLevelEnum;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.CustomFieldTypeEnum;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.job.Job;

@Startup
@Singleton
public class ImportCustomersJob extends Job {

    @Inject
    private ImportCustomersAsync importCustomersAsync;

    @Inject
    private ResourceBundle resourceMessages;

    @Override
    protected void execute(JobExecutionResultImpl result, JobInstance jobInstance, User currentUser) throws BusinessException {
        try {
            Long nbRuns = new Long(1);
            Long waitingMillis = new Long(0);
            try {
                nbRuns = (Long) jobInstance.getCFValue("ImportCustomersJob_nbRuns");
                waitingMillis = (Long) jobInstance.getCFValue("ImportCustomersJob_waitingMillis");
                if (nbRuns == -1) {
                    nbRuns = (long) Runtime.getRuntime().availableProcessors();
                }
            } catch (Exception e) {
                log.warn("Cant get customFields for " + jobInstance.getJobTemplate());
            }
            List<Future<String>> futures = new ArrayList<Future<String>>();
            for (int i = 0; i < nbRuns.intValue(); i++) {
                futures.add(importCustomersAsync.launchAndForget(result, currentUser));
                if (i > 0) {
                    try {
                        Thread.sleep(waitingMillis.longValue());
                    } catch (InterruptedException e) {
                        log.error("", e);
                    }
                }
            }
            // Wait for all async methods to finish
            for (Future<String> future : futures) {
                try {
                    future.get();

                } catch (InterruptedException e) {
                    // It was cancelled from outside - no interest

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    result.registerError(cause.getMessage());
                    log.error("Failed to execute async method", cause);
                }
            }

        } catch (Exception e) {
            log.error("Failed to import customers", e);
            result.registerError(e.getMessage());
        }
    }

    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.IMPORT_HIERARCHY;
    }

    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

        CustomFieldTemplate customFieldNbRuns = new CustomFieldTemplate();
        customFieldNbRuns.setCode("ImportCustomersJob_nbRuns");
        customFieldNbRuns.setAccountLevel(AccountLevelEnum.TIMER);
        customFieldNbRuns.setActive(true);
        customFieldNbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
        customFieldNbRuns.setFieldType(CustomFieldTypeEnum.LONG);
        customFieldNbRuns.setDefaultValue("1");
        customFieldNbRuns.setValueRequired(false);
        result.put("ImportCustomersJob_nbRuns", customFieldNbRuns);

        CustomFieldTemplate customFieldNbWaiting = new CustomFieldTemplate();
        customFieldNbWaiting.setCode("ImportCustomersJob_waitingMillis");
        customFieldNbWaiting.setAccountLevel(AccountLevelEnum.TIMER);
        customFieldNbWaiting.setActive(true);
        customFieldNbWaiting.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
        customFieldNbWaiting.setFieldType(CustomFieldTypeEnum.LONG);
        customFieldNbWaiting.setDefaultValue("500");
        customFieldNbWaiting.setValueRequired(false);
        result.put("ImportCustomersJob_waitingMillis", customFieldNbWaiting);

        return result;
    }
}