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
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class UpdateTriggerPriceObjectHelper {
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.pricing.udid}")
	private String appUUID;
	@Value("${eka.pricing.triggerPrice.objectUUID}")
	private String objUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CurveService curveService;
	
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(UpdateTriggerPriceObjectHelper.class);
	
	public void updateTriggerPrice(TriggerPrice triggerPrice) throws PricingException {
		String uri = connectHost + "/workflow";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());

		JSONArray fieldsArr = new JSONArray();
		JSONObject fieldsToSave = new JSONObject();
		fieldsToSave.accumulate("internalContractRefNo", triggerPrice.getInternalContractRefNo());
		fieldsToSave.accumulate("internalContractItemRefNo", triggerPrice.getInternalContractItemRefNo());
		fieldsToSave.accumulate("itemFixedQtyAvailable", triggerPrice.getItemFixedQtyAvailable());
		if(curveService.checkZero(triggerPrice.getItemFixedQtyAvailable())) {
			fieldsToSave.accumulate("fixationStatus", "FULLY FIXED");
		}
		else {
			fieldsToSave.accumulate("fixationStatus", "Active");
		}
		fieldsToSave.accumulate("fixationRefNo", triggerPrice.getFixationRefNo());
		
		fieldsArr.put(fieldsToSave);
		JSONObject itemListingWithGMR = new JSONObject();
		itemListingWithGMR.put("triggerdata_save", fieldsArr);

		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("workflowTaskName", "triggerdata_save");
		bodyObj.accumulate("task", "triggerdata_save");
		bodyObj.accumulate("appName", "pricing");
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
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving gmr : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "038", new ArrayList<String>()));
		}

	}
	public void deleteTriggerPrice(TriggerPrice trigger)
			throws PricingException {
		String uri = connectHost + "/data/" +appUUID+"/"+objUUID+"/bulkDelete";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());

		JSONObject filter = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		filter.put("fieldName", "fixationRefNo");
		filter.put("value", trigger.getFixationRefNo());
		filter.put("operator", "eq");
		fieldsArr.put(filter);
		JSONObject filterData = new JSONObject();
		filterData.put("filter", fieldsArr);
		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("filterData", filterData);

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Delete Trigger Price entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Delete Trigger Price  entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for Deleting Trigger Price : " + e.getMessage()));
			throw new PricingException("Error while deleting Trigger Price");
		}

	}
}
