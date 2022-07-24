package com.eka.ekaPricing.standalone;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class EODProcessor {
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.pricingResult.udid}")
	private String pricingResultUDID;
	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	ContextProvider contextProvider;
	final static  Logger logger = ESAPI.getLogger(EODProcessor.class);
	
	public JSONArray processEOD(List<String> items) throws PricingException {
		JSONArray result = new JSONArray();
		for(String item: items) {
			JSONObject resultObj = fetchStoredDocs(item);
			JSONObject res = prepareResult(resultObj);
//			JSONObject res = new JSONObject();
//			res.put("contractPrice", resultObj.optDouble("contractItemPrice"));
//			res.put("gmrPrices", resultObj.optJSONArray("gmrPrices").optJSONArray(0));
//			res.put("internalContractRefNo", resultObj.optString("internalContractRefNo"));
//			res.put("internalContractItemRefNo", resultObj.optString("internalContractItemRefNo"));
			result.put(res);
		}
		return result;
	}
	
	public JSONObject fetchStoredDocs(String item) throws PricingException {
		String uri = contractURL + "/data/" + pricingUDID + "/" + pricingResultUDID + "?internalContractItemRefNo=" + item;
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTMLAttribute("fetchStoredDocs - entity: " + entity));
			HttpEntity<String> formula = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			String json = formula.getBody().toString();
			json = json.substring(1, json.length() - 1);
//			System.out.println("formula : "+json);
			JSONObject resultObj = new JSONObject(json);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchStoredDocs - : " + resultObj));
			return resultObj;
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exception at " + uri + " : " + e.getMessage()));
			List<String> params = new ArrayList<String>();
			params.add("Stored Pricing Object");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));

		}
	}
	
	public JSONObject prepareResult(JSONObject resultObj) {
		JSONObject outputJSON = new JSONObject(resultObj.optString("outputJSON"));
		return outputJSON;
	}
}
