/*
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.services.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.jboss.seam.security.Identity;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResult;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.crm.impl.ProviderService;

@Stateless
public class TimerEntityService extends PersistenceService<TimerEntity> {

	public static HashMap<String, Job> jobEntries = new HashMap<String, Job>();
	public static HashMap<Long,Timer> jobTimers = new HashMap<Long, Timer>();
	
	@Inject
	Identity identity;
	
	@Inject
	ProviderService providerService;
	
	@Resource
	TimerService timerService;
	
	/*static boolean timersCleaned = false;*/
	
	static ParamBean paramBean = ParamBean.getInstance();

	static Long defaultProviderId = Long.parseLong(paramBean.getProperty("jobs.autoStart.providerId", "1"));

	static boolean allTimerCleanded=false;
	
	/**
	 * Used by job instance classes to register themselves to the timer service
	 * 
	 * @param name
	 *            unique name in the application, used by the admin to manage
	 *            timers
	 * @param description
	 *            describe the task realized by the job
	 * @param JNDIName
	 *            used to instanciate the implementation to execute the job
	 *            (instantiacion class must be a session EJB)
	 */
	public static void registerJob(Job job) {
		if (!jobEntries.containsKey(job.getClass().getSimpleName())) {
			jobEntries.put(job.getClass().getSimpleName(), job);
		}
		job.getJobExecutionService().getTimerEntityService().startTimers(job);
	}
	
	public Collection<Timer> getTimers(){
		return timerService.getTimers();
	}
	
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void startTimers(Job job){
		//job.cleanAllTimers();
		@SuppressWarnings("unchecked")
		List<TimerEntity> timerEntities =getEntityManager().createQuery("from TimerEntity t where t.jobName=:jobName")
		.setParameter("jobName", job.getClass().getSimpleName()).getResultList();
		if(timerEntities!=null){
			log.debug("staring " + timerEntities.size()+" timers for "+job.getClass().getSimpleName());
			for(TimerEntity timerEntity :timerEntities){
				jobTimers.put(timerEntity.getId(), job.createTimer(timerEntity.getScheduleExpression(),
						timerEntity.getTimerInfo()));
			}
		}
	}


	public void create(TimerEntity entity) {// FIXME: throws BusinessException{
		if (jobEntries.containsKey(entity.getJobName())) {
			Job job = jobEntries.get(entity.getJobName());
			entity.getTimerInfo().setJobName(entity.getJobName());
			entity.getTimerInfo().setProviderId(getCurrentProvider()==null?defaultProviderId:getCurrentProvider().getId());
			if(entity.getFollowingTimer()!=null){
				entity.getTimerInfo().setFollowingTimerId(entity.getFollowingTimer().getId());
			}
			super.create(entity);
			jobTimers.put(entity.getId(), job.createTimer(entity.getScheduleExpression(),entity.getTimerInfo()));
		}
	}

	public void update(TimerEntity entity) {// FIXME: throws BusinessException{
		log.info("update " + entity.getJobName());
		if (jobEntries.containsKey(entity.getJobName())) {
			Job job = jobEntries.get(entity.getJobName());
			Timer timer = jobTimers.get(entity.getId());
			log.info("cancelling existing " + timer.getTimeRemaining() / 1000
					+ " sec");
			timer.cancel();
			if(entity.getFollowingTimer()!=null){
				entity.getTimerInfo().setFollowingTimerId(entity.getFollowingTimer().getId());
			}
			jobTimers.put(entity.getId(),job.createTimer(entity.getScheduleExpression(), entity.getTimerInfo()));
			super.update(entity);
		}
	}

	public void remove(TimerEntity entity) {// FIXME: throws BusinessException{
		Timer timer = jobTimers.get(entity.getId());
		timer.cancel();
		jobTimers.remove(entity.getId());
		super.remove(entity);
	}

	public void execute(TimerEntity entity) throws BusinessException {
		log.info("execute " + entity.getJobName());
		if (entity.getTimerInfo().isActive() && jobEntries.containsKey(entity.getJobName())) {
			Job job = jobEntries.get(entity.getJobName());
			Provider provider = providerService.findById(entity.getTimerInfo().getProviderId());
			job.execute(entity.getTimerInfo() != null ? entity.getTimerInfo().getParametres() : null,
					provider);
		}
	}

	public JobExecutionResult manualExecute(TimerEntity entity) throws BusinessException {
		JobExecutionResult result = null;
		log.info("manual execute " + entity.getJobName());
		if(entity.getTimerInfo() != null && entity.getTimerInfo().getProviderId()!=getCurrentProvider().getId()){
			throw new BusinessException("not authorized to execute this job");
		}
		if (jobEntries.containsKey(entity.getJobName())) {
			Job job = jobEntries.get(entity.getJobName());
			result = job.execute(
					entity.getTimerInfo() != null ? entity.getTimerInfo().getParametres() : null,
							getCurrentProvider());
		}
		return result;
	}

	
	public TimerEntity getByTimer(Timer timer){
		Set<Map.Entry<Long,Timer>> entrySet = jobTimers.entrySet();
		for(Map.Entry<Long, Timer> entry : entrySet){
			if(entry.getValue()==timer){
				return findById(entry.getKey());
			}
		}
		return null;
	}
	
	private QueryBuilder getFindQuery(PaginationConfiguration configuration) {
		String sql = "select distinct t from TimerEntity t";
		QueryBuilder qb = new QueryBuilder(sql);// FIXME: .cacheable(); there is
												// no cacheable in MEVEO
												// QueryBuilder
		qb.addCriterionEntity("provider", getCurrentProvider());
		qb.addPaginationConfiguration(configuration);
		return qb;
	}

	@SuppressWarnings("unchecked")
	public List<TimerEntity> find(PaginationConfiguration configuration) {
		return getFindQuery(configuration).find(getEntityManager());
	}

	public long count(PaginationConfiguration configuration) {
		return getFindQuery(configuration).count(getEntityManager());
	}

}
