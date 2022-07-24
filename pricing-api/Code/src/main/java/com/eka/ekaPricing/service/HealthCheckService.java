package com.eka.ekaPricing.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

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
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PlatformException;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.ContextProvider;
import com.eka.ekaPricing.util.RestTemplateUtil;

@Service
public class HealthCheckService {

	@Autowired
	private ManifestService manifestService;

	@Autowired
	ContextProvider contextProvider;
	
	@Autowired
	private RestTemplateUtil restTemplateUtil;

	@Value("${eka.contract.url}")
	private String connectHost;
	@Value("${collections.url}")
	private String collectionURL;
	@Value("${holiday.DS.name}")
	private String holidayCollectionName;
	@Value("${eka.fx.param}")
	private String fxValue;
	@Value("${eka.fx.param}")
	private String collection;
	@Value("${eka.pricing.collection}")
	private String DSMarketPrice;
	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	@Value("${eka.curveBuilder.uuid}")
	private String appUUID;
	@Value("${eka.curveBuilder.objectUUID}")
	private String objectUDID;
	@Value("${eka.physicals.udid}")
	private String compUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.component.objectUUID}")
	private String objUUID;
	@Value("${eka.pdAdjustment.objectUUID}")
	private String pdaobjUUID;
	@Value("${eka.pricing.udid}")
	private String pringUDID;
	@Value("${eka.pricing.triggerPrice.objectUUID}")
	private String TriobjUUID;
	@Value("${eka.storedGMR.objectUUID}")
	private String GmrobjUUID;
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(HealthCheckService.class);

	public List<Map<String, Object>> execute() {
		
		PricingProperties pricingProps = contextProvider.getCurrentContext().getPricingProperties();
		String clientURL = pricingProps.getPlatform_url();
		String eka_mdm_host = pricingProps.getEka_mdm_host();
		String url= collectionURL;
		String collectionMapper= "/collectionmapper/" + pricingUDID + "/" + pricingUDID +"/";
		String HOLIDAY_DATA = url + holidayCollectionName + "&limit=1";
		String DS_MARKET_PRICE = url+ DSMarketPrice + "&limit=1";
		String DS_EXPIRY_CALENDER =  collectionMapper+"fetchCollectionRecords";
		String UPLOAD_EXPOSURE =  collectionMapper+"addToCollection";
		String MDM_FETCHER = "/mdm/" + pricingUDID + "/data";
		String GMR_DETAILS_FETCHER = "/data/" + appUUID + "/" + GmrobjUUID + "?internalContractItemRefNo=";
		String FETCH_COMPONENT = "/data/" + compUUID + "/" + objUUID + "?internalContractItemRefNo=";
		String CONTRACT_ITEM_FETCHER = "/data/" + compUUID + "/contract?internalContractRefNo=";
		String PDA_ADJUSTMENT_FETCHER = "/data/" + appUUID + "/" + pdaobjUUID + "?internalContractItemRefNo=";
		String TRIGGER_PRICE_FETCHER = "/data/" + pringUDID + "/" + TriobjUUID + "?internalContractItemRefNo=";
		String CURVE_BUILDER = "/data/" + appUUID + "/" + objectUDID;
		String CONTRACT_DATA_FETCHER = "/data/" + compUUID + "/contract/";
		String FORMULA_FETCHER="/data/" + pringUDID + "/Formula/";
		
		List<Map<String, Object>> responseObject = new ArrayList<>();

		checkFormBeat(responseObject, clientURL + HOLIDAY_DATA, "Holiday Data");
		checkPlatFormHeartBeat(responseObject, clientURL + DS_MARKET_PRICE,"DS-Market Prices");
		checkConnectHeartBeat(responseObject, connectHost + DS_EXPIRY_CALENDER,"DS - Expiry Calendar");
		checkUploadExposure(responseObject, connectHost + UPLOAD_EXPOSURE,"Upload Exposure");
		checkFormBeat(responseObject, eka_mdm_host+ MDM_FETCHER, "Pricing MDM");
		checkFormBeat(responseObject, connectHost + GMR_DETAILS_FETCHER, "GMR Details Fetcher");
		checkFormBeat(responseObject, connectHost + FETCH_COMPONENT, "Fetch Component");
		checkFormBeat(responseObject, connectHost + CONTRACT_ITEM_FETCHER, "Contract Item Fetcher");
		checkFormBeat(responseObject, connectHost + PDA_ADJUSTMENT_FETCHER, "PDA Adjustment Fetcher");
		checkFormBeat(responseObject, connectHost + TRIGGER_PRICE_FETCHER, "Trigger price Fetcher");
		checkFormBeat(responseObject, connectHost + CURVE_BUILDER, "Curve Builder");
		checkFormBeat(responseObject, connectHost + CONTRACT_DATA_FETCHER, "Contract Data Fetcher");
		checkFormBeat(responseObject, connectHost + FORMULA_FETCHER, "Formula Fetcher");

		getVersionAndManifestInfo(responseObject);
		return responseObject;

	}

	private void getVersionAndManifestInfo(List<Map<String, Object>> responseObject) {

		Map<String, Object> returnObject = new HashMap<>();
		returnObject.put("targetType", "Pricing Service");
		returnObject.put("target", "Pricing API Version");
		returnObject.put("status", "success");
		Map<String, Object> returnObject1 = new HashMap<>();

		try {
			Attributes manifestAttributes = manifestService.getManifestAttributes();
			returnObject.put("version", manifestAttributes.getValue("Manifest-Version"));

			responseObject.add(returnObject);


			returnObject1.put("targetType", "Pricing Service");
			returnObject1.put("target", "Pricing API Manifest Info");
			returnObject1.put("status", "success");

			returnObject1.put("envDetails", manifestAttributes);

			responseObject.add(returnObject1);
		} catch (Exception e) {

			logger.error(Logger.EVENT_FAILURE, "error occured while fetching manifest information", e);
			returnObject.put("status", "Fail");
			returnObject1.put("status", "success");

			returnObject.put("statusMessage", "error occured while fetching manifest information");
			responseObject.add(returnObject);


		}

	}

	private void checkPlatFormHeartBeat(List<Map<String, Object>> responseObject, String uri, String target) {
      
      Map<String, Object> returnObject = new HashMap<>();
      returnObject.put("targetType", "Pricing Service");
	  returnObject.put("target", target);
	 
      try {
    	  JSONObject outerObj = new JSONObject();
      RestTemplate restTemplate = new RestTemplate();
		outerObj.put("skip", "0");
		outerObj.put("limit", "1");
		outerObj.put("collectionName", target);
		HttpHeaders httpHeaders = restTemplateUtil.getCommonHttpHeaders();
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), httpHeaders);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		returnObject.put("status", "Success");
      }
		 catch (PlatformException e) {
			 returnObject.put("status", "Failed");
			 returnObject.put("statusMessage", uri + "is not a valid url. " + e.getMessage());
			} catch (ResourceAccessException e) {
				returnObject.put("status", "Failed");
				returnObject.put("statusMessage", uri + " is not reachable;" + e.getMessage());

			} 

			catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, " ping failed for " + uri, e);
				returnObject.put("status", "Failed");
				returnObject.put("statusMessage", " Exception occured while accessing [" + uri + "]." + e.getMessage());

			}
      responseObject.add(returnObject);
	}
	
	private void checkConnectHeartBeat(List<Map<String, Object>> responseObject, String uri, String target) {
	      Map<String, Object> returnObject = new HashMap<>();
	      returnObject.put("targetType", "Pricing Service");
		  returnObject.put("target", target);
		 
	      try {
	    	  JSONObject outerObj = new JSONObject();
	  		outerObj.put("skip", "0");
	  		outerObj.put("limit", "1");
	  		outerObj.put("collectionName", target);
	  		RestTemplate restTemplate = new RestTemplate();
	  		HttpHeaders headers =  restTemplateUtil.getCommonHttpHeaders();
	  		headers.add("ttl", "100");
	  		JSONArray expiryArray = new JSONArray();
	  		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
	  		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("expiry fetcher major- entity: " + entity));
	  		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			returnObject.put("status", "Success");
	      }
			 catch (PlatformException e) {
				 returnObject.put("status", "Failed");
				 returnObject.put("statusMessage", uri + "is not a valid url. " + e.getMessage());
				} catch (ResourceAccessException e) {
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", uri + " is not reachable;" + e.getMessage());

				} 

				catch (Exception e) {
					logger.error(Logger.EVENT_FAILURE, " ping failed for " + uri, e);
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", " Exception occured while accessing [" + uri + "]." + e.getMessage());

				}
	      responseObject.add(returnObject);
		}
	
	private void checkUploadExposure(List<Map<String, Object>> responseObject, String uri, String target) {
	      
	      Map<String, Object> returnObject = new HashMap<>();
	      returnObject.put("targetType", "Pricing Service");
		  returnObject.put("target", target);
		 
	      try {
	    	  JSONObject bodyObject = new JSONObject();
	      RestTemplate restTemplate = new RestTemplate();
	      bodyObject.put("collectionConnectMapProperty", "pricingexposure_collectionConnectMap");
			bodyObject.put("collectionHeaderProperty", "pricingexposure_collectionHeader");
			bodyObject.put("collectionName", "Liquids_Online_Exposure");
			HttpHeaders httpHeaders = restTemplateUtil.getCommonHttpHeaders();
			HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), httpHeaders);
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			returnObject.put("status", "Success");
	      }
			 catch (PlatformException e) {
				 returnObject.put("status", "Failed");
				 returnObject.put("statusMessage", uri + "is not a valid url. " + e.getMessage());
				} catch (ResourceAccessException e) {
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", uri + " is not reachable;" + e.getMessage());

				} 

				catch (Exception e) {
					logger.error(Logger.EVENT_FAILURE, " ping failed for " + uri, e);
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", " Exception occured while accessing [" + uri + "]." + e.getMessage());

				}
	      responseObject.add(returnObject);
		}
	
	private void checkFormBeat(List<Map<String, Object>> responseObject, String uri, String target) {
	      
	      Map<String, Object> returnObject = new HashMap<>();
	      returnObject.put("targetType", "Pricing Service");
		  returnObject.put("target", target);
		 
	      try {
	        RestTemplate restTemplate = new RestTemplate();
			HttpHeaders httpHeaders = restTemplateUtil.getCommonHttpHeaders();
			HttpEntity<String> entity = new HttpEntity<String>(httpHeaders);
			HttpEntity<String> formula = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			returnObject.put("status", "Success");
	      }
			 catch (PlatformException e) {
				 returnObject.put("status", "Failed");
				 returnObject.put("statusMessage", uri + "is not a valid url. " + e.getMessage());
				} catch (ResourceAccessException e) {
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", uri + " is not reachable;" + e.getMessage());

				} 

				catch (Exception e) {
					logger.error(Logger.EVENT_FAILURE, " ping failed for " + uri, e);
					returnObject.put("status", "Failed");
					returnObject.put("statusMessage", " Exception occured while accessing [" + uri + "]." + e.getMessage());

				}
	      responseObject.add(returnObject);
		}
	
	
}
