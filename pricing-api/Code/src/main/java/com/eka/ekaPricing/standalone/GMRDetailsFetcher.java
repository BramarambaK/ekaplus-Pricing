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
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class GMRDetailsFetcher {
	final static Logger logger = ESAPI.getLogger(GMRDetailsFetcher.class);
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.storedGMR.objectUUID}")
	private String objUUID;

	Gson gson = new Gson();
	public List<String> fetchActiveGMRFromTRM(String internalContractItemRefNo) throws PricingException {
		String platformURL = contextProvider.getCurrentContext().getPricingProperties().getPlatform_url();
		String uri = platformURL + "/api/contract/gmr/" + internalContractItemRefNo;
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		RestTemplate restTemplate = new RestTemplate();
		List<String> activeGRMList = new ArrayList<String>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("GMR details fetcher entity: "+entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("GMR details fetcher response: "+response));
			JSONArray gmrItems = new JSONObject(response.getBody()).optJSONArray("items");
			try {
				for(int i=0; i<gmrItems.length(); i++) {
					JSONObject gmrObject = gmrItems.optJSONObject(i);
					activeGRMList.add(gmrObject.optString("internalRefNo"));
				}
			}
			catch (Exception e) {
				logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while parsing GMR details"));
			}
			
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching GMR details"));
//			This API throws 400 if records are not found for any contract item, hence removing exception code
//			List<String> params = new ArrayList<String>();
//			params.add("GMR Details from TRM");
//			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}
		
		return activeGRMList;
	}
	
	public List<GMR> getGMRListFromConnect(String internalContractItemRefNo, List<String> activeGMRList)
			throws PricingException {
		String uri = contractURL + "/data/" + appUUID + "/" + objUUID + "?internalContractItemRefNo="
				+ internalContractItemRefNo;
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		List<GMR> gmrDetails = new ArrayList<GMR>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("GMR details connect fetcher entity: " + entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("GMR details connect fetcher response: " + response));
			JSONArray gmrArr = new JSONArray(response.getBody());
			try {
				for (int i = 0; i < gmrArr.length(); i++) {
					JSONObject gmrObj = gmrArr.optJSONObject(i);
					if (activeGMRList.contains(gmrObj.optString("internalGMRRefNo"))) {
						JSONObject inputObj = new JSONObject(gmrObj.optString("inputPayload"));
						GMR gmr = gson.fromJson(inputObj.toString(), GMR.class);
						gmrDetails.add(gmr);
					}
				}
			} catch (Exception e) {
				logger.error(Logger.EVENT_SUCCESS,
						ESAPI.encoder().encodeForHTML("Exception while parsing GMR details from connect"));
			}

		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching Components"));
			List<String> params = new ArrayList<String>();
			params.add("GMR Details from Connect");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}
		return gmrDetails;
	}
}
