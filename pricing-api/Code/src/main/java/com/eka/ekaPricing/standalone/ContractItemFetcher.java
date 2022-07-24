package com.eka.ekaPricing.standalone;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ContractItemFetcher {
	final static Logger logger = ESAPI.getLogger(ContractItemFetcher.class);
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.physicals.udid}")
	private String contractUDID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;
	@Autowired
	RestTemplateGetRequestBodyFactory restTemplateGetWityBody;
	@Autowired
	MDMServiceFetcher mdmFetcher;

	public JSONArray getItemForContract(ContextProvider context, String contractID, Contract con) throws Exception {
		String uri = validator.cleanData(contractURL + "/data/"+contractUDID+"/contract?internalContractRefNo=" + contractID);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity entity = new HttpEntity(headers);

		try {
			HttpEntity<String> contract = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			con.setContractRefNo(new JSONArray(contract.getBody()).getJSONObject(0).optString("contractRefNo"));
			con.setContractDraftId(new JSONArray(contract.getBody()).getJSONObject(0).optString("_id"));
			con.setContractType(new JSONArray(contract.getBody()).getJSONObject(0).optString("contractType"));
			con.setIncoTermId(new JSONArray(contract.getBody()).getJSONObject(0).optString("incotermId"));
			String generalDisplayValue = new JSONArray(contract.getBody()).getJSONObject(0).optString("generalDetailsDisplayValue");
			JSONObject object =null;
			if(null!=generalDisplayValue && !generalDisplayValue.isEmpty()) {
				 object = new JSONObject(generalDisplayValue);
				 con.setIncoTermName(object.optString("incotermIdDisplayName"));
			}
			JSONArray contractArray = new JSONArray(contract.getBody()).getJSONObject(0).getJSONArray("itemDetails");
			return contractArray;
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Exception while fetching Contract Items at "+uri));
		}
		return null;
	}
	
	public List<String> getContractsForProvidedFormula(List<String> formulaList) {
		List<String> internalContractItemRefNoList = new ArrayList<String>();
		String uri = validator.cleanData(contractURL + "/data/" + contractUDID + "/contract");
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = restTemplateGetWityBody.getRestTemplate();
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
		Map<Object, List<String>> groups = formulaList.stream().collect(Collectors.groupingBy(s -> 20));
		List<List<String>> subSets = new ArrayList<List<String>>(groups.values());
		for (int index = 0; index < subSets.size(); index++) {
			JSONObject body = new JSONObject();
			JSONObject filterObj = new JSONObject();
			JSONArray filterArray = new JSONArray();
			JSONObject filter1 = new JSONObject();
			filter1.put("fieldName", "itemDetails.pricing.pricingFormulaId");
			filter1.put("value", formulaList);
			filter1.put("operator", "in");
			filterArray.put(filter1);
			filterObj.put("filter", filterArray);
			body.put("filterData", filterObj);
			HttpEntity<String> entity = new HttpEntity<String>(body.toString(), headers);
			try {
				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("checking time : " + LocalDateTime.now()));
				logger.info(Logger.EVENT_SUCCESS,
						ESAPI.encoder().encodeForHTML("contracts fetcher for formulaList: " + entity));
				HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
				JSONArray contracts = new JSONArray(response.getBody());
				for (int i = 0; i < contracts.length(); i++) {
					JSONObject contractObj = contracts.getJSONObject(i);
					String internalContractRefNo = contractObj.optString("internalContractRefNo");
					internalContractItemRefNoList.add(internalContractRefNo);
				}
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE,
						ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()), e);
			}
		}
		
		return internalContractItemRefNoList;
	}
	
	public JSONObject getContractForRef(String contractRef) throws Exception {
		String uri = validator.cleanData(contractURL + "/data/"+contractUDID+"/contract?internalContractRefNo=" + contractRef);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(headers);

		try {
			HttpEntity<String> contract = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONObject contractObject = new JSONArray(contract.getBody()).getJSONObject(0);
			return contractObject;
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Exception while fetching Contract for "+contractRef));
		}
		return null;
	}
	
	public void getItemForContractDensity(JSONArray itemArray, String internalContractRefNo, Contract con) throws Exception {
	
		try {
			for (int k = 0; k < itemArray.length(); k++) {
				JSONObject contractItem = itemArray.optJSONObject(k);
				String internalRefNo=contractItem.optString("internalItemRefNo");
				if(internalRefNo.contains(internalContractRefNo)) {
					String dailyObj = contractItem.optString("itemDisplayValue");
				if(null!=dailyObj && !dailyObj.isEmpty()) {
					JSONObject object = new JSONObject(dailyObj);  
					con.setQualityName(object.optString("qualityDisplayName"));
					con.setProfitCenter(object.optString("profitCenterIdDisplayName"));
					con.setStrategy(object.optString("strategyAccIdDisplayName"));
					String[] locationFieldArr= mdmFetcher.getIncotermDetails(con.getIncoTermId());
					String locationField=locationFieldArr[0];
					if(locationField.equalsIgnoreCase("ORIGINATION")) {
						con.setLocationName(object.optString("originationCityIdDisplayName"));
					}else {
						con.setLocationName(object.optString("destinationCityIdDisplayName"));
					}
				}
					Object contractQualityDensity=contractItem.get("densityFactor");
					if(!contractQualityDensity.equals(null)) {
						con.setContractQualityDensity(contractItem.optDouble("densityFactor"));
						con.setContractQualityMassUnit(contractItem.optString("densityMassQtyUnitId"));
						con.setContractQualityVolumeUnit(contractItem.optString("densityVolumeQtyUnitId"));
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("densityFactor : " + contractItem.optDouble("densityFactor")));
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("densityMassQtyUnitId : " + contractItem.optString("densityMassQtyUnitId")));
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("densityVolumeQtyUnitId : " + contractItem.optString("densityVolumeQtyUnitId")));
					}
					
				}
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Exception while fetching Contract Items at Density level "));
		}
	}
}
