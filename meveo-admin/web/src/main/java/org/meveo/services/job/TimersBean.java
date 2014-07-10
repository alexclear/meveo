package org.meveo.services.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.Timer;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.admin.util.pagination.PaginationConfiguration;
import org.meveo.admin.util.pagination.PaginationDataModel;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.model.jobs.TimerInfo;
import org.meveo.service.base.local.IPersistenceService;
import org.omnifaces.util.Messages;

@ConversationScoped
@Named
public class TimersBean extends BaseBean<TimerEntity> {

	private static final long serialVersionUID = 5578930292531038376L;

	@Inject
	TimerEntityService timerEntityservice;

	private int pageSize = 20;
	private PaginationDataModel<TimerEntity> timersDataModel;


	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/*
	 * to be used in picklist to select a job
	 */
	public Set<String> getJobNames() {
		return TimerEntityService.jobEntries.keySet();
	}

	@Produces
	@RequestScoped
	@Named("timersDataModel")
	public PaginationDataModel<TimerEntity> find() {
		if (timersDataModel == null) {
			timersDataModel = new TimersDataModel();
		}

		timersDataModel.forceRefresh();

		return timersDataModel;
	}

	public List<Timer> getEjbTimers() {// FIXME: throws BusinessException {

		return timerEntityservice.getEjbTimers();
	}

	public TimerInfo getTimerInfo(Timer timer) {
		return (TimerInfo) timer.getInfo();
	}

	public String getTimerSchedule(Timer timer) {
		String result = "";
		try {
			result = timer.getSchedule().toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void cancelEjbTimer(Timer timer) {

		try {
			TimerEntity timerEntity = timerEntityservice.findByTimerHandle(timer.getHandle());

			if (timerEntity != null) {
				timerEntityservice.remove(timerEntity);
			} else {
				timer.cancel();
			}
			Messages.createInfo( "info.ejbTimer.cancelled");
		} catch (Exception e) {
			e.printStackTrace();
			Messages.createError( "error.ejbTimer.cancellation");
		}
	}

	public void cancelEjbTimers() {

		try {
			for (Timer timer : timerEntityservice.getEjbTimers()) {
				TimerEntity timerEntity = timerEntityservice.findByTimerHandle(timer.getHandle());

				if (timerEntity != null) {
					timerEntityservice.remove(timerEntity);
				} else {
					timer.cancel();
				}
			}

			Messages.createInfo( "info.ejbTimers.cancelled");
		} catch (Exception e) {
			Messages.createError( "error.ejbTimers.cancellation");
		}
	}

	/***********************************************************************************/
	/* DATATABLE MODEL */
	/***********************************************************************************/
	class TimersDataModel extends PaginationDataModel<TimerEntity> {

		private static final long serialVersionUID = 1L;

		@Override
		protected int countRecords(PaginationConfiguration paginatingData) {
			int userCount = (int) timerEntityservice.count(paginatingData);
			return userCount;
		}

		@Override
		protected List<TimerEntity> loadData(PaginationConfiguration configuration) {
			return timerEntityservice.find(configuration);
		}
	}

	class JBossTimersDataModel extends PaginationDataModel<Timer> {

		private static final long serialVersionUID = 1L;

		@Override
		protected int countRecords(PaginationConfiguration paginatingData) {
			try {
				return timerEntityservice.getEjbTimers().size();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		protected List<Timer> loadData(PaginationConfiguration configuration) {
			try {
				return new ArrayList<Timer>(timerEntityservice.getEjbTimers());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}

	@Override
	protected IPersistenceService<TimerEntity> getPersistenceService() {
		// TODO Auto-generated method stub
		return timerEntityservice;
	}

}
