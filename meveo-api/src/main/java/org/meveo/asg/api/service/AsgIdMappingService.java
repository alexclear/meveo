package org.meveo.asg.api.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.asg.api.model.AsgIdMapping;
import org.meveo.asg.api.model.EntityCodeEnum;
import org.meveo.commons.utils.QueryBuilder;
import org.meveo.service.base.PersistenceService;

@Stateless
public class AsgIdMappingService extends PersistenceService<AsgIdMapping> {

	public String getNewCode(String asgId, EntityCodeEnum entityType)
			throws EntityAlreadyExistsException {
		return getNewCode(getEntityManager(), asgId, entityType);
	}

	public String getNewCode(EntityManager em, String asgId,
			EntityCodeEnum entityType) throws EntityAlreadyExistsException {
		@SuppressWarnings("unchecked")
		List<AsgIdMapping> ids = getEntityManager()
				.createQuery(
						"from " + AsgIdMapping.class.getSimpleName()
								+ " where asgId=:asgId")
				.setParameter("asgId", asgId).getResultList();
		if (ids != null && ids.size() > 0) {
			throw new EntityAlreadyExistsException("Entity with ASG code "
					+ asgId + " already exists");
		}

		AsgIdMapping idMapping = new AsgIdMapping();
		idMapping.setEntityType(entityType);
		idMapping.setAsgId(asgId);
		em.persist(idMapping);

		return entityType + "" + idMapping.getMeveoCode();
	}

	public String getMeveoCode(String asgId, EntityCodeEnum entityType)
			throws BusinessException {
		return getMeveoCode(getEntityManager(), asgId, entityType);
	}

	public String getMeveoCode(EntityManager em, String asgId,
			EntityCodeEnum entityType) throws BusinessException {
		@SuppressWarnings("unchecked")
		List<AsgIdMapping> ids = em
				.createQuery(
						"FROM " + AsgIdMapping.class.getSimpleName()
								+ " WHERE asgId=:asgId")
				.setParameter("asgId", asgId).getResultList();
		if (ids == null || ids.size() == 0) {
			throw new BusinessException("Entity with ASG code " + asgId
					+ " does not exist");
		}

		return ids.get(0).getEntityType() + "" + ids.get(0).getMeveoCode();
	}

	public void removeByCodeAndType(EntityManager em, String asgId,
			EntityCodeEnum entityType) {
		QueryBuilder qb = new QueryBuilder(AsgIdMapping.class, "a");
		qb.addCriterion("asgId", "=", asgId, false);
		qb.addCriterionEnum("entityType", entityType);

		try {
			AsgIdMapping obj = (AsgIdMapping) qb.getQuery(em).getSingleResult();
			if (obj != null) {
				remove(em, obj);
			}
		} catch (NoResultException e) {
			log.warn(e.getMessage());
		}
	}
}
