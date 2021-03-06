package org.meveo.api.job;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.MeveoApiErrorCodeEnum;
import org.meveo.api.dto.job.JobInstanceDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.service.crm.impl.CustomFieldTemplateService;
import org.meveo.service.job.Job;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.job.TimerEntityService;

@Stateless
public class JobInstanceApi extends BaseApi {

	@Inject
	private JobInstanceService jobInstanceService;
	
	@Inject
	private TimerEntityService timerEntityService;

	@Inject
	private CustomFieldTemplateService customFieldTemplateService;
    
	public void create(JobInstanceDto postData, User currentUser) throws MeveoApiException, BusinessException {
		if (StringUtils.isBlank(postData.getJobTemplate()) || StringUtils.isBlank(postData.getCode())) {

			if ( StringUtils.isBlank(postData.getJobTemplate())) {
				missingParameters.add("JobTemplate");
			}
			if (StringUtils.isBlank(postData.getCode())) {
				missingParameters.add("Code");
			}			
			handleMissingParameters();
		}

		Provider provider = currentUser.getProvider();

		if (jobInstanceService.findByCode(postData.getCode(), provider) != null) {
			throw new EntityAlreadyExistsException(JobInstance.class, postData.getCode());
		}
				
        Job job = jobInstanceService.getJobByName(postData.getJobTemplate());
        JobCategoryEnum jobCategory = job.getJobCategory();
        
        JobInstance jobInstance = new JobInstance();
        jobInstance.setActive(postData.isActive());
        jobInstance.setParametres(postData.getParameter());  
        jobInstance.setJobCategoryEnum(jobCategory);
        jobInstance.setJobTemplate(postData.getJobTemplate());
        jobInstance.setCode(postData.getCode());
        jobInstance.setDescription(postData.getDescription());
        
         if (!StringUtils.isBlank(postData.getTimerCode())) {
             TimerEntity timerEntity = timerEntityService.findByCode(postData.getTimerCode(),provider); 
             jobInstance.setTimerEntity(timerEntity);
              if(timerEntity==null ){
             throw new MeveoApiException(MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION, "Invalid timer entity=" + postData.getTimerCode());
             }}
        
         if (!StringUtils.isBlank(postData.getFollowingJob())) {
            JobInstance nextJob = jobInstanceService.findByCode(postData.getFollowingJob(),provider);
            jobInstance.setFollowingJob(nextJob);
            if(nextJob==null ){
              throw new MeveoApiException(MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION, "Invalid next job=" + postData.getFollowingJob());
            }
        }         

        // Create any missing CFT for a given provider and job
		Map<String, CustomFieldTemplate> jobCustomFields = job.getCustomFields();
		if (jobCustomFields != null) {
			customFieldTemplateService.createMissingTemplates(jobInstance, jobCustomFields.values(), currentUser);
		}
		
        try {
			jobInstanceService.create(jobInstance, currentUser);
		} catch (BusinessException e1) {
			throw new MeveoApiException(e1.getMessage());
		}
        
		// Populate customFields
        try {
            populateCustomFields(postData.getCustomFields(), jobInstance, true, currentUser);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("Failed to associate custom field instance to an entity", e);
            throw new MeveoApiException("Failed to associate custom field instance to an entity");
        }

	}
	
	/**
	 * Updates JobInstance based on Code
	 * @param jobInstanceDto
	 * @param currentUser
	 * @throws MeveoApiException
	 * @throws BusinessException 
	 */
	public void update(JobInstanceDto postData, User currentUser) throws MeveoApiException, BusinessException {
		
		String jobInstanceCode = postData.getCode(); 
		Provider provider = currentUser.getProvider();
		
		if (StringUtils.isBlank(jobInstanceCode)) {
			missingParameters.add("Code");
			handleMissingParameters();
		} else {
			
			JobInstance jobInstance = jobInstanceService.findByCode(jobInstanceCode, provider); 
			
			if (jobInstance == null ) {
				throw new EntityDoesNotExistsException(JobInstance.class, jobInstanceCode);
            }
			
	        Job job = jobInstanceService.getJobByName(postData.getJobTemplate());
	        JobCategoryEnum jobCategory = job.getJobCategory();

            jobInstance.setJobTemplate(postData.getJobTemplate());
            jobInstance.setParametres(postData.getParameter()); // TODO setParametres should be renamed
            jobInstance.setActive(postData.isActive());
            jobInstance.setJobCategoryEnum(jobCategory);
            jobInstance.setDescription(postData.getDescription());

            if (!StringUtils.isBlank(postData.getTimerCode())) {
                TimerEntity timerEntity = timerEntityService.findByCode(postData.getTimerCode(), provider);
                jobInstance.setTimerEntity(timerEntity);
                if (timerEntity == null) {
                    throw new MeveoApiException(MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION, "Invalid timer entity=" + postData.getTimerCode());
                }
            }

            if (!StringUtils.isBlank(postData.getFollowingJob())) {
                JobInstance nextJob = jobInstanceService.findByCode(postData.getFollowingJob(), provider);
                jobInstance.setFollowingJob(nextJob);
                if (nextJob == null) {
                    throw new MeveoApiException(MeveoApiErrorCodeEnum.BUSINESS_API_EXCEPTION, "Invalid next job=" + postData.getFollowingJob());
                }
            }
			
            // Create any missing CFT for a given provider and job
			Map<String, CustomFieldTemplate> jobCustomFields = job.getCustomFields();
			if (jobCustomFields != null) {
				customFieldTemplateService.createMissingTemplates(jobInstance, jobCustomFields.values(), currentUser);
			}

			jobInstance = jobInstanceService.update(jobInstance, currentUser);
            
            // Populate customFields
            try {
                populateCustomFields(postData.getCustomFields(), jobInstance, false, currentUser);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Failed to associate custom field instance to an entity", e);
                throw new MeveoApiException("Failed to associate custom field instance to an entity");
            }
			
		}
	}
	
	/**
	 * Create or update Job Instance based on code.
	 * @param jobInstanceDto
	 * @param currentUser
	 * @throws MeveoApiException
	 * @throws BusinessException 
	 */
	public void createOrUpdate(JobInstanceDto jobInstanceDto, User currentUser) throws MeveoApiException, BusinessException {
		if (jobInstanceService.findByCode(jobInstanceDto.getCode(), currentUser.getProvider()) == null) {
			create(jobInstanceDto, currentUser);
		} else {
			update(jobInstanceDto, currentUser);
		}
	}
	
	    /**
     * Retrieves a Job Instance base on the code if it is existing.
     * 
     * @param code
     * @param provider
     * @return
     * @throws MeveoApiException
     */
    public JobInstanceDto find(String code, Provider provider) throws MeveoApiException {

        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
            handleMissingParameters();
        }
        
        JobInstance jobInstance = jobInstanceService.findByCode(code, provider);
        if (jobInstance == null) {
            throw new EntityDoesNotExistsException(JobInstance.class, code);
        }
        
        JobInstanceDto jobInstanceDto = new JobInstanceDto(jobInstance, entityToDtoConverter.getCustomFieldsDTO(jobInstance));
        return jobInstanceDto;

    }

    /**
     * 
     * Removes a Job Instance base on a code.
     * 
     * @param code
     * @param provider
     * @throws MeveoApiException
     */
    public void remove(String code, Provider provider) throws MeveoApiException {
        if (StringUtils.isBlank(code)) {
            missingParameters.add("code");
            handleMissingParameters();
        }
        JobInstance jobInstance = jobInstanceService.findByCode(code, provider);
        if (jobInstance == null) {
            throw new EntityDoesNotExistsException(JobInstance.class, code);
        }
        jobInstanceService.remove(jobInstance);
    }
}