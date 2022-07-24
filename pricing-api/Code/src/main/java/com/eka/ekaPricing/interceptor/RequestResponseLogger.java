package com.eka.ekaPricing.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

import com.eka.ekaPricing.util.ContextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

@Aspect
@Component
public class RequestResponseLogger {
	@Autowired
	ContextProvider contextProvider;

	private static final Logger esapiRequestResponseLogger = ESAPI
			.getLogger(RequestResponseLogger.class.getName());
	 private static final org.slf4j.Logger  requestResponseLogger = LoggerFactory.getLogger(RequestResponseLogger.class.getName());
	 
	 private static final Logger esapiResponseBodyLogger = ESAPI
				.getLogger(RequestResponseLogger.class.getName()+".body");
	 
	 private static final org.slf4j.Logger  responseBodyLogger = LoggerFactory.getLogger(RequestResponseLogger.class.getName()+".body");
	 
	static {
		if (responseBodyLogger instanceof ch.qos.logback.classic.Logger) {
			((ch.qos.logback.classic.Logger) responseBodyLogger)
					.setLevel(Level.ERROR);
		}
	}
	
	 public static void logRequest(HttpServletRequest request)
			throws IOException {

		if (!requestResponseLogger.isDebugEnabled()) {
			return;
		}

		logUserRequest(request);

	}

	private static void logUserRequest(HttpServletRequest request) {
		try {
			if(request==null){
				return;
			}
			Object body = null;
			ByteArrayOutputStream cachedBytes = new ByteArrayOutputStream();
			IOUtils.copy(request.getInputStream(), cachedBytes);
			body = cachedBytes.toString("UTF-8");

			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"****************************************************************");
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"REQUEST URI --> " + request.getRequestURI());
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"HTTP METHOD  --> " + request.getMethod());

			Map<String, String> requestHeaders = new HashMap<>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				requestHeaders.put(headerName, request.getHeader(headerName));
			}

			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS, "REQUEST HEADERS  --> "
					+ requestHeaders);

			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"QUERY STRING  --> " + request.getQueryString());

			if(body!=null){
				try{
					body = new JSONObject(body.toString());
				}catch(Exception e){
					//do nothing.
				}
			}
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS, "REQUEST BODY  --> " +body.toString());

			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"****************************************************************");

		} catch (Exception e) {
			esapiRequestResponseLogger.error(Logger.EVENT_FAILURE,
					"exception occured while logging request.", e);
		}
	}

	public static void logResponseHeaders(HttpServletResponse response)
			throws IOException {

		if (!requestResponseLogger.isDebugEnabled()) {
			return;
		}

		logResponseHeaderDetails(response);

	}

	public static void logResponseHeaderDetails(HttpServletResponse response) {
		try {
			Map<String, String> responseHeaders = new HashMap<>();

			Collection<String> headerNames = response.getHeaderNames();
			for (String header : headerNames) {
				responseHeaders.put(header, response.getHeader(header));
			}

			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"****************************************************************");
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS, "RESPONSE HEADERS --> "
					+ responseHeaders);
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS, "RESPONSE STATUS --> "
					+ response.getStatus());
			esapiRequestResponseLogger.error(Logger.EVENT_SUCCESS,
					"****************************************************************");

		} catch (Exception e) {
			esapiRequestResponseLogger.error(Logger.EVENT_FAILURE,
					"exception occured while logging response.", e);
		}
	}

	@AfterReturning("execution(* com.eka.ekaPricing.resource..* (..))")
	public void afterReturningVoid(JoinPoint joinPoint) {

		try {
			if (responseBodyLogger.isDebugEnabled()) {

				esapiResponseBodyLogger.debug(Logger.EVENT_SUCCESS, "completd "
						+ joinPoint.getSignature().toString());
			}
		} catch (Exception e) {
			esapiResponseBodyLogger.error(Logger.EVENT_FAILURE,
					"exception occured while logging "
							+ joinPoint.getSignature().toString(), e);
		}
	}

	@AfterReturning(value = "execution(* com.eka.ekaPricing.resource..* (..))", returning = "result")
	public void afterReturning(JoinPoint joinPoint, Object result) {

		try {
			if (responseBodyLogger.isDebugEnabled()) {
				ObjectMapper mapper = new ObjectMapper();
				esapiResponseBodyLogger.debug(Logger.EVENT_SUCCESS, "Response Body of "
						+ joinPoint.getSignature().toString() + " -- > "
						+ mapper.writeValueAsString(result));

			}
		} catch (Exception e) {
			esapiResponseBodyLogger.error(Logger.EVENT_FAILURE,
					"exception occured while logging response of "
							+ joinPoint.getSignature().toString(), e);
		}
	}

	@AfterThrowing(value = "execution(* com.eka.ekaPricing.resource..* (..))", throwing = "e")
	public void afterThrowing(JoinPoint joinPoint, Throwable e) {
		if(contextProvider.getCurrentContext()!=null)
		logUserRequest(contextProvider.getCurrentContext().getRequest());
		esapiResponseBodyLogger.error(Logger.EVENT_FAILURE, joinPoint.getSignature().toString(),
				e);

	}

}
