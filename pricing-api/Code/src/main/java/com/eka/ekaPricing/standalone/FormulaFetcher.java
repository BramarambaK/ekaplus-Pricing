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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class FormulaFetcher {
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.pricing.udid}")
	private String pringUDID;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	ContextProvider contextProvider;
	final static  Logger logger = ESAPI.getLogger(FormulaFetcher.class);
	public JSONObject getFormula(ContextProvider context, String formulaID) throws Exception {
		String uri = contractURL + "/data/"+pringUDID+"/Formula/" + formulaID;
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
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTMLAttribute("formula fetcher - entity: "+entity));
			HttpEntity<String> formula = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			String json = formula.getBody().toString();
			json = json.substring(1, json.length() - 1);
//			System.out.println("formula : "+json);
			JSONObject formulaObj = new JSONObject(json);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("formulafetcher - formulaObj: "+formulaObj));
			return formulaObj;
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Exception at "+uri+" : "+e.getMessage()));
			List<String> params = new ArrayList<String>();
			params.add("Formula object");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
			
		}
	}
	
	public List<String> fetchFormulaForModifiedCurveData(List<String> modifiedCurves) {
		List<String> formulaList = new ArrayList<String>();
		String uri = contractURL + "/data/"+pringUDID+"/formula";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-Locale", contextProvider.getCurrentContext().getLocale());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Formula for modified curves - entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONArray formulas = new JSONArray(response.getBody());
			for(int i=0; i<formulas.length(); i++) {
				JSONObject formulaObj = formulas.getJSONObject(i);
				JSONArray includedCurves = formulaObj.optJSONArray("includedCurves");
				for(int j=0; j<includedCurves.length(); j++) {
					String includedCurveName = includedCurves.getString(j);
					if(modifiedCurves.contains(includedCurveName)) {
						formulaList.add(formulaObj.optString("_id"));
						break;
					}
				}
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Exception at "+uri+" for fetchFormulaForModifiedCurveData"),e);
		}
		return formulaList;
	}
}
