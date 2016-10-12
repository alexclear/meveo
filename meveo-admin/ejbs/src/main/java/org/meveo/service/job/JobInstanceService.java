/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.service.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.JobDoesNotExistsException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.api.dto.job.JobInstanceInfoDto;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobCategoryEnum;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.jobs.JobInstance;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.base.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class JobInstanceService extends PersistenceService<JobInstance> {
    public static Map<JobCategoryEnum, HashMap<String, String>> jobEntries = new HashMap<JobCategoryEnum, HashMap<String, String>>();
    public static Map<Long, Timer> jobTimers = new HashMap<Long, Timer>();
    public static List<Long> runningJobs = new ArrayList<Long>();

    @Resource
    private TimerService timerService;

    @Inject
    private UserService userService;

    @EJB
    private JobExecutionService jobExecutionService;

    private static Logger log = LoggerFactory.getLogger(JobInstanceService.class);

    /* static boolean timersCleaned = false; */

    static ParamBean paramBean = ParamBean.getInstance();

    static Long defaultProviderId = Long.parseLong(paramBean.getProperty("jobs.autoStart.providerId", "1"));

    static boolean allTimerCleanded = false;

    /**
     * Used by job instance classes to register themselves to the timer service
     * 
     * @param name unique name in the application, used by the admin to manage timers
     * @param description describe the task realized by the job
     * @param JNDIName used to instanciate the implementation to execute the job (instantiacion class must be a session EJB)
     */

    public static void registerJob(Job job) {
        synchronized (jobEntries) {
            if (jobEntries.containsKey(job.getJobCategory())) {
                Map<String, String> jobs = jobEntries.get(job.getJobCategory());
                if (!jobs.containsKey(job.getClass().getSimpleName())) {
                    log.debug("registerJob " + job.getClass().getSimpleName() + " into existing category " + job.getJobCategory());
                    jobs.put(job.getClass().getSimpleName(), "java:global/" + paramBean.getProperty("meveo.moduleName", "meveo") + "/" + job.getClass().getSimpleName());
                }
            } else {
                log.debug("registerJob " + job.getClass().getSimpleName() + " into new category " + job.getJobCategory());
                HashMap<String, String> jobs = new HashMap<String, String>();
                jobs.put(job.getClass().getSimpleName(), "java:global/" + paramBean.getProperty("meveo.moduleName", "meveo") + "/" + job.getClass().getSimpleName());
                jobEntries.put(job.getJobCategory(), jobs);
            }
            job.getJobExecutionService().getJobInstanceService().startTimers(job);
        }
    }

    public Collection<Timer> getTimers() {
        return timerService.getTimers();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void startTimers(Job job) {
        // job.cleanAllTimers();
        @SuppressWarnings("unchecked")
        List<JobInstance> jobInstances = getEntityManager().createQuery("from JobInstance ji JOIN FETCH ji.followingJob where ji.jobTemplate=:jobName")
            .setParameter("jobName", job.getClass().getSimpleName()).getResultList();

        if (jobInstances != null) {
            int started = 0;
            for (JobInstance jobInstance : jobInstances) {
                if (jobInstance.isActive() && jobInstance.getTimerEntity() != null) {
                    jobTimers.put(jobInstance.getId(), job.createTimer(jobInstance.getTimerEntity().getScheduleExpression(), jobInstance));
                    started++;
                }
            }
            log.debug("Found {} job instances for {}, started {}", jobInstances.size(), job.getClass().getSimpleName(), started);
        }
    }

    public Job getJobByName(String jobName) {
        Job result = null;
        try {
            InitialContext ic = new InitialContext();
            result = (Job) ic.lookup("java:global/" + paramBean.getProperty("meveo.moduleName", "meveo") + "/" + jobName);
        } catch (NamingException e) {
            log.error("Failed to get job by name {}", jobName, e);
        }
        return result;
    }

    public List<Job> getJobs() {
        List<Job> jobs = new ArrayList<Job>();

        for (HashMap<String, String> jobInfos : jobEntries.values()) {
            for (String url : jobInfos.values()) {
                try {
                    InitialContext ic = new InitialContext();
                    Job job = (Job) ic.lookup(url);
                    jobs.add(job);

                } catch (NamingException e) {
                    log.error("Failed to get job by url {}", url, e);
                }
            }
        }

        return jobs;
    }

    @Override
    public void create(JobInstance jobInstance, User currentUser) throws BusinessException {

        super.create(jobInstance, currentUser);
        scheduleUnscheduleJob(jobInstance);
    }

    @Override
    public JobInstance update(JobInstance jobInstance, User currentUser) throws BusinessException {

        super.update(jobInstance, currentUser);
        scheduleUnscheduleJob(jobInstance);

        return jobInstance;
    }

    @Override
    public void remove(JobInstance entity, User currentUser) throws BusinessException {
        log.info("remove jobInstance {}, id={}", entity.getJobTemplate(), entity.getId());
        if (entity.getId() == null) {
            log.info("removing jobInstance entity with null id, something is wrong");
        } else if (jobTimers.containsKey(entity.getId())) {
            try {
                Timer timer = jobTimers.get(entity.getId());
                timer.cancel();
            } catch (Exception ex) {
                log.info("cannot cancel timer " + ex);
            }
            jobTimers.remove(entity.getId());
        } else {
            log.info("jobInstance timer not found, cannot remove it");
        }
        super.remove(entity, currentUser);
    }

    @Override
    public JobInstance enable(JobInstance jobInstance, User currentUser) throws BusinessException {
        jobInstance = super.enable(jobInstance, currentUser);

        log.info("Enabling jobInstance {}, id={}", jobInstance.getJobTemplate(), jobInstance.getId());
        scheduleUnscheduleJob(jobInstance);

        return jobInstance;
    }

    @Override
    public JobInstance disable(JobInstance jobInstance, User currentUser) throws BusinessException {
        jobInstance = super.disable(jobInstance, currentUser);

        log.info("Disabling jobInstance {}, id={}", jobInstance.getJobTemplate(), jobInstance.getId());
        scheduleUnscheduleJob(jobInstance);

        return jobInstance;
    }

    private void scheduleUnscheduleJob(JobInstance jobInstance) {

        try {

            if (!jobEntries.containsKey(jobInstance.getJobCategoryEnum())) {
                log.error("Not registered job category {} for jobInstance {}", jobInstance.getJobCategoryEnum(), jobInstance.getCode());
                throw new RuntimeException("Not registered job category " + jobInstance.getJobCategoryEnum());
            }
            HashMap<String, String> jobs = jobEntries.get(jobInstance.getJobCategoryEnum());
            if (!jobs.containsKey(jobInstance.getJobTemplate())) {
                log.error("cannot find job {} for jobInstance {}", jobInstance.getJobTemplate(), jobInstance.getCode());
                throw new RuntimeException("cannot find job " + jobInstance.getJobTemplate());
            }

            if (jobTimers.containsKey(jobInstance.getId())) {
                try {
                    Timer timer = jobTimers.get(jobInstance.getId());
                    timer.cancel();
                    jobTimers.remove(jobInstance.getId());
                    log.info("Cancelled timer {}, id={}", jobInstance.getJobTemplate(), jobInstance.getId());
                } catch (Exception ex) {
                    log.error("Failed to cancel timer {}, id={}", jobInstance.getJobTemplate(), jobInstance.getId(), ex);
                }
            }
            if (jobInstance.isActive() && jobInstance.getTimerEntity() != null) {
                InitialContext ic = new InitialContext();
                Job job = (Job) ic.lookup(jobs.get(jobInstance.getJobTemplate()));
                log.info("Scheduling job {} : timer {}", job, jobInstance.getId());
                jobTimers.put(jobInstance.getId(), job.createTimer(jobInstance.getTimerEntity().getScheduleExpression(), jobInstance));
            } else {
                log.debug("Job {} is inactive or has no timer and will not be scheduled", jobInstance.getCode());
            }

        } catch (NamingException e) {
            log.error("Failed to schedule job", e);
        }
    }

    public void execute(JobInstance entity) throws BusinessException {
        log.info("execute {}", entity.getJobTemplate());
        InitialContext ic;
        try {
            ic = new InitialContext();
            if (jobEntries.containsKey(entity.getJobCategoryEnum())) {
                HashMap<String, String> jobs = jobEntries.get(entity.getJobCategoryEnum());
                if (entity.isActive() && jobs.containsKey(entity.getJobTemplate())) {

                    Job job = (Job) ic.lookup(jobs.get(entity.getJobTemplate()));

                    User currentUser = userService.attach(entity.getAuditable().getUpdater() != null ? entity.getAuditable().getUpdater() : entity.getAuditable().getCreator());
                    job.execute(entity, currentUser);
                }
            }
        } catch (NamingException e) {
            log.error("Failed to execute timerEntity job", e);
        }
    }

    public void triggerExecution(String jobInstanceCode, Map<String, String> params, User user) throws BusinessException {
        log.info("triggerExecution jobInstance={} via trigger", jobInstanceCode);
        execute(jobInstanceCode, user, params);

    }

    public void manualExecute(JobInstance entity) throws BusinessException {
        log.info("Manual execute a job {} of type {}", entity.getCode(), entity.getJobTemplate());
        try {
            // Retrieve a timer entity from registered job timers, so if job is launched manually and automatically at the same time, only one will run
            if (jobTimers.containsKey(entity.getId())) {
                entity = (JobInstance) jobTimers.get(entity.getId()).getInfo();
            }

            InitialContext ic = new InitialContext();
            User currentUser = userService.attach(entity.getAuditable().getUpdater() != null ? entity.getAuditable().getUpdater() : entity.getAuditable().getCreator());
            if (!currentUser.doesProviderMatch(getCurrentProvider())) {
                throw new BusinessException("Not authorized to execute this job");
            }

            if (jobEntries.containsKey(entity.getJobCategoryEnum())) {
                HashMap<String, String> jobs = jobEntries.get(entity.getJobCategoryEnum());
                if (jobs.containsKey(entity.getJobTemplate())) {
                    Job job = (Job) ic.lookup(jobs.get(entity.getJobTemplate()));
                    job.execute(entity, getCurrentUser());
                }
            } else {
                throw new BusinessException("Cannot find job category " + entity.getJobCategoryEnum());
            }
        } catch (NamingException e) {
            log.error("failed to manually execute ", e);

        } catch (Exception e) {
            log.error("Failed to manually execute a job {} of type {}", entity.getCode(), entity.getJobTemplate(), e);
            throw e;
        }
    }

    public Long executeAPITimer(JobInstanceInfoDto jobInstanceInfoDTO, User currentUser) throws BusinessException {
        log.info("executeAPITimer jobInstance={} via api", jobInstanceInfoDTO.toString());
        String jobInstanceCode = jobInstanceInfoDTO.getCode();

        if (StringUtils.isBlank(jobInstanceCode)) {
            jobInstanceCode = jobInstanceInfoDTO.getTimerName();
        }
        return execute(jobInstanceCode, currentUser, null);
    }

    public Long execute(String jobInstanceCode, User currentUser, Map<String, String> params) throws BusinessException {
        log.info("execute timer={} ", jobInstanceCode);
        JobInstance entity = null;
        entity = findByCode(jobInstanceCode, currentUser.getProvider());
        if (entity == null) {
            throw new JobDoesNotExistsException(jobInstanceCode);
        }

        JobExecutionResultImpl result = new JobExecutionResultImpl();
        result.setJobInstance(entity);
        InitialContext ic;
        try {
            ic = new InitialContext();
            if (jobEntries.containsKey(entity.getJobCategoryEnum())) {
                HashMap<String, String> jobs = jobEntries.get(entity.getJobCategoryEnum());
                if (jobs.containsKey(entity.getJobTemplate())) {
                    Job job = (Job) ic.lookup(jobs.get(entity.getJobTemplate()));
                    jobExecutionService.create(result, currentUser);
                    job.execute(entity, result, currentUser);
                }
            } else {
                throw new BusinessException("cannot find job category " + entity.getJobCategoryEnum());
            }
        } catch (NamingException e) {
            log.error("failed to execute ", e);
        }

        return result.getId();
    }

    public JobInstance getByTimer(Timer timer) {
        Set<Map.Entry<Long, Timer>> entrySet = jobTimers.entrySet();
        for (Map.Entry<Long, Timer> entry : entrySet) {
            if (entry.getValue() == timer) {
                return findById(entry.getKey());
            }
        }

        return null;
    }

    private QueryBuilder getFindQuery(PaginationConfiguration configuration) {
        String sql = "select distinct t from JobInstance t";
        QueryBuilder qb = new QueryBuilder(sql);// FIXME: .cacheable(); there is
        // no cacheable in MEVEO
        // QueryBuilder
        qb.addCriterionEntity("provider", getCurrentProvider());
        qb.addPaginationConfiguration(configuration);
        return qb;
    }

    @SuppressWarnings("unchecked")
    public List<JobInstance> find(PaginationConfiguration configuration) {
        return getFindQuery(configuration).find(getEntityManager());
    }

    public long count(PaginationConfiguration configuration) {
        return getFindQuery(configuration).count(getEntityManager());
    }

    /**
     * Check if a timer, identifier by a timer id, is running
     * 
     * @param timerEntityId Timer entity id
     * @return True if running
     */
    public boolean isJobRunning(Long timerEntityId) {
        return runningJobs.contains(timerEntityId);
    }

    public JobInstance findByCode(String code, Provider provider) {
        // QueryBuilder qb = new QueryBuilder(JobInstance.class, "t");
        QueryBuilder qb = new QueryBuilder(JobInstance.class, "t", Arrays.asList("timerEntity"), provider);
        qb.addCriterion("t.code", "=", code, true);
        // qb.addCriterionEntity("t.provider", provider);
        try {
            return (JobInstance) qb.getQuery(getEntityManager()).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}