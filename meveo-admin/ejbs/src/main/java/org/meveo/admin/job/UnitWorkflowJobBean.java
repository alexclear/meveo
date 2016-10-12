package org.meveo.admin.job;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.BusinessEntity;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.wf.Workflow;
import org.meveo.service.wf.WorkflowService;
import org.slf4j.Logger;

/**
 * 
 * @author anasseh
 */

@Stateless
public class UnitWorkflowJobBean{

    @Inject
    private Logger log;
    
    @Inject
    private WorkflowService workflowService;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void execute(JobExecutionResultImpl result, User currentUser, BusinessEntity entity,Workflow workflow) {    	
    	try {    		
    		workflowService.executeWorkflow(entity, workflow, currentUser);
    		result.registerSucces();
        } catch (Exception e) {
            log.error("Failed to unit workflow for {}", entity, e);           
            result.registerError(entity.getClass().getName()+entity.getId(), e.getMessage());
        }
    }	
}