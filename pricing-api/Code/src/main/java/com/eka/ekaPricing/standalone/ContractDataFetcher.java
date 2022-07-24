package com.eka.ekaPricing.standalone;

import org.json.JSONArray;
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
import com.eka.ekaPricing.util.CommonValidator;


@Component
public class ContractDataFetcher {

	static String precision;
	final static  Logger logger = ESAPI.getLogger(ContractDataFetcher.class); 

	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.physicals.udid}")
	private String contractUDID;
	@Autowired
	CommonValidator validator;


	public JSONArray getContracts(String token, String tenantID, String contractID) throws Exception {
		String uri = validator.cleanData(contractURL + "/data/"+contractUDID+"/contract/" + contractID);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", validator.cleanData(token));
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", validator.cleanData(tenantID));
		HttpEntity entity = new HttpEntity(headers);
		try {
			HttpEntity<String> contract = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("contracts - entity: "+entity));
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("contracts - response: "+contract));
			precision = new JSONArray(contract.getBody()).getJSONObject(0).getString("pricePrecision");
			JSONArray contractArray = new JSONArray(contract.getBody()).getJSONObject(0).getJSONArray("itemDetails");
			return contractArray;
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri +" : " + e.getMessage()));
			throw new PricingException("Error while fetching contract item object");
		}
	}

	public boolean checkFormulaInContract(String token, String tenantID, String pricingFormulaId) {
		String uri = validator.cleanData(contractURL + "/common/isFormulaExists/"+contractUDID+"/contract/pricingFormulaId/" + pricingFormulaId);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", validator.cleanData(token));
		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", validator.cleanData(tenantID));
		HttpEntity entity = new HttpEntity(headers);
		try {
			HttpEntity<Boolean> result = restTemplate.exchange(uri, HttpMethod.GET, entity, Boolean.class);
//			JSONArray contractArray = new JSONArray(contract.getBody()).getJSONObject(0).getJSONArray("itemDetails");
			return result.getBody().booleanValue();
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri +" : " + e.getMessage()));
		}
		return false;
	}

	public String getPrecision() {
		return precision;
	}
}
