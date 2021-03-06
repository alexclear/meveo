/**
 * 
 */
package org.meveo.admin.async;

import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.job.importexport.ImportAccountsJobBean;
import org.meveo.model.admin.User;
import org.meveo.model.jobs.JobExecutionResultImpl;

/**
 * @author anasseh
 * 
 */

@Stateless
public class ImportAccountsAsync {

    @Inject
    private ImportAccountsJobBean importAccountsJobBean;

    @Asynchronous
    public Future<String> launchAndForget(JobExecutionResultImpl result, User currentUser) {
        importAccountsJobBean.execute(result, currentUser);

        return new AsyncResult<String>("OK");
    }
}
