package org.meveo.admin.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.async.SubListCreator;
import org.meveo.admin.async.UsageRatingAsync;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.event.qualifier.Rejected;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.billing.impl.EdrService;
import org.meveo.service.crm.impl.CustomFieldInstanceService;
import org.slf4j.Logger;

@Stateless
public class UsageRatingJobBean {

	@Inject
	private Logger log;

	@Inject
	private EdrService edrService;

	@Inject
	private UsageRatingAsync usageRatingAsync;

	@Inject
	@Rejected
	Event<Serializable> rejectededEdrProducer;
	
    @Inject
    protected CustomFieldInstanceService customFieldInstanceService;

	@SuppressWarnings("unchecked")
    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void execute(JobExecutionResultImpl result, User currentUser, JobInstance jobInstance) {
		log.debug("Running for user={}, parameter={}", currentUser, jobInstance.getParametres());
		
		try {
			
			List<Long> ids = edrService.getEDRidsToRate(currentUser.getProvider());		
			log.debug("edr to rate:" + ids.size());
			result.setNbItemsToProcess(ids.size());
			Long nbRuns = new Long(1);		
			Long waitingMillis = new Long(0);
			try{
				nbRuns = (Long) customFieldInstanceService.getCFValue(jobInstance, "nbRuns", currentUser);  			
				waitingMillis = (Long) customFieldInstanceService.getCFValue(jobInstance, "waitingMillis", currentUser);
				if(nbRuns == -1){
					nbRuns  = (long) Runtime.getRuntime().availableProcessors();
				}
			}catch(Exception e){
				nbRuns = new Long(1);
				waitingMillis = new Long(0);
				log.warn("Cant get customFields for "+jobInstance.getJobTemplate(),e.getMessage());
			}

			List<Future<String>> futures = new ArrayList<Future<String>>();
	    	SubListCreator subListCreator = new SubListCreator(ids,nbRuns.intValue());
	    	log.debug("block to run:" + subListCreator.getBlocToRun());
	    	log.debug("nbThreads:" + nbRuns);
			while (subListCreator.isHasNext()) {	
				futures.add(usageRatingAsync.launchAndForget((List<Long>) subListCreator.getNextWorkSet(),result, currentUser));

                if (subListCreator.isHasNext()) {
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
                    result.addReport(cause.getMessage());
                    log.error("Failed to execute async method", cause);
                }
            }
        } catch (Exception e) {
            log.error("Failed to run usage rating job",e);
            result.registerError(e.getMessage());
            result.addReport(e.getMessage());
        }
	}


}
