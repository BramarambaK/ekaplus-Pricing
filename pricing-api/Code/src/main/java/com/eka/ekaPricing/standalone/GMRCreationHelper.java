package com.eka.ekaPricing.standalone;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.eka.ekaPricing.pojo.Event;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.GMRQualityDetails;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class GMRCreationHelper {
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Value("${eka.storedGMR.objectUUID}")
	private String objUUID;
	@Value("${eka.GMRConvFact.objectUUID}")
	private String GMRConvUUID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;

	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(GMRCreationHelper.class);

	public void createGMR(ContextProvider context, String internalContractRefNo, String internalContractItemRefNo,
			String internalGMRRefNo, String executedQuantity, String openQuantity, String estimatedPrice,
			String itmeQtyUnit, String payload, String inputPayload)
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
		fieldsToSave.accumulate("internalContractRefNo", internalContractRefNo);
		fieldsToSave.accumulate("internalContractItemRefNo", internalContractItemRefNo);
		fieldsToSave.accumulate("internalGMRRefNo", internalGMRRefNo);
		fieldsToSave.accumulate("executedQuantity", executedQuantity);
		fieldsToSave.accumulate("openQuantity", openQuantity);
		fieldsToSave.accumulate("estimatedPrice", estimatedPrice);
		fieldsToSave.accumulate("qtyUnit", itmeQtyUnit);
		fieldsToSave.accumulate("payload", payload);
		fieldsToSave.accumulate("inputPayload", inputPayload);
		fieldsArr.put(fieldsToSave);
		JSONObject itemListingWithGMR = new JSONObject();
		itemListingWithGMR.put("itemListingWithGMR", fieldsArr);

		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("workflowTaskName", "itemListingWithGMR");
		bodyObj.accumulate("task", "itemListingWithGMR");
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
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving gmr : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(context, "038", new ArrayList<String>()));
		}

	}
	
	public JSONArray fetchGMRData(String internalContractItemRefNo) throws PricingException {

		String uri = validator.cleanData(connectHost + "/data/" + appUUID + "/" + objUUID
				+ "?internalContractItemRefNo=" + internalContractItemRefNo);
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
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchGMRData entity :" + entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchGMRData response :" + response));
			JSONArray objArr = new JSONArray(response.getBody());
			return objArr;
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching GMR Data"), e);
			List<String> params = new ArrayList<String>();
			params.add("GMR data");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}
	}
	
	public void updateGMRWeightedAvgConversionFactor(ContextProvider context, String internalContractItemRefNo,	 String itemQtyUnitId, String itemQtyUnit, 
			List<GMR> gmrList,int index, String itemQtyUnitCheck, Map<String, Double> weightedAvgConversionFactorGMRQty,
			Map<String, GMRQualityDetails> gmrQualityDetails)throws PricingException {
		String uri = connectHost + "/workflow";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());

		JSONObject GMRStockLevelData = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		String eventDate=null;
		for (GMR gmr : gmrList) {
			GMRQualityDetails gmrQuality = gmrQualityDetails.get(gmr.getRefNo());
			JSONObject fieldsToSave = new JSONObject();
			List<Map<String, Object>> stockList = new ArrayList<>();
			fieldsToSave.accumulate("internalContractItemRefNo", internalContractItemRefNo);
			fieldsToSave.accumulate("gmrRefNo", gmr.getRefNo());
			fieldsToSave.accumulate("weightedAvgConFactor", gmrQuality.getActualConversionFactorGMR());
			fieldsToSave.accumulate("weightedAvgConversionFactorGMRQty", weightedAvgConversionFactorGMRQty.get(gmr.getRefNo()));
			fieldsToSave.accumulate("itemQtyUnitId", itemQtyUnitId);
			fieldsToSave.accumulate("curveQtyUnitId", gmrQuality.getCurveQtyUnitId());
			fieldsToSave.accumulate("itemQtyUnit", itemQtyUnit);
			fieldsToSave.accumulate("curveQtyUnit", gmrQuality.getCurveQty());
			fieldsToSave.accumulate("itemQtyUnitCheck", itemQtyUnitCheck);
			fieldsToSave.accumulate("curveQtyUnitCheck", gmrQuality.getCurveQtyUnitCheck());
			String vesselName="";
			if(StringUtils.isEmpty(gmr.getVesselName())) {
				vesselName="Not Available";
			}else {
				vesselName=gmr.getVesselName();
			}
			fieldsToSave.accumulate("vesselName", vesselName);
			for (Stock st : gmr.getStocks()) {
				if (st.getGMRRefNo().equals(gmr.getRefNo())) {
					List<Event> eventList = gmr.getEvent();
					for (Event e : eventList) {
						eventDate = e.getDate();
						
					}
					Map<String, Object> stock = new HashMap<>();
					stock.put("stockPrice", st.getStockPrice());
					stock.put("gmrQty", st.getStockQtyInGmr());
					stock.put("gmrUnit", st.getQtyUnit());
					stock.put("stockRefNo", st.getRefNo());
					stock.put("densityVolumeQtyUnit", st.getDensityVolumeQtyUnitId());
					stock.put("massToVolumeConversionFactor", st.getMassToVolConversionFactor());
					stock.put("densityValue", st.getDensityValue());
					stock.put("massUnit", st.getMassUnitId());
					stock.put("volumeUnit", st.getVolumeUnitId());
					stock.put("eventDate", eventDate);
					stock.put("gmrQtyInItemQty", st.getQty());
					stockList.add(stock);
					
				}
			}
			fieldsToSave.accumulate("stock", stockList);
			fieldsArr.put(fieldsToSave);
		}
		
		GMRStockLevelData.put("GMRStockLevelData", fieldsArr);
		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("workflowTaskName", "GMRStockLevelData");
		bodyObj.accumulate("task", "GMRStockLevelData");
		bodyObj.accumulate("appName", "physicals");
		bodyObj.accumulate("appId", appUUID);
		bodyObj.accumulate("output", GMRStockLevelData);

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("GMR Weighted Conversion Factor entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("GMR Weighted Conversion Factor entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving GMR Weighted Conversion Factor : " + e.getMessage()));
		}

	}
	
	public void deleteGMR(String internalGMRRefNo, String internalContractItemRefNo)
			throws PricingException {
		String uri = validator.cleanData(connectHost + "/data/" +appUUID+"/"+objUUID+"/bulkDelete");
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObj = new JSONObject();
		JSONObject filter1 = new JSONObject();
		JSONObject filter2 = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		filter1.put("fieldName", "internalGMRRefNo");
		filter1.put("value", internalGMRRefNo);
		filter1.put("operator", "eq");
		filter2.put("fieldName", "internalContractItemRefNo");
		filter2.put("value", internalContractItemRefNo);
		filter2.put("operator", "eq");
		fieldsArr.put(filter1);
		fieldsArr.put(filter2);
		
		JSONObject filterData = new JSONObject();
		filterData.put("filter", fieldsArr);
		bodyObj.accumulate("filterData", filterData);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Delete GMR entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Delete GMR  entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for Deleting GMR : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "055", new ArrayList<String>()));
		}

	}
	
	public void deleteGMRConvFactor(String internalGMRRefNo, String internalContractItemRefNo)
			throws PricingException {
		String uri = validator.cleanData(connectHost + "/data/" +appUUID+"/"+GMRConvUUID+"/bulkDelete");
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObj = new JSONObject();
		JSONObject filter1 = new JSONObject();
		JSONObject filter2 = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		filter1.put("fieldName", "gmrRefNo");
		filter1.put("value", internalGMRRefNo);
		filter1.put("operator", "eq");
		filter2.put("fieldName", "internalContractItemRefNo");
		filter2.put("value", internalContractItemRefNo);
		filter2.put("operator", "eq");
		fieldsArr.put(filter1);
		fieldsArr.put(filter2);
		
		JSONObject filterData = new JSONObject();
		filterData.put("filter", fieldsArr);
		bodyObj.accumulate("filterData", filterData);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Delete GMR ConvFactor entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Delete GMR  entity response in GMR ConvFactor: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for Deleting GMR ConvFactor : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "055", new ArrayList<String>()));
		}

	}
	
	  public double getTotalGmrQty(String internalContractItemRefNo,List<GMR> gmrList) throws PricingException {	
		String uri = validator.cleanData(connectHost + "/data/" +appUUID+"/"+GMRConvUUID
				+ "?internalContractItemRefNo=" + internalContractItemRefNo);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObj = new JSONObject();
		JSONObject filter = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		filter.put("fieldName", "internalContractItemRefNo");
		filter.put("value", internalContractItemRefNo);
		filter.put("operator", "eq");
		fieldsArr.put(filter);
		
		JSONObject filterData = new JSONObject();
		filterData.put("filter", fieldsArr);
		bodyObj.accumulate("filterData", filterData);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		Map<String , Double> map = new HashMap<>();
		double totalGMRQty=0;
		List <String> list = new ArrayList<>();
		if(gmrList.size()>0) {
			GMR gmr=gmrList.get(0);
			for (Stock st : gmr.getStocks()) {
				list.add(st.getGMRRefNo());
			}
		}
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("get GMR entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("get GMR  entity response: " + response.getBody()));
			JSONArray objArr = new JSONArray(response.getBody());
			for (int gmrInd = 0; gmrInd < objArr.length(); gmrInd++) {
				JSONObject gmrData = objArr.optJSONObject(gmrInd);
				String gmrRefNo = gmrData.optString("gmrRefNo");
				double convFactor = gmrData.optDouble("weightedAvgConversionFactorGMRQty");
				double stockQtyInItemQty = 0;
				int k = 0;
				JSONArray stockArray = gmrData.optJSONArray("stock");
				if(!list.contains(gmrRefNo)) {
						if (stockArray != null && stockArray.length() > 0) {
							double totalStockQty=0;
							while (k < stockArray.length()) {
								JSONObject stockObj = stockArray.getJSONObject(k);
								double stockQty = stockObj.optDouble("gmrQty", 0l);
								 totalStockQty = totalStockQty + stockQty;
								k++;
							}
							stockQtyInItemQty=totalStockQty* convFactor;
						}
				}
						totalGMRQty=totalGMRQty+stockQtyInItemQty;
	         }
			
			
			
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for get GMR : " + e.getMessage()));
			return totalGMRQty;
		}
		
		return totalGMRQty;
		
	}
}
