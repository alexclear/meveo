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
package org.meveo.service.job;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;

import org.meveo.commons.utils.QueryBuilder;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.FtpImportedFile;
import org.meveo.service.base.PersistenceService;

@Stateless
public class FtpImportedFileService extends PersistenceService<FtpImportedFile> {

	public FtpImportedFile findByCode(String code, Provider provider) {
			QueryBuilder qb = new QueryBuilder(FtpImportedFile.class, "t");
			qb.addCriterion("t.code", "=", code, true); 
			qb.addCriterionEntity("provider", provider);
			try {
	            return (FtpImportedFile) qb.getQuery(getEntityManager()).getSingleResult();
	        } catch (NoResultException e) {
	            return null;
	        }
		}
}