package com.eka.ekaPricing.standalone;

import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PricingCallHelper {
	@Value("${eka.pricing.host}")
	private String pricingHost;
	@Value("${eka.pricing.udid}")
	private String pringUDID;
	
	String path = "/api/pricing/formula?mode=Detailed";
	final static  org.owasp.esapi.Logger logger = ESAPI.getLogger(CollectionDataFetcher.class);
	
	@Async
	public boolean callPricing(JSONObject payload, HttpHeaders headers) {
		String uri = pricingHost + path;
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<String>(payload.toString(), headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			if(response.getStatusCodeValue()==200) {
				return true;
			}
			
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,ESAPI.encoder().encodeForHTML("Pricing reevaluation call failed"));
		}
		return false;
	}
}
