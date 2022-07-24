package com.eka.ekaPricing.standalone;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ExpiryCalenderFetcher {
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(ExpiryCalenderFetcher.class);
	@Value("${eka.pricing.expiryCollection}")
	private String collection;

	@Value("${eka.contract.url}")
	private String connectHost;

	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	ContextProvider contextProvider;

	public JSONArray getData(Curve c, ContextProvider context) throws PricingException {
		String uri = connectHost + "/collectionmapper/" + pricingUDID + "/" + pricingUDID + "/fetchCollectionRecords";
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		JSONObject obj1 = new JSONObject();
		obj1.put("fieldName", "Instrument");
		obj1.put("value", c.getCurveName());
		obj1.put("operator", "eq");
		filterArr.put(obj1);
		fieldName.put("filter", filterArr);
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "Settlement Date");
		sortObj.put("direction", "ASC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		JSONObject outerObj = new JSONObject();
		outerObj.put("skip", "0");
		outerObj.put("limit", "10000");
		outerObj.put("collectionName", "DS - Expiry Calendar");
		outerObj.put("criteria", fieldName);
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
//		headers.add("X-Remote-User", "ekaApp");
		headers.add("ttl", "100");
		JSONArray expiryArray = new JSONArray();
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("expiry fetcher major- entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			expiryArray = new JSONArray(response.getBody());
			return expiryArray;
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("error while fetching expiry calender for : " + c.getCurveName()));
			return expiryArray;
		}
	}

}
