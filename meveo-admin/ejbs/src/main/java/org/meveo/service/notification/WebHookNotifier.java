package org.meveo.service.notification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.IEntity;
import org.meveo.model.notification.NotificationHistoryStatusEnum;
import org.meveo.model.notification.WebHook;
import org.meveo.model.notification.WebHookMethodEnum;
import org.meveo.service.base.ValueExpressionWrapper;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.ScriptInterface;
import org.meveo.util.MeveoJpaForJobs;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
import org.slf4j.Logger;

@Stateless
public class WebHookNotifier {

	@Inject
	@MeveoJpaForJobs
	private EntityManager em;

	@Inject
	Logger log;

	@Inject
	NotificationHistoryService notificationHistoryService;
	
	@Inject
	ScriptInstanceService scriptInstanceService;

	private String evaluate(String expression, IEntity e,Map<String, Object> context) throws BusinessException {
		HashMap<Object, Object> userMap = new HashMap<Object, Object>();
		userMap.put("event", e);
		userMap.put("context", context);
		return (String) ValueExpressionWrapper.evaluateExpression(expression, userMap, String.class);
	}

	private Map<String, String> evaluateMap(Map<String, String> map, IEntity e,Map<String, Object> context) throws BusinessException {
		Map<String, String> result = new HashMap<String, String>();
		HashMap<Object, Object> userMap = new HashMap<Object, Object>();
		userMap.put("event", e);
		userMap.put("context", context);

		for (String key : map.keySet()) {
			result.put(key, (String) ValueExpressionWrapper.evaluateExpression(map.get(key), userMap, String.class));
		}

		return result;
	}

	@Asynchronous
	public void sendRequest(WebHook webHook, IEntity e,Map<String, Object> context) {
		log.debug("webhook sendRequest");
		String result = "";

		try {
			String url = webHook.getHost().startsWith("http") ? webHook.getHost() : "http://" + webHook.getHost();
			if (webHook.getPort() > 0) {
				url += ":" + webHook.getPort();
			}

			if (!StringUtils.isBlank(webHook.getPage())) {
				url += "/" + evaluate(webHook.getPage(), e,context);
			}
			Map<String,String> params = evaluateMap(webHook.getWebhookParams(), e,context);
            String paramQuery="";
            String sep="";
            for(String paramKey:params.keySet()){
            	paramQuery+=sep+URLEncoder.encode(paramKey, "UTF-8")+"="+URLEncoder.encode(params.get(paramKey), "UTF-8");
            	sep="&";
            }
            if(WebHookMethodEnum.HTTP_GET == webHook.getHttpMethod()){
            	url+="?"+paramQuery;
            } else {
            	log.debug("paramQuery={}",paramQuery);
            }
			log.debug("webhook url: {}", url);
			URL obj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();

            Map<String, String> headers = evaluateMap(webHook.getHeaders(), e,context);
            if(!StringUtils.isBlank(webHook.getUsername()) && !headers.containsKey("Authorization")){
     			byte[] bytes = Base64.encodeBase64((webHook.getUsername() + ":" + webHook.getPassword()).getBytes());
     			headers.put("Authorization", "Basic "+new String(bytes));
			}
           
			for (String key : headers.keySet()) {
		        conn.setRequestProperty(key, headers.get(key));
			}
			
			if (WebHookMethodEnum.HTTP_GET == webHook.getHttpMethod()) {
				conn.setRequestMethod("GET");
			} else if (WebHookMethodEnum.HTTP_POST == webHook.getHttpMethod()) {
				conn.setRequestMethod("POST");
			} else if (WebHookMethodEnum.HTTP_PUT == webHook.getHttpMethod()) {
				conn.setRequestMethod("PUT");
			} else if (WebHookMethodEnum.HTTP_DELETE == webHook.getHttpMethod()) {
				conn.setRequestMethod("DELETE");
			}
			conn.setUseCaches(false);
			
            if(WebHookMethodEnum.HTTP_GET != webHook.getHttpMethod()){
            	conn.setDoOutput(true);
	            OutputStream os = conn.getOutputStream();
	            BufferedWriter writer = new BufferedWriter(
	                    new OutputStreamWriter(os, "UTF-8"));
	            writer.write(paramQuery);
	            writer.flush();
	            writer.close();
	            os.close();
            }
			int responseCode = conn.getResponseCode();
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			result = response.toString();
			if(responseCode!=200){
				try {
					log.debug("webhook httpStatus error : " + responseCode + " response="+result);
					notificationHistoryService.create(webHook, e,"http error status="+responseCode +" response="+result,
							NotificationHistoryStatusEnum.FAILED);
				} catch (BusinessException e2) {
					log.error("Failed to create webhook ",e);
				}
			} else {
				if(webHook.getScriptInstance() != null){
				HashMap<Object, Object> userMap = new HashMap<Object, Object>();
				userMap.put("event", e);
				userMap.put("response",result);
			
			        Class<ScriptInterface> scriptInterfaceClass = scriptInstanceService.getScriptInterface(webHook.getScriptInstance().getProvider(),webHook.getScriptInstance().getCode());
			        try{
			        	ScriptInterface scriptInterface = scriptInterfaceClass.newInstance();
			        	Map<String, Object> paramsEvaluated = new HashMap<String, Object>();
			        	paramsEvaluated.putAll(webHook.getParams());
			            
			        	for (@SuppressWarnings("rawtypes") Map.Entry entry : paramsEvaluated.entrySet()) {
			        	    paramsEvaluated.put((String) entry.getKey(), ValueExpressionWrapper.evaluateExpression( (String)entry.getValue(), userMap, String.class));
			        	}
			        	paramsEvaluated.put("response",result);
				    	scriptInterface.execute(paramsEvaluated,webHook.getScriptInstance().getProvider(),webHook.getScriptInstance().getAuditable().getCreator());
			        } catch(Exception ee){
			        	log.error("failed script execution",ee);
			        }
			    
				}
				notificationHistoryService.create(webHook, e, result, NotificationHistoryStatusEnum.SENT);
				log.debug("webhook answer : " + result);
			}
		} catch (BusinessException e1) {
			try {
				log.debug("webhook business error : ",e1);
				notificationHistoryService.create(webHook, e, e1.getMessage(), NotificationHistoryStatusEnum.FAILED);
			} catch (BusinessException e2) {
				log.error("Failed to create webhook business ",e2);
				 
			}
		} catch (IOException e1) {
			try { 
				log.debug("webhook io error : ",e1);
				notificationHistoryService.create(webHook, e, e1.getMessage(), NotificationHistoryStatusEnum.TO_RETRY);
			} catch (BusinessException e2) {
				log.error("Failed to create webhook io ",e2);
			}
		}
	}
	
	public static void main(String[] args){
		
	
			String response = "{\"private_id\": \"1234567821@clearwater.local\", \"sip_username\": \"1234567821\", \"formatted_number\": \"234567821\", \"number\": \"1234567821\", \"sip_uri\": \"sip:1234567821@clearwater.local\", \"number_id\": \"65d3da8967a84e2e8350d3ae2b9d6bbc\", \"sip_password\": \"GzVpwH3PX\", \"pstn\": false}";		
			System.out.println("value  :"+ StringUtils.patternMacher("\"sip_uri\": \"(.*?)\",", response));
		
		
	}
}
