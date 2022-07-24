package com.eka.ekaPricing.standalone;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ErrorMessageFetcher {
	@Autowired
	ContextProvider contextProvider;
	final static Logger logger = ESAPI.getLogger(ErrorMessageFetcher.class);
	final String error = "Internal Server Error";
	@Value("${eka.contract.url}")
	private String contractURL;

	public String fetchErrorMessage(ContextProvider context, String errorCode, List<String> params)
			throws PricingException {
		String uri = contractURL + "/meta/pricing/getErrorMessage/" + errorCode;
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("X-Locale", contextProvider.getCurrentContext().getLocale());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		JSONObject bodyObj = new JSONObject();
		JSONArray paramsArray = new JSONArray();
		for(String parameter: params) {
			paramsArray.put(parameter);
		}
		bodyObj.put("parameters", paramsArray);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTMLAttribute("Error message fetcher - entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			String errorMessage = response.getBody();
			return errorMessage;
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Exception while fetching Error message details"));
			e.printStackTrace();
			throw new PricingException(error);
		}
	}
}
