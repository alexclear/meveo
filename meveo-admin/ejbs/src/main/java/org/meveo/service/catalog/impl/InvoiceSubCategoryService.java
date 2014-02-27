/*
 * (C) Copyright 2009-2013 Manaty SARL (http://manaty.net/) and contributors.
 *
 * Licensed under the GNU Public Licence, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meveo.service.catalog.impl;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.billing.InvoiceSubCategory;
import org.meveo.service.base.PersistenceService;

/**
 * InvoiceSubCategory service implementation.
 * 
 * @author Ignas Lelys
 * @created Dec 15, 2010
 * 
 */
@Stateless
@LocalBean
public class InvoiceSubCategoryService extends
		PersistenceService<InvoiceSubCategory> {

	public InvoiceSubCategory findByCode(EntityManager em, String code) {
		QueryBuilder qb = new QueryBuilder(InvoiceSubCategory.class, "sc");
		qb.addCriterion("code", "=", code, false);

		try {
			return (InvoiceSubCategory) qb.getQuery(em).getSingleResult();
		} catch (NoResultException e) {
			log.warn(e.getMessage());
			return null;
		}
	}

	public InvoiceSubCategory findByCode(String code) {
		QueryBuilder qb = new QueryBuilder(InvoiceSubCategory.class, "sc");
		qb.addCriterion("code", "=", code, false);

		return (InvoiceSubCategory) qb.getQuery(getEntityManager())
				.getSingleResult();
	}

}
