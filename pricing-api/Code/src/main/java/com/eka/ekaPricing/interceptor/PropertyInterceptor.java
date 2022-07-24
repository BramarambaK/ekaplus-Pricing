package com.eka.ekaPricing.interceptor;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.eka.ekaPricing.EkaPricingApplication.MyException;
import com.eka.ekaPricing.pojo.ContextInfo;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.standalone.GlobalConstants;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;


 @Component
public class PropertyInterceptor implements HandlerInterceptor {


 	@Value("${eka.properties.host}")
	private String propertyAPIEndpoint;
 	@Value("${eka.pricing.udid}")
 	private String pricingUDID;
 	@Autowired
 	ContextInfo requestContext;
 	@Autowired
	ContextProvider contextProvider;
 	@Autowired
 	CommonValidator validator;
 	final static Logger logger = ESAPI.getLogger(PropertyInterceptor.class);

 	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		setTenantNameAndRequestIdToLog(request);
		
		String requestURI = request.getRequestURI();
		String requestMethod = request.getMethod();
		logger.info(Logger.EVENT_SUCCESS, "********* Pricing-PreHandle Started......"+"Request Details: " + requestMethod + " " + requestURI);
		
		RequestResponseLogger.logRequest(request);
		setEnvirnomentProps(request);
		
		logger.info(Logger.EVENT_SUCCESS, "********* Pricing-PreHandle Completed......"+"Request Details: " + requestMethod + " " + requestURI);

		return true;
	}

 	public void setEnvirnomentProps(HttpServletRequest request) throws Exception {
 		String token = validator.cleanData(request.getHeader("Authorization"));
		String tenantId = validator.cleanData(request.getHeader("X-TenantID"));
		String locale = validator.cleanData(request.getHeader("X-Locale"));
		String origin = validator.cleanData(request.getHeader("origin"));
		if (null == tenantId) {
			throw new MyException();
		}

		if (null == locale) {
			throw new MyException();
		}
		
		if (null == token) {
			throw new MyException();
		}
//		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(" first check context : "
//				+ contextProvider.getCurrentContext().getTenantID() + contextProvider.getCurrentContext().getToken()));
//		if(!tenantId.contains("mexgaspreprod")) {
//			logger.info(Logger.EVENT_SUCCESS,
//					ESAPI.encoder().encodeForHTML(" headers for non-mexgas tenants : " + tenantId + token));
//			throw new PricingException("testing for mexgaspreprod");
//		}
 		ContextInfo freshContext = contextProvider.getCurrentContext();
        freshContext.setLocale(locale);
        freshContext.setTenantID(tenantId);
        freshContext.setToken(token);
        freshContext.setRequest(request);
        freshContext.setOrigin(origin);
        
        contextProvider.setCurrentContext(freshContext);
 		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization",validator.cleanData( request.getHeader("Authorization")));
		headers.add("X-TenantID", validator.cleanData(request.getHeader("X-TenantID")));
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		headers.add("origin", contextProvider.getCurrentContext().getOrigin());
//		System.out.println("===============");
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("header coming from caller : "+headers));
		logger.debug(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(" second check context : "
				+ contextProvider.getCurrentContext().getTenantID() + contextProvider.getCurrentContext().getToken()));
		HttpEntity<String> requestBody = new HttpEntity<String>(new JSONObject().toString(), headers);
		RestTemplate template = new RestTemplate();
		try {

			ResponseEntity<String> responseEntity = template.exchange(validator.cleanData(propertyAPIEndpoint), HttpMethod.POST,
					requestBody, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder()
					.encodeForHTMLAttribute("property fetcher - output: " + responseEntity.getBody()));
			if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {

				// Prepare ApplicationPros obj and set it

				String propsStr = responseEntity.getBody();

				Gson gson = new Gson();
				PricingProperties props = gson.fromJson(propsStr, PricingProperties.class);
//				BeanUtils.copyProperties(props, pricingProperties);
//				requestContext.setPricingProperties(props);
				contextProvider.getCurrentContext().setPricingProperties(props);

			} else {
				throw new Exception("Failed to set Property API.");
			}

		} catch (HttpStatusCodeException ex) {

			logger.error(Logger.EVENT_FAILURE,
					"Failed to call Property API: " + ex.getMessage() + ex.getResponseBodyAsString(), ex);

			throw new Exception("Failed to call Property API: " + ex.getMessage() + ex);

		}
		catch (Exception ex) {

			logger.error(Logger.EVENT_FAILURE, "Failed to call Property API in final exception: " + ex.getMessage(),
					ex);

			throw new Exception("Failed to call Property API: " + ex.getMessage() + ex);

		}
 		

 	}
 	
 	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			@Nullable ModelAndView modelAndView) throws Exception {
 		RequestResponseLogger.logResponseHeaders(response);
	}
 	
 	@Override
 	public void afterCompletion(HttpServletRequest request,
 			HttpServletResponse response, Object handler, Exception ex)
 			throws Exception {
 		String requestURI = request.getRequestURI();
		String requestMethod = request.getMethod();
		response.addHeader(GlobalConstants.REQUEST_ID, contextProvider.getCurrentContext().getRequestId());
		if(ex!=null){
			RequestResponseLogger.logResponseHeaderDetails(response);
		}
		logger.info(Logger.EVENT_SUCCESS,
				"********* Pricing User Request completed......"
						+ "Request Details: " + requestMethod + " "
						+ requestURI);
		removeContext();
		MDC.clear();
 		
 	}
 	
	private void setTenantNameAndRequestIdToLog(HttpServletRequest request) {
		String requestId = null;
		String sourceDeviceId = null;
		String tenantName = null;
		if (null != request.getHeader("requestId")) {
			requestId = validator.cleanData(request.getHeader("requestId"));
		} else {
			requestId = UUID.randomUUID().toString().replace("-", "")+"-GEN";

		}if (null != request.getHeader("sourceDeviceId")) {
			sourceDeviceId = validator.cleanData(request.getHeader("sourceDeviceId"));
		} else {
			sourceDeviceId = "na";

		}
		MDC.put("requestId", requestId);
		MDC.put("sourceDeviceId", sourceDeviceId);

		if (null != request.getHeader("X-TenantID")) {
			tenantName = validator.cleanData(request.getHeader("X-TenantID"));
			MDC.put("tenantName", tenantName);
		}
		ContextInfo freshContext = new ContextInfo();
		freshContext.setRequestId(requestId);
		freshContext.setSourceDeviceId(sourceDeviceId);
		contextProvider.setCurrentContext(freshContext);


	}


	private void removeContext() {
		contextProvider.remove();
	}

 }
