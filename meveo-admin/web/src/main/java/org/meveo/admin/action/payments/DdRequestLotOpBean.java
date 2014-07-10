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
package org.meveo.admin.action.payments;


import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.payments.ActionPlanItem;
import org.meveo.model.payments.DDRequestLotOp;
import org.meveo.model.payments.DDRequestOpEnum;
import org.meveo.model.payments.DDRequestOpStatusEnum;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.payments.impl.DDRequestLotOpService;
import org.omnifaces.util.Messages;

/**
 * Standard backing bean for {@link ActionPlanItem} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 * 
 * @author Tyshan(tyshan@manaty.net)
 */
@Named
@ConversationScoped
public class DdRequestLotOpBean extends BaseBean<DDRequestLotOp> {

    private static final long serialVersionUID = 1L;

    @Inject
    private DDRequestLotOpService ddRequestLotOpService; 
    
    

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public DdRequestLotOpBean() {
        super(DDRequestLotOp.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
 
    
	public DDRequestLotOp initEntity() {
		return super.initEntity();
	}
    
   
    @Override
    public String saveOrUpdate(DDRequestLotOp entity) {
		if (entity.isTransient()) {
			entity.setDdrequestOp(DDRequestOpEnum.CREATE); 
	    	 entity.setStatus(DDRequestOpStatusEnum.WAIT);
			getPersistenceService().create(entity);
			Messages.createInfo( "save.successful");
		} else {
			getPersistenceService().update(entity);
			Messages.createInfo( "update.successful");
		}

		return back();
	}
    
    
    
	@Override
	public String getNewViewName() {
		return "ddrequestLotOpDetail";
	}
	
	@Override
	protected String getListViewName() {
		return "ddrequestLotOps";
	}

 

	@Override
	public String getEditViewName() {
		return "ddrequestLotOpDetail";
	}
    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<DDRequestLotOp> getPersistenceService() {
        return ddRequestLotOpService;
    }
 
}
