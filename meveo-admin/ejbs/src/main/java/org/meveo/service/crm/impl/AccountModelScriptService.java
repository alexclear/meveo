package org.meveo.service.crm.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.AccountEntity;
import org.meveo.model.admin.Seller;
import org.meveo.model.admin.User;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.account.AccountScript;
import org.meveo.service.script.account.AccountScriptInterface;

/**
 * @author Edward P. Legaspi
 **/
@Stateless
@Startup
public class AccountModelScriptService implements Serializable {

    private static final long serialVersionUID = -5209560989584270634L;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    // Interface methods
    public void createAccount(String scriptCode, Seller seller, AccountEntity account, User currentUser) throws BusinessException {
        AccountScriptInterface scriptInterface = (AccountScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, account);
        scriptContext.put(AccountScript.CONTEXT_SELLER, seller);
        scriptInterface.createAccount(scriptContext, currentUser);
    }

    public void updateAccount(String scriptCode, Seller seller, AccountEntity account, User currentUser) throws BusinessException {
        AccountScriptInterface scriptInterface = (AccountScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, account);
        scriptContext.put(AccountScript.CONTEXT_SELLER, seller);
        scriptInterface.updateAccount(scriptContext, currentUser);
    }

    public void terminateAccount(String scriptCode, Seller seller, AccountEntity account, User currentUser) throws BusinessException {
        AccountScriptInterface scriptInterface = (AccountScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, account);
        scriptContext.put(AccountScript.CONTEXT_SELLER, seller);
        scriptInterface.terminateAccount(scriptContext, currentUser);
    }

    public void closeAccount(String scriptCode, Seller seller, AccountEntity account, User currentUser) throws BusinessException {
        AccountScriptInterface scriptInterface = (AccountScriptInterface) scriptInstanceService.getScriptInstance(currentUser.getProvider(), scriptCode);
        Map<String, Object> scriptContext = new HashMap<String, Object>();
        scriptContext.put(Script.CONTEXT_ENTITY, account);
        scriptContext.put(AccountScript.CONTEXT_SELLER, seller);
        scriptInterface.closeAccount(scriptContext, currentUser);
    }

}
