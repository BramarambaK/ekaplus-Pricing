package com.eka.ekaPricing.standalone;

import java.util.ArrayList;

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
import com.eka.ekaPricing.pojo.FixationObject;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class GMRStatusObjectCreationHelper {
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Autowired
	ContextProvider contextProvider;
	
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(GMRStatusObjectCreationHelper.class);
	
	public void createGMRStatusObject(GMRStatusObject gmrStatusObject)
			throws PricingException {
		String uri = connectHost + "/workflow";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());

		JSONArray fieldsArr = new JSONArray();
		JSONObject fieldsToSave = new JSONObject();
		fieldsToSave.accumulate("internalContractRefNo", gmrStatusObject.getInternalContractRefNo());
		fieldsToSave.accumulate("internalContractItemRefNo", gmrStatusObject.getInternalContractItemRefNo());
		fieldsToSave.accumulate("gmrRefNo", gmrStatusObject.getGmrRefNo());
		fieldsToSave.accumulate("gmrId", gmrStatusObject.getGmrId());
		fieldsToSave.accumulate("gmrQty", gmrStatusObject.getGmrQty());
		fieldsToSave.accumulate("gmrFixedQty", gmrStatusObject.getGmrFixedQty());
		fieldsToSave.accumulate("gmrUnFixedQty", gmrStatusObject.getGmrUnFixedQty());
		fieldsToSave.accumulate("gmrCancelledQty", gmrStatusObject.getGmrCancelledQty());
		fieldsToSave.accumulate("gmrStatus", gmrStatusObject.getGmrStatus());
		fieldsToSave.accumulate("qtyUnitId", gmrStatusObject.getQtyUnitId());
		fieldsToSave.accumulate("qtyUnitVal", gmrStatusObject.getQtyUnitVal());
		Gson gson = new Gson();
		JSONArray fixationArray = new JSONArray();
		if(!gmrStatusObject.getGmrStatus().equals("CANCELLED")) {
			for(FixationObject fixation: gmrStatusObject.getFixationUsed()) {
				JSONObject fixationObj = new JSONObject(gson.toJson(fixation));
				fixationArray.put(fixationObj);
			}
		}
		fieldsToSave.put("fixationUsed", fixationArray);
		fieldsArr.put(fieldsToSave);
		JSONObject itemListingWithGMR = new JSONObject();
		itemListingWithGMR.put("gmr-trigger-price", fieldsArr);

		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("workflowTaskName", "gmr-trigger-price");
		bodyObj.accumulate("task", "gmr-trigger-price");
		bodyObj.accumulate("appName", "physicals");
		bodyObj.accumulate("appId", appUUID);
		bodyObj.accumulate("output", itemListingWithGMR);

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("gmr creation entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("gmr creation entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving gmr : " ),e);
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "038", new ArrayList<String>()));
		}

	}
}
