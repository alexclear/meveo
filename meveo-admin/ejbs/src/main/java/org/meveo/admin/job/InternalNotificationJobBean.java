package org.meveo.admin.job;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.job.logging.JobLoggingInterceptor;
import org.meveo.commons.utils.StringUtils;
import org.meveo.interceptor.PerformanceInterceptor;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.JobExecutionResultImpl;
import org.meveo.model.notification.Notification;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.notification.NotificationService;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.ScriptInterface;
import org.slf4j.Logger;

@Stateless
public class InternalNotificationJobBean {

	@Inject
	protected Logger log;

	// iso 8601 date and datetime format
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat tf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:hh");

	@PersistenceContext(unitName = "MeveoAdmin")
	private EntityManager em;

	@Inject
	private BeanManager manager;

	@Inject
	NotificationService notificationService;

	@Inject
	ScriptInstanceService scriptInstanceService;

	@Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void execute(String filterCode, String notificationCode, JobExecutionResultImpl result, User currentUser) {
		log.debug("Running for user={}, filterCode={}", currentUser, filterCode);
		Provider provider = currentUser.getProvider();
		if (StringUtils.isBlank(filterCode)) {
			result.registerError("filterCode has no SQL query set.");
			return;
		} else if (filterCode.indexOf("#{provider}") < 0) {
			result.registerError("filterCode must filter result by provider using the #{provider} variable.");
			return;
		}
		Notification notification = notificationService.findByCode(em, notificationCode, provider);
		if (notification == null) {
			result.registerError("no notification found for " + notificationCode);
			return;
		}
		try {

			String queryStr = filterCode.replaceAll("#\\{date\\}", df.format(new Date()));
			queryStr = queryStr.replaceAll("#\\{dateTime\\}", tf.format(new Date()));
			queryStr = queryStr.replaceAll("#\\{provider\\}", "" + provider.getId());
			log.debug("execute query:{}", queryStr);
			Query query = em.createNativeQuery(queryStr);
			@SuppressWarnings("unchecked")
			List<Object> results = query.getResultList();
			result.setNbItemsToProcess(results.size());
			for (Object res : results) {
				Map<Object, Object> userMap = new HashMap<Object, Object>();
				userMap.put("event", res);
				userMap.put("manager", manager);
				if (!StringUtils.isBlank(notification.getElFilter())) {
					Object o = ValueExpressionWrapper.evaluateExpression(notification.getElFilter(), userMap, Boolean.class);
					try {
						if (!(Boolean) o) {
							result.registerSucces();
							continue;
						}
					} catch (Exception e) {
						throw new BusinessException("Expression " + notification.getElFilter() + " do not evaluate to boolean but " + res);
					}
				}
				try {
					if (notification.getScriptInstance() != null) {
						Class<ScriptInterface> scriptInterfaceClass = scriptInstanceService.getScriptInterface(provider, notification.getScriptInstance().getCode());
						ScriptInterface scriptInterface = scriptInterfaceClass.newInstance();
						Map<String, Object> paramsEvaluated = new HashMap<String, Object>();
						for (@SuppressWarnings("rawtypes")
						Map.Entry entry : notification.getParams().entrySet()) {
							paramsEvaluated.put((String) entry.getKey(), ValueExpressionWrapper.evaluateExpression((String) entry.getValue(), userMap, String.class));
						}
						scriptInterface.execute(paramsEvaluated, provider);
						result.registerSucces();
					} else {
						log.debug("No script instance on this Notification");
					}
				} catch (Exception e) {
					result.registerError("Error execution " + notification.getScriptInstance() + " on " + res);
					throw new BusinessException("Expression " + notification.getElFilter() + " do not evaluate to boolean but " + res);
				}
			}

		} catch (Exception e) {
			result.registerError("filterCode contain invalid SQL query: " + e.getMessage());
		}
	}

}
