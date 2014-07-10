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
package org.meveo.admin.action.billing;

import java.util.Date;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.solder.servlet.http.RequestParam;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.billing.InstanceStatusEnum;
import org.meveo.model.billing.ServiceInstance;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.billing.impl.ServiceInstanceService;
import org.omnifaces.util.Messages;

/**
 * Standard backing bean for {@link ServiceInstance} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their
 * create, edit, view, delete operations). It works with Manaty custom JSF components.
 * 
 * 
 */
@Named
@ConversationScoped
public class ServiceInstanceBean extends BaseBean<ServiceInstance> {

    private static final long serialVersionUID = 1L;

    /**
     * Injected
     * 
     * @{link ServiceInstance} service. Extends {@link PersistenceService}.
     */
    @Inject
    private ServiceInstanceService serviceInstanceService;
  

    /**
     * Offer Id passed as a parameter. Used when creating new Service from Offer window, so default offer will be set on newly created service.
     */
    @Inject
    @RequestParam
    private Instance<Long> offerInstanceId;

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public ServiceInstanceBean() {
        super(ServiceInstance.class);
    }

    /**
     * Factory method for entity to edit. If objectId param set load that entity from database, otherwise create new.
     * 
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Override
    public ServiceInstance initEntity() {
        super.initEntity();
        if (offerInstanceId!=null &&  offerInstanceId.get() != null) {
//            entity.setOfferInstance(offerInstanceService.findById(offerInstanceId.get());
        }
        return entity;
    }


    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<ServiceInstance> getPersistenceService() {
        return serviceInstanceService;
    }

    public String serviceInstanciation(ServiceInstance serviceInstance) {
        log.info("serviceInstanciation serviceInstanceId:" + serviceInstance.getId());
        try {
            serviceInstanceService.serviceInstanciation(serviceInstance, getCurrentUser());
        } catch (BusinessException e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String activateService() {
        log.info("activateService serviceInstanceId:" + entity.getId());
        try {
            serviceInstanceService.serviceActivation(entity, null, null, getCurrentUser());
            Messages.createInfo( "activation.activateSuccessful");
            return "/pages/resource/serviceInstances/serviceInstanceDetail.xhtml?objectId=" + entity.getId() + "&edit=false";
        } catch (BusinessException e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String resiliateService() {
        log.info("resiliateService serviceInstanceId:" + entity.getId());
        try {
            // serviceInstanceService.serviceTermination(serviceInstance, new
            // Date(), currentUser);
            Messages.createInfo( "resiliation.resiliateSuccessful");
            return "/pages/resource/serviceInstances/serviceInstanceDetail.xhtml?objectId=" + entity.getId() + "&edit=false";

        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String resiliateWithoutFeeService() {
        log.info("cancelService serviceInstanceId:" + entity.getId());
        try {
            // serviceInstanceService.serviceCancellation(serviceInstance, new
            // Date(), currentUser);
            Messages.createInfo( "cancellation.cancelSuccessful");
            return "/pages/resource/serviceInstances/serviceInstanceDetail.xhtml?objectId=" + entity.getId() + "&edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String cancelService() {
        log.info("cancelService serviceInstanceId:" + entity.getId());
        try {
            entity.setStatus(InstanceStatusEnum.CANCELED);
            serviceInstanceService.update(entity, getCurrentUser());
            Messages.createInfo( "resiliation.resiliateSuccessful");
            return "/pages/resource/serviceInstances/serviceInstanceDetail.xhtml?objectId=" + entity.getId() + "&edit=false";
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }

    public String suspendService() {
        log.info("closeAccount serviceInstanceId:" + entity.getId());
        try {
            serviceInstanceService.serviceSuspension(entity, new Date(), getCurrentUser());
            Messages.createInfo( "suspension.suspendSuccessful");
            return "/pages/resource/serviceInstances/serviceInstanceDetail.xhtml?objectId=" + entity.getId() + "&edit=false";
        } catch (BusinessException e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Messages.createError(e.getMessage());
        }
        return null;
    }
}
