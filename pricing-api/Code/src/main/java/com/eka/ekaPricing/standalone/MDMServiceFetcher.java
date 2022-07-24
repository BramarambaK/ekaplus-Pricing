package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
//import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;

import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class MDMServiceFetcher {
	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	@Value("${eka.physicals.udid}")
	private String physicalUDID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	FormulaeCalculator formulaeCal;
	@Autowired
	CommonValidator validator;
	
	final static Logger logger = ESAPI.getLogger(MDMServiceFetcher.class);

	public double getQtyUnitConversionRate(ContextProvider context, String productID, String fromUnit, String toUnit)
			throws Exception {
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/" + pricingUDID + "/data";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObject = new JSONObject();
		JSONArray bodyArr = new JSONArray();
		bodyObject.put("serviceKey", "quantityConversionFactor");
		String[] dependsArr = new String[3];
		dependsArr[0] = productID;
		dependsArr[1] = fromUnit;
		dependsArr[2] = toUnit;
		bodyObject.put("dependsOn", dependsArr);
		bodyArr.put(bodyObject);
		HttpEntity<String> entity = new HttpEntity<String>(bodyArr.toString(), headers);
		String cacheKey = contextProvider.getCurrentContext().getTenantID() + "-" + productID + "-" + fromUnit + "-"
				+ toUnit;
		Object cachedValue = formulaeCal.retrieveCacheStored(cacheKey, "3600");
		if (null == cachedValue) {
			try {
				HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher - entity: "+entity));
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher - response: "+response));
				JSONArray quantityConversionArray = new JSONObject(response.getBody())
						.getJSONArray("quantityConversionFactor");
				double conversionFactor = quantityConversionArray.getJSONObject(0).optDouble("value");
				boolean isCachingSuccess = formulaeCal.storeCache(cacheKey, Double.toString(conversionFactor), "3600");
				if (!isCachingSuccess) {
					logger.error(Logger.EVENT_FAILURE,
							ESAPI.encoder().encodeForHTML("QTY conversion not stored for : " + fromUnit + " & " +toUnit));
				}
				return quantityConversionArray.getJSONObject(0).optDouble("value");
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
			}
			return 1;
		}
		else {
			try {
				return Double.parseDouble(cachedValue.toString());
			}
			catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
				return 1;
			}
		}
		
	}

	public double getCurrencyUnitConversionRate(ContextProvider context, String unit) throws Exception {
//		System.out.println("latency 11: "+LocalDateTime.now());
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/" + pricingUDID + "/currency-details";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("currencyCode", unit);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), headers);
		String cacheKey = contextProvider.getCurrentContext().getTenantID() + "-" + unit;
		Object cachedValue = formulaeCal.retrieveCacheStored(cacheKey, "3600");
		if (null == cachedValue) {
			try {
				HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher currency conversion- entity: "+entity));
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher currency conversion- response: "+response));
				JSONObject responsebody = new JSONObject(response.getBody());
				boolean isCachingSuccess = formulaeCal.storeCache(cacheKey, responsebody.toString(), "3600");
				if (!isCachingSuccess) {
					logger.error(Logger.EVENT_FAILURE,
							ESAPI.encoder().encodeForHTML("subcurrency cache not stored for : " + unit));
				}
				if(responsebody.optString("isSubCurrency").equalsIgnoreCase("Y")) {
					return responsebody.optDouble("conversionFactor");
				}
				else {
					return 1;
				}
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
				throw new PricingException(
						messageFetcher.fetchErrorMessage(context, "043", new ArrayList<String>()));
				
			}
		}
		else {
			String cahedObjectString = validator.cleanData(cachedValue.toString());
			JSONObject cahedJSON = new JSONObject(cahedObjectString);
			if(cahedJSON.optString("isSubCurrency").equalsIgnoreCase("Y")) {
				return cahedJSON.optDouble("conversionFactor");
			}
			else {
				return 1;
			}
		}
		
	}

	public String getCurrencyKey(ContextProvider context, String currency, String qty, String productID)
			throws Exception {
		Map<String, String> currKeyValMap = contextProvider.getCurrentContext().getCurrKeyValMap();
		if (null == currKeyValMap || currKeyValMap.size() == 0) {
			populateMDMData(productID);
			currKeyValMap = contextProvider.getCurrentContext().getCurrKeyValMap();
		}
		if (currKeyValMap.containsKey(currency)) {
			return currKeyValMap.get(currency);
		}
		return null;
//		throw new PricingException("Currency not available in the system : " + currency);
	}

	public String getQuantityKey(ContextProvider context, String qty, String productID) throws Exception {
		Map<String, String> qtyKeyMapper = contextProvider.getCurrentContext().getQtyKeyMapper();
		if(null == qtyKeyMapper || qtyKeyMapper.size()==0) {
			populateMDMData(productID);
			qtyKeyMapper = contextProvider.getCurrentContext().getQtyKeyMapper();
		}
		if (qtyKeyMapper.containsKey(qty)) {
			return qtyKeyMapper.get(qty);
		}
		return null;
		
	}
	
	public String getContractQty(ContextProvider context, String qtyUnitID, String productID) throws Exception {
		Map<String, String> qtyKeyMapper = contextProvider.getCurrentContext().getQtyKeyMapper();
		if(null == qtyKeyMapper || qtyKeyMapper.size()==0) {
			populateMDMData(productID);
			qtyKeyMapper = contextProvider.getCurrentContext().getQtyKeyMapper();
		}
		if(qtyKeyMapper.size()==0 || !qtyKeyMapper.containsValue("qtyUnitID")) {
			try {
				getQuantityKey(context, qtyUnitID, productID);	
			}
			catch (Exception e) {
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("populated quantity key values"));
			}
		}
		for(Map.Entry<String, String> entity : qtyKeyMapper.entrySet()) {
			if(entity.getValue().equals(qtyUnitID)) {
				return entity.getKey();
			}
		}
		return "";
	}
	
	public String getProductValue(ContextProvider context, String productId) throws Exception {
		Map<String, String> productKeyValMap = contextProvider.getCurrentContext().getProductKeyValMap();
		if (null == productKeyValMap || productKeyValMap.size() == 0) {
			populateMDMData(productId);
			productKeyValMap = contextProvider.getCurrentContext().getProductKeyValMap();
		}
		if(productKeyValMap.containsKey(productId)){
			return productKeyValMap.get(productId);
		}
		return "";
	}
	
	public String getPriceUnitValue(String productId, String priceUnitId) throws Exception {
		Map<String, String> priceUnitMap = contextProvider.getCurrentContext().getPriceUnitMap();
		if (null == priceUnitMap || priceUnitMap.size() == 0) {
			populateMDMData(productId);
			priceUnitMap = contextProvider.getCurrentContext().getPriceUnitMap();
		}
		if(priceUnitMap.containsKey(priceUnitId)){
			return priceUnitMap.get(priceUnitId);
		}
		throw new Exception("price Unit not available in the system : " + priceUnitId);
	}
	
	public void populateMDMData(String productID) throws Exception {


		if (null != contextProvider.getCurrentContext().getQtyKeyMapper()
				&& !contextProvider.getCurrentContext().getQtyKeyMapper().isEmpty()) {
			return;
		}
		Map<String, String> qtyKeyMapper = new HashMap<String, String>();
		Map<String, String> currKeyValMap = new HashMap<String, String>();
		Map<String, String> productKeyValMap = new HashMap<String, String>();
		Map<String, String> priceUnitMap = new HashMap<String, String>();
		Map<String, String> qualityUnitMap = new HashMap<String, String>();
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
//		System.out.println("latency 1: "+LocalDateTime.now());
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/" + pricingUDID + "/data";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONArray bodyArr = new JSONArray();
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("serviceKey", "physicalproductquantitylist");
		String[] dependsArr = new String[1];
		bodyObject.put("dependsOn", dependsArr);
		JSONObject currBodyObject = new JSONObject();
		currBodyObject.put("serviceKey", "productCurrencyList");
		JSONObject productComboObject = new JSONObject();
		productComboObject.put("serviceKey", "productComboDropDrown");
		JSONObject priceUnitObject = new JSONObject();
		String[] priceUnitDependsArr = new String[1];
		priceUnitDependsArr[0] = productID;
		priceUnitObject.put("serviceKey", "productPriceUnit");
		priceUnitObject.put("dependsOn", priceUnitDependsArr);
		JSONObject qualityUnitObject = new JSONObject();
		String[] qualityUnitDependsArr = new String[1];
		qualityUnitDependsArr[0] = productID;
		qualityUnitObject.put("serviceKey", "qualityComboDropDrown");
		qualityUnitObject.put("dependsOn", qualityUnitDependsArr);
		bodyArr.put(bodyObject);
		bodyArr.put(currBodyObject);
		bodyArr.put(productComboObject);
		bodyArr.put(priceUnitObject);
		bodyArr.put(qualityUnitObject);
		dependsArr[0] = productID;
		String cacheKey = bodyArr.toString() + "-" + contextProvider.getCurrentContext().getTenantID();
		Object cachedObject = formulaeCal.retrieveCacheStored(cacheKey, "3600");
		String response = null;
		if(null == cachedObject) {
			HttpEntity<String> entity = new HttpEntity<String>(bodyArr.toString(), headers);
			try {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("MDM fetcher -qty key entity: " + entity));
				HttpEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				response = responseEntity.getBody();
				if(validatePopulateMDMResponse(response) && formulaeCal.storeCache(cacheKey, response, "3600")) {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call success"));
				}
				else {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call unsuccessful"));
				}
				logger.error(Logger.EVENT_FAILURE,
						ESAPI.encoder().encodeForHTML("MDM fetcher -qty key response: " + response));
				
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML(
						"exception at " + uri + " : " + e.getMessage() + " for product Id: " + productID), e);
				throw new PricingException(
						messageFetcher.fetchErrorMessage(contextProvider, "008", new ArrayList<String>()));
			}
		}
		else {
			response  = validator.cleanData(cachedObject.toString());
		}
		
		try {
			JSONArray currArr = new JSONObject(response).getJSONArray("productCurrencyList");
			for (int i = 0; i < currArr.length(); i++) {
				JSONObject jObj = currArr.getJSONObject(i);
				currKeyValMap.put(jObj.optString("value"), jObj.optString("key"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API - ",e);
			throw new Exception("MDM call Failing for productCurrencyList");
		}
		try {
			JSONArray qtyArr = new JSONObject(response).getJSONArray("physicalproductquantitylist");
			for (int i = 0; i < qtyArr.length(); i++) {
				JSONObject jObj = qtyArr.getJSONObject(i);
				qtyKeyMapper.put(jObj.optString("value"), jObj.optString("key"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API - ",e);
			throw new Exception("MDM call Failing for physicalproductquantitylist");
		}
		try {
			JSONArray prodctArr = new JSONObject(response).getJSONArray("productComboDropDrown");
			for (int i = 0; i < prodctArr.length(); i++) {
				JSONObject jObj = prodctArr.getJSONObject(i);
				productKeyValMap.put(jObj.optString("key"), jObj.optString("value"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API - ",e);
		}
		try {
			JSONArray priceunitArr = new JSONObject(response).getJSONArray("productPriceUnit");
			for (int i = 0; i < priceunitArr.length(); i++) {
				JSONObject jObj = priceunitArr.getJSONObject(i);
				priceUnitMap.put(jObj.optString("key"), jObj.optString("value"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API - ",e);
			throw new Exception("MDM call Failing for productPriceUnit");
		}
		try {
			JSONArray qualityArr = new JSONObject(response).getJSONArray("qualityComboDropDrown");
			for (int i = 0; i < qualityArr.length(); i++) {
				JSONObject jObj = qualityArr.getJSONObject(i);
				qualityUnitMap.put(jObj.optString("key"), jObj.optString("value"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API for quality- ",e);
		}
		if (!currKeyValMap.isEmpty()) {
			contextProvider.getCurrentContext().setCurrKeyValMap(currKeyValMap);
		}
		if (!qtyKeyMapper.isEmpty()) {
			contextProvider.getCurrentContext().setQtyKeyMapper(qtyKeyMapper);
		}
		if (!productKeyValMap.isEmpty()) {
			contextProvider.getCurrentContext().setProductKeyValMap(productKeyValMap);
		}
		if (!priceUnitMap.isEmpty()) {
			contextProvider.getCurrentContext().setPriceUnitMap(priceUnitMap);
		}
		if (!qualityUnitMap.isEmpty()) {
			contextProvider.getCurrentContext().setQualityUnitMap(qualityUnitMap);
		}
	}
	
	
	public String[] getQualityExchangeUnit(ContextProvider context, String qualityId) throws Exception {
		String[] resArr = new String[6];
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/masterdatas/" + physicalUDID + "/qualityexchange";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("qualityId", qualityId);
		String cacheKey = bodyObject.toString() + "-" + contextProvider.getCurrentContext().getTenantID();
		Object cachedObject = formulaeCal.retrieveCacheStored(cacheKey, "3600");
		String response = null;
		if(null == cachedObject) {
			HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), headers);
		
			try {
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher Quality Exchange Unit: "+entity));
				HttpEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher Quality Exchange Unit- response: "+response));
				response=responseEntity.getBody();
				if(validateQualityExchangeMDMResponse(response) && formulaeCal.storeCache(cacheKey, response, "3600")) {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call for Quality Exchange Unit success"));
				}
				else {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call for Quality Exchange Unit unsuccessful"));
				}
				
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
				resArr[0] = "";
				resArr[1] = "";
				resArr[2]= "";
				resArr[3]= "";
				resArr[4]= "Failed";
				resArr[5]= "Unable to get Quality Exchange Unit";
				
		  }
		}else {
			response  = validator.cleanData(cachedObject.toString());
		}
		try {
			JSONObject responsebody = new JSONObject(response);
			resArr[0] = responsebody.optString("instrument");
			resArr[1] = responsebody.optString("valuationPriceUnit");
			resArr[2]= responsebody.optString("instrumentType");
			resArr[3]= responsebody.optString("exchangeName");
			resArr[4]= "Success";
			resArr[5]= "";
			return resArr;
		
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
			resArr[0] = "";
			resArr[1] = "";
			resArr[2]= "";
			resArr[3]= "";
			resArr[4]= "Failed";
			resArr[5]= "Unable to get Quality Exchange Unit";
			return resArr;
		}
  }
	
	public String[] getBaseQtyUnit(ContextProvider context, String productId) throws Exception {
		String[] resArr = new String[4];
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/masterdatas/" + physicalUDID + "/baseQuantity";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("productId", productId);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), headers);
		
			try {
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher Base Quantity Unit: "+entity));
				HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher Base Quantity- response: "+response));
				JSONObject responsebody = new JSONObject(response.getBody());
				resArr[0] = responsebody.optString("baseQuantityUnit");
				resArr[1] = "Success";
				resArr[2] = "";
				return resArr;
				
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
				resArr[0] = "";
				resArr[1] = "Failed";
				resArr[2] = "Unable to get Base Quantity Unit";
			}
			return resArr;
		
	}
	public String getQualityUnitValue(String productId, String qualityUnitId) throws Exception {
		Map<String, String> qualityUnitMap = contextProvider.getCurrentContext().getQualityUnitMap();
		if (null == qualityUnitMap || qualityUnitMap.size() == 0) {
			populateMDMData(productId);
			qualityUnitMap = contextProvider.getCurrentContext().getPriceUnitMap();
		}
		if(qualityUnitMap.containsKey(qualityUnitId)){
			return qualityUnitMap.get(qualityUnitId);
		}
		throw new Exception("quality Unit not available in the system : " + qualityUnitId+" for the Product Id" +productId);
	}
	
	public String getQualityUnitId(String productId, String qualityUnitName) throws Exception {
		Map<String, String> qualityUnitMap = contextProvider.getCurrentContext().getQualityUnitMap();
		if (null == qualityUnitMap || qualityUnitMap.size() == 0) {
			populateMDMData(productId);
			qualityUnitMap = contextProvider.getCurrentContext().getPriceUnitMap();
		}
		if(qualityUnitMap.containsValue(qualityUnitName)){
		
		for(Entry<String, String> entry: qualityUnitMap.entrySet()) {

		      if(entry.getValue().equalsIgnoreCase(qualityUnitName)) {
		    	  return entry.getKey();
		      }
		    }
		  
		}
		return "";
	}
	
	public Map<String, String> populateMDMDataForMassVol(String productID,String type) throws Exception {

		Map<String, String> massVolKeyMapper = new HashMap<String, String>();
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/" + pricingUDID + "/data";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONArray bodyArr = new JSONArray();
		JSONObject massVolUnitObject = new JSONObject();
		String[] massVolUnitDependsArr = new String[2];
		massVolUnitDependsArr[0] = productID;
		massVolUnitDependsArr[1] = type;
		massVolUnitObject.put("serviceKey", "productQtyUnitByType");
		massVolUnitObject.put("dependsOn", massVolUnitDependsArr);
		bodyArr.put(massVolUnitObject);
		String cacheKey = bodyArr.toString() + "-" + contextProvider.getCurrentContext().getTenantID();
		Object cachedObject = formulaeCal.retrieveCacheStored(cacheKey, "3600");
		String response = null;
		if(null == cachedObject) {
			HttpEntity<String> entity = new HttpEntity<String>(bodyArr.toString(), headers);
			try {
				logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher -mass volume key entity: " + entity));
				HttpEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				response = responseEntity.getBody();
				
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher -mass volume key response: "+response));
				if(formulaeCal.storeCache(cacheKey, response, "3600")) {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call success"));
				}
				else {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("caching of MDM call unsuccessful"));
				}
				logger.error(Logger.EVENT_FAILURE,
						ESAPI.encoder().encodeForHTML("MDM fetcher -qty key response: " + response));
				
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML(
						"exception at " + uri + " : " + e.getMessage() + " for product Id: " + productID), e);
				massVolKeyMapper.put("error", "MDM call Failing for productQtyUnitByType for Mass and Volume");
				return massVolKeyMapper;
			}
		}else {
			response  = validator.cleanData(cachedObject.toString());
		}
		
		try {
			JSONArray currArr = new JSONObject(response).getJSONArray("productQtyUnitByType");
			for (int i = 0; i < currArr.length(); i++) {
				JSONObject jObj = currArr.getJSONObject(i);
				massVolKeyMapper.put(jObj.optString("value"), jObj.optString("key"));
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at MDM API - ",e);
			massVolKeyMapper.put("error", "MDM call Failing for productQtyUnitByType for Mass and Volume");
			return massVolKeyMapper;
		}
		return massVolKeyMapper;
	}
	
	public String[] getIncotermDetails(String incotermId) throws Exception {
		String[] resArr = new String[3];
		PricingProperties pricingProperties = contextProvider.getCurrentContext().getPricingProperties();
		String mdmURL = pricingProperties.getEka_mdm_host();
		String uri = mdmURL + "/mdm/masterdatas/" + physicalUDID + "/incoterm";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("incoTermId", incotermId);
		HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), headers);
		
			try {
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher incoTermId : "+entity));
				HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("MDM fetcher incoTermId- response: "+response));
				JSONObject responsebody = new JSONObject(response.getBody());
				resArr[0] = responsebody.optString("locationField");
				resArr[1]= "Success";
				resArr[2]= "";
				return resArr;
				
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()),e);
				resArr[0] = "";
				resArr[1]= "Failed";
				resArr[2]= "Unable to get Incoterm Details";
				
			}
			return resArr;
		
	}
	
	public boolean validatePopulateMDMResponse(String response) {
		JSONObject responseObj = new JSONObject(response);
		if (responseObj.has("productCurrencyList") && responseObj.has("physicalproductquantitylist")
				&& responseObj.has("productComboDropDrown") && responseObj.has("productPriceUnit")
				&& responseObj.has("qualityComboDropDrown")) {
			return true;
		}
		
		return false;
	}
	public boolean validateQualityExchangeMDMResponse(String response) {
		JSONObject responseObj = new JSONObject(response);
		if (responseObj.has("instrument")) {
			return true;
		}
		
		return false;
	}
}
