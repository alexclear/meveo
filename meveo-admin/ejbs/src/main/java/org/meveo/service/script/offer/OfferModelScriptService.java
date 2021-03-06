package org.meveo.service.script.offer;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.exception.ElementNotFoundException;
import org.meveo.admin.exception.InvalidScriptException;
import org.meveo.model.admin.User;
import org.meveo.model.billing.Subscription;
import org.meveo.model.billing.SubscriptionTerminationReason;
import org.meveo.model.catalog.OfferTemplate;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

/**
 * @author Edward P. Legaspi
 **/
@Singleton
@Startup
public class OfferModelScriptService implements Serializable {

    private static final long serialVersionUID = -2580475102375024245L;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    public void subscribe(Subscription entity, String scriptCode, User currentUser) throws ElementNotFoundException, InvalidScriptException, BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.subscribe(scriptContext, currentUser);
    }

    public void terminateSubscription(Subscription entity, String scriptCode, Date terminationDate, SubscriptionTerminationReason terminationReason, User currentUser)
            throws ElementNotFoundException, InvalidScriptException, BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.put(OfferScript.CONTEXT_TERMINATION_DATE, terminationDate);
        scriptContext.put(OfferScript.CONTEXT_TERMINATION_REASON, terminationReason);
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.terminateSubscription(scriptContext, currentUser);
    }

    public void suspendSubscription(Subscription entity, String scriptCode, Date suspensionDate, User currentUser) throws ElementNotFoundException, InvalidScriptException,
            BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(OfferScript.CONTEXT_SUSPENSION_DATE, suspensionDate);
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.suspendSubscription(scriptContext, currentUser);
    }

    public void reactivateSubscription(Subscription entity, String scriptCode, Date activationDate, User currentUser) throws ElementNotFoundException, InvalidScriptException,
            BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(OfferScript.CONTEXT_ACTIVATION_DATE, activationDate);
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.reactivateSubscription(scriptContext, currentUser);
    }

    public void createOfferTemplate(OfferTemplate entity, String scriptCode, User currentUser) throws ElementNotFoundException, InvalidScriptException, BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.createOfferTemplate(scriptContext, currentUser);
    }

    public void updateOfferTemplate(OfferTemplate entity, String scriptCode, User currentUser) throws ElementNotFoundException, InvalidScriptException, BusinessException {
        OfferScriptInterface scriptInterface = (OfferScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, entity);
        scriptInterface.updateOfferTemplate(scriptContext, currentUser);
    }
}