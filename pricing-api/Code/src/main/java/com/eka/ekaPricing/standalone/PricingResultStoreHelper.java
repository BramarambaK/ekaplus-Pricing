package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.util.ContextProvider;

@Component	
public class PricingResultStoreHelper {

	@Value("${eka.pricing.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	GMRCreationHelper gmrCreationHelper;
	
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(PricingResultStoreHelper.class);
	
	public void storePricingResult(ContextProvider context, JSONObject payload, JSONObject outputJson) throws PricingException {
		String uri = connectHost + "/workflow";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		if(StringUtils.isEmpty(payload) || StringUtils.isEmpty(outputJson)) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception for saving pricing result due to incorrect data"));
		}
		JSONArray fieldsArr = new JSONArray();
		JSONObject fieldsToSave = createPayloadBody(payload, outputJson);
		if(StringUtils.isEmpty(fieldsToSave.optString("internalContractItemRefNo")) || 
				StringUtils.isEmpty(fieldsToSave.optString("internalContractRefNo"))) {
			return;
		}
		fieldsArr.put(fieldsToSave);
		JSONObject itemListingWithGMR = new JSONObject();
		itemListingWithGMR.put("savePricingResult", fieldsArr);
		
		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("workflowTaskName", "savePricingResult");
		bodyObj.accumulate("task", "savePricingResult");
		bodyObj.accumulate("appName", "pricing");
		bodyObj.accumulate("appId", appUUID);
		bodyObj.accumulate("output", itemListingWithGMR);
		
		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("pricing result store entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("pricing result store entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving pricing result : " + e.getMessage()));
			//throw new PricingException("Error while saving pricing result");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "040", new ArrayList<String>()));
		}
	}
	
	public JSONObject createPayloadBody(JSONObject payload, JSONObject outputJson) throws PricingException {
		JSONObject contractObj = outputJson.optJSONObject("contract");
		String contract = contractObj.optString("refNo");
		JSONObject itemObj = contractObj.optJSONArray("itemDetails").getJSONObject(0);
		String contractItem = itemObj.optString("refNo");
		String contractItemPrice = itemObj.optJSONObject("priceDetails").optString("contractPrice");
		Map<String, String> gmrPrices = new HashMap<String, String>();
		JSONArray gmrDetails = itemObj.optJSONArray("gmrDetails");
		JSONArray gmrDataArr = gmrCreationHelper.fetchGMRData(contractItem);
		JSONArray outpuatGMRArr = new JSONArray();
		if (null == gmrDetails) {
			gmrDetails = new JSONArray();
		}
		for (int i = 0; i < gmrDetails.length(); i++) {
			JSONObject gmrObj = gmrDetails.getJSONObject(i);
			gmrPrices.put(gmrObj.optString("refNo"), gmrObj.optString("price"));
		}
		for (int i = 0; i < gmrDataArr.length(); i++) {
			JSONObject gmrObj = gmrDataArr.getJSONObject(i);
			JSONObject gmrOutObj = new JSONObject();
			if (gmrPrices.containsKey(gmrObj.optString("refNo"))) {
				gmrOutObj.put(gmrObj.optString("refNo"), gmrPrices.get(gmrObj.optString("refNo")));
			}
			else {
				gmrOutObj.put(gmrObj.optString("refNo"), gmrObj.optDouble("estimatedPrice"));
			}
			outpuatGMRArr.put(gmrOutObj);
		}
		JSONObject fieldsToSave = new JSONObject();
		fieldsToSave.accumulate("payload", payload.toString());
		fieldsToSave.accumulate("outputJSON", outputJson.toString());
		fieldsToSave.accumulate("internalContractRefNo", contract);
		fieldsToSave.accumulate("internalContractItemRefNo", contractItem);
		fieldsToSave.accumulate("contractItemPrice", contractItemPrice);
		fieldsToSave.accumulate("gmrPrices", outpuatGMRArr);
		return fieldsToSave;
	}
}
