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
package org.meveo.admin.action.billing;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.billing.RatedTransaction;
import org.meveo.model.billing.WalletOperation;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.RatedTransactionService;
import org.meveo.service.billing.impl.WalletOperationService;
import org.omnifaces.cdi.ViewScoped;

/**
 * Standard backing bean for {@link RatedTransaction} (extends {@link BaseBean}
 * that provides almost all common methods to handle entities filtering/sorting
 * in datatable, their create, edit, view, delete operations). It works with
 * Manaty custom JSF components.
 */
@Named
@ViewScoped
public class RatedTransactionBean extends BaseBean<RatedTransaction> {

	private static final long serialVersionUID = 1L;

	/**
	 * Injected @{link RatedTransaction} service. Extends
	 * {@link PersistenceService}.
	 */
	@Inject
	private RatedTransactionService ratedTransactionService;
	
	@Inject
	private WalletOperationService walletOperationService;

	/**
	 * Constructor. Invokes super constructor and provides class type of this
	 * bean for {@link BaseBean}.
	 */
	public RatedTransactionBean() {
		super(RatedTransaction.class);
	}

	/**
	 * Factory method for entity to edit. If objectId param set load that entity
	 * from database, otherwise create new.
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@Produces
	@Named("ratedTransaction")
	public RatedTransaction init() {
		return initEntity();
	}

	/**
	 * @see org.meveo.admin.action.BaseBean#getPersistenceService()
	 */
	@Override
	protected IPersistenceService<RatedTransaction> getPersistenceService() {
		return ratedTransactionService;
	}

	public String getWalletOperationCode(Long walletOperationId){
		try{
			WalletOperation walletOperation = walletOperationService.findById(walletOperationId);
		    return walletOperation!=null?walletOperation.getCode():null;
		}catch(Exception e){
			log.error("failed to get wallet operation",e);
			return null;
		}
	}
}
