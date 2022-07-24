package com.eka.ekaPricing.standalone;

import java.time.LocalDate;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ForwardCollectionDataFetcher {
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	CollectionDataFetcher collectionDataFetcher;

	@Value("${eka.contract.url}")
	private String connectHost;

	@Value("${eka.pricing.udid}")
	private String pricingUDID;

	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(ForwardCollectionDataFetcher.class);

	public JSONArray getForwardMarketData(Curve c, LocalDate asOf) {
		String latestAsof = getLatestAsonDate(c, asOf);
		String uri = connectHost + "/collectionmapper/" + pricingUDID + "/" + pricingUDID + "/fetchCollectionRecords";
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		JSONObject obj1 = new JSONObject();
		obj1.put("fieldName", "Instrument Name");
		obj1.put("value", c.getCurveName());
		obj1.put("operator", "eq");
		JSONObject obj2 = new JSONObject();
		obj2.put("fieldName", "As On Date");
		obj2.put("value", latestAsof);
		obj2.put("operator", "eq");
		filterArr.put(obj1);
		filterArr.put(obj2);
		JSONObject outerObj = new JSONObject();
		outerObj.put("skip", "0");
		outerObj.put("limit", "10000");
		outerObj.put("collectionName", "DS-Forward Prices");

		fieldName.put("filter", filterArr);
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "Pricing Date");
		sortObj.put("direction", "ASC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		outerObj.put("criteria", fieldName);
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
//		headers.add("X-Remote-User", "ekaApp");
		headers.add("ttl", "100");
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
		JSONArray arr = new JSONArray();
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("collection fetcher major- entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
//			JSONObject obj = new JSONObject(response.getBody());

			arr = new JSONArray(response.getBody());
//			if (arr.length() == 0 && c.getPricePoint().equalsIgnoreCase("Spot")) {
//				arr = getLatestRecord(headers, fromDateStr, toDateStr, c, restTemplate, uri, asOf, contextProvider);
//			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("exception for fetching forward market data at " + uri + " : " + e.getMessage()));
			List<String> params = new ArrayList<String>();
			params.add("DS-Forward Prices");
		}
		return arr;
	}
	
	public String getLatestAsonDate(Curve c, LocalDate asOf) {
		String uri = connectHost + "/collectionmapper/" + pricingUDID + "/" + pricingUDID + "/fetchCollectionRecords";
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		JSONObject obj1 = new JSONObject();
		obj1.put("fieldName", "Instrument Name");
		obj1.put("value", c.getCurveName());
		obj1.put("operator", "eq");
		filterArr.put(obj1);
		JSONObject outerObj = new JSONObject();
		outerObj.put("skip", "0");
		outerObj.put("limit", "10000");
		outerObj.put("collectionName", "DS-Forward Prices");

		fieldName.put("filter", filterArr);
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "As On Date");
		sortObj.put("direction", "DESC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		outerObj.put("criteria", fieldName);
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
//		headers.add("X-Remote-User", "ekaApp");
		headers.add("ttl", "100");
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
		JSONArray arr = new JSONArray();
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("collection fetcher major- entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
//			JSONObject obj = new JSONObject(response.getBody());

			arr = new JSONArray(response.getBody());
			for(int i=0; i<arr.length(); i++) {
				JSONObject object = arr.optJSONObject(i);
				return object.optString("As On Date");
			}
//			if (arr.length() == 0 && c.getPricePoint().equalsIgnoreCase("Spot")) {
//				arr = getLatestRecord(headers, fromDateStr, toDateStr, c, restTemplate, uri, asOf, contextProvider);
//			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
					.encodeForHTML("exception for fetching forward market data at " + uri + " : " + e.getMessage()));
			List<String> params = new ArrayList<String>();
			params.add("DS-Forward Prices");
		}
		return "";
	}
}
