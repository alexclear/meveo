package org.meveo.service.script.service;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.admin.User;
import org.meveo.service.script.module.ModuleScript;

/**
 * @author Edward P. Legaspi
 **/
public class ServiceScript extends ModuleScript implements ServiceScriptInterface {

    public static String CONTEXT_ACTIVATION_DATE = "CONTEXT_ACTIVATION_DATE";
    public static String CONTEXT_SUSPENSION_DATE = "CONTEXT_SUSPENSION_DATE";
    public static String CONTEXT_TERMINATION_DATE = "CONTEXT_TERMINATION_DATE";
    public static String CONTEXT_TERMINATION_REASON = "CONTEXT_TERMINATION_REASON";

    @Override
    public void createServiceTemplate(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void updateServiceTemplate(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void instantiateServiceInstance(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void activateServiceInstance(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void suspendServiceInstance(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void reactivateServiceInstance(Map<String, Object> methodContext, User user) throws BusinessException {

    }

    @Override
    public void terminateServiceInstance(Map<String, Object> methodContext, User user) throws BusinessException {

    }
}