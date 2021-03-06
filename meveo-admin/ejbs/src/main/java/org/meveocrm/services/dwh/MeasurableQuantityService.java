package org.meveocrm.services.dwh;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.meveo.commons.utils.QueryBuilder;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.crm.Provider;
import org.meveo.service.base.BusinessService;
import org.meveocrm.model.dwh.MeasurableQuantity;

@Stateless
public class MeasurableQuantityService extends
		BusinessService<MeasurableQuantity> {

	public Object[] executeMeasurableQuantitySQL(MeasurableQuantity mq) {
		try{
			Query q = getEntityManager().createNativeQuery(mq.getSqlQuery());
			return (Object[]) q.getSingleResult();
		}catch(Exception e){
			log.error("failed run {} - {}",mq.getSqlQuery(),e.getMessage());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<MeasurableQuantity> listToBeExecuted(Date date,Provider provider) {
		QueryBuilder queryBuilder = new QueryBuilder(MeasurableQuantity.class,
				"a", null, provider);
		queryBuilder.addCriterionDateRangeToTruncatedToDay("last_measure_date",date);
		Query query = queryBuilder.getQuery(getEntityManager());
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<MeasurableQuantity> listEditable() {
		QueryBuilder queryBuilder = new QueryBuilder(MeasurableQuantity.class,
				"a", null, getCurrentProvider());
		queryBuilder.addBooleanCriterion("editable", true);
		Query query = queryBuilder.getQuery(getEntityManager());
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<MeasurableQuantity> list(EntityManager em) {
		QueryBuilder queryBuilder = new QueryBuilder(entityClass, "a", null,
				getCurrentProvider());
		Query query = queryBuilder.getQuery(em);
		return query.getResultList();
	}

    public List<MeasurableQuantity> listByCode(String code) {
        return listByCode(code, getCurrentProvider());
    }
    
	@SuppressWarnings("unchecked")
    public List<MeasurableQuantity> listByCode(String code, Provider provider) {
		QueryBuilder queryBuilder = new QueryBuilder(MeasurableQuantity.class,
				"a", null, provider);
		queryBuilder.addCriterion("code", "=", code, false);
		Query query = queryBuilder.getQuery(getEntityManager());
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<MeasurableQuantity> listByCodeAndDim(
			String measurableQuantityCode, String dimension1Filter,
			String dimension2Filter, String dimension3Filter,
			String dimension4Filter) {

		QueryBuilder queryBuilder = new QueryBuilder(MeasurableQuantity.class,
				"a", null, getCurrentProvider());
		queryBuilder.addCriterion("code", "=", measurableQuantityCode, false);
		if (!StringUtils.isBlank(dimension1Filter)) {
			queryBuilder.addCriterion("dimension1", "=", dimension1Filter,
					false);
		}
		if (!StringUtils.isBlank(dimension2Filter)) {
			queryBuilder.addCriterion("dimension2", "=", dimension2Filter,
					false);
		}
		if (!StringUtils.isBlank(dimension3Filter)) {
			queryBuilder.addCriterion("dimension3", "=", dimension3Filter,
					false);
		}
		if (!StringUtils.isBlank(dimension4Filter)) {
			queryBuilder.addCriterion("dimension4", "=", dimension4Filter,
					false);
		}
		Query query = queryBuilder.getQuery(getEntityManager());
		return query.getResultList();
	}
}
