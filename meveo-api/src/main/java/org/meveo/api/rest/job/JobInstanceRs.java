package org.meveo.api.rest.job;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.job.JobInstanceDto;
import org.meveo.api.dto.response.job.JobInstanceResponseDto;
import org.meveo.api.rest.IBaseRs;
import org.meveo.api.rest.security.RSSecured;

/**
 * 
 * @author Manu Liwanag
 *
 */
@Path("/jobInstance")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@RSSecured
public interface JobInstanceRs extends IBaseRs {

	@Path("/create")
	@POST
	ActionStatus create(JobInstanceDto postData);

	@Path("/update")
	@POST
	ActionStatus update(JobInstanceDto postData);

	@Path("/createOrUpdate")
	@POST
	ActionStatus createOrUpdate(JobInstanceDto postData);

	@Path("/")
	@GET
	JobInstanceResponseDto find(@QueryParam("jobInstanceCode") String jobInstanceCode);

	@Path("/{jobInstanceCode}")
	@DELETE
	ActionStatus remove(@PathParam("jobInstanceCode") String jobInstanceCode);
	
}
