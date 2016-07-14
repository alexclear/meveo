package org.meveo.api.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.payment.DunningPlanTransitionDto;
import org.meveo.api.dto.response.payment.DunningPlanTransitionResponseDto;

@WebService
public interface DunningPlanTransitionWs extends IBaseWs {
 
	    @WebMethod
	    ActionStatus create(@WebParam(name = "dunningPlanTransition") DunningPlanTransitionDto postData);
	    
	    @WebMethod
	    ActionStatus update(@WebParam(name = "dunningPlanTransition") DunningPlanTransitionDto postData);
	    
	    @WebMethod
	    ActionStatus createOrUpdate(@WebParam(name = "dunningPlanTransition") DunningPlanTransitionDto postData);
	    
	    @WebMethod
	    DunningPlanTransitionResponseDto find(@WebParam(name = "dunningPlanCode") String dunningPlanCode ,
	    		@WebParam(name = "dunningLevelFrom") String dunningLevelFrom, 
	    		@WebParam(name = "dunningLevelTo") String dunningLevelTo);
	    
	    @WebMethod
	    ActionStatus remove(@WebParam(name = "dunningPlanCode") String dunningPlanCode ,
	    		@WebParam(name = "dunningLevelFrom") String dunningLevelFrom, 
	    		@WebParam(name = "dunningLevelTo") String dunningLevelTo);
	    
}