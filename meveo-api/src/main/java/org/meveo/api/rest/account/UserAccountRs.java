package org.meveo.api.rest.account;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.account.ApplyProductRequestDto;
import org.meveo.api.dto.account.UserAccountDto;
import org.meveo.api.dto.response.account.GetUserAccountResponseDto;
import org.meveo.api.dto.response.account.UserAccountsResponseDto;
import org.meveo.api.dto.response.billing.GetCountersInstancesResponseDto;
import org.meveo.api.rest.IBaseRs;
import org.meveo.api.rest.security.RSSecured;

/**
 * @author Edward P. Legaspi
 **/
@Path("/account/userAccount")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@RSSecured
public interface UserAccountRs extends IBaseRs {

    @POST
    @Path("/")
    ActionStatus create(UserAccountDto postData);

    @PUT
    @Path("/")
    ActionStatus update(UserAccountDto postData);

    /**
     * Search for a user account with a given code.
     * 
     * @param userAccountCode
     * @return
     */
    @GET
    @Path("/")
    GetUserAccountResponseDto find(@QueryParam("userAccountCode") String userAccountCode);

    @DELETE
    @Path("/{userAccountCode}")
    ActionStatus remove(@PathParam("userAccountCode") String userAccountCode);

    /**
     * List UserAccount filter by billingAccountCode.
     * 
     * @param billingAccountCode
     * @return
     */
    @GET
    @Path("/list")
    UserAccountsResponseDto listByBillingAccount(@QueryParam("billingAccountCode") String billingAccountCode);

    @POST
    @Path("/createOrUpdate")
    ActionStatus createOrUpdate(UserAccountDto postData);
    
    /**
     * filter counters by period date
     * @param userAccountCode
     * @param date
     * @return
     */
    @GET
    @Path("/filterCountersByPeriod")
	GetCountersInstancesResponseDto filterUserAccountCountersByPeriod(@QueryParam("userAccountCode") String userAccountCode, 
			@QueryParam("date") String date);

    /**
     * Apply a product on a userAccount.
     * @param ApplyProductRequestDto userAccount field must be set
     * @return
     */
    @POST
    @Path("/applyProduct")
    ActionStatus applyProduct(ApplyProductRequestDto postData);
}
