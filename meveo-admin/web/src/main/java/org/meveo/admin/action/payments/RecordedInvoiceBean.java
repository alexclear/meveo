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
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.payments.RecordedInvoice;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.payments.impl.RecordedInvoiceService;
import org.omnifaces.util.Messages;

/**
 * Standard backing bean for {@link RecordedInvoice} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 * 
 * @author Ignas
 * @created 2009.10.13
 */
@Named
@ConversationScoped
public class RecordedInvoiceBean extends BaseBean<RecordedInvoice> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected @{link RecordedInvoice} service. Extends {@link PersistenceService}.
     */
    @Inject
    private RecordedInvoiceService recordedInvoiceService;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public RecordedInvoiceBean() {
        super(RecordedInvoice.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Produces
    @Named("recordedInvoice")
    public RecordedInvoice init() {
        return initEntity();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.meveo.admin.action.BaseBean#saveOrUpdate(boolean)
     */
    @Override
    public String saveOrUpdate(boolean killConversation) {
        entity.getCustomerAccount().getAccountOperations().add(entity);
        return super.saveOrUpdate(killConversation);
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<RecordedInvoice> getPersistenceService() {
        return recordedInvoiceService;
    }

    public String addLitigation() {
        try {
            recordedInvoiceService.addLitigation(entity, getCurrentUser());
            Messages.createInfo( "customerAccount.addLitigationSuccessful");
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String cancelLitigation() {

        try {
            recordedInvoiceService.cancelLitigation(entity, getCurrentUser());
            Messages.createInfo( "customerAccount.cancelLitigationSuccessful");
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

}
