package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.eka.ekaPricing.pojo.PDRule;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class PDRuleFetcher {
	final static Logger logger = ESAPI.getLogger(ComponentFetcher.class);
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.pdRule.ObjectUUID}")
	private String objUUID;
	@Autowired
	CommonValidator validator;
	@Autowired
	ContextProvider contextProvider;

	Gson gson = new Gson();

	public Map<String, PDRule> fetchPDRules(ContextProvider context, String internalContractItemRefNo, String pdRuleName)
			throws PricingException {
		Map<String, PDRule> ruleMap = new HashMap<String, PDRule>();
		String uri = validator.cleanData(contractURL + "/data/" + appUUID + "/" + objUUID + "?internalContractItemRefNo="
				+ internalContractItemRefNo + "&pdRuleName=" + pdRuleName);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		List<PDRule> ruleList = new ArrayList<PDRule>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONArray adjArr = new JSONArray(response.getBody());
			for (int i = 0; i < adjArr.length(); i++) {
				PDRule adjustment = gson.fromJson(adjArr.getJSONObject(i).toString(), PDRule.class);
				ruleList.add(adjustment);
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching PD Rule"));
			throw new PricingException("Error while fetching components");
		}
		for (PDRule rule : ruleList) {
			ruleMap.put(rule.getPdRuleName() + " " + rule.getProductAttribute(), rule);
		}
		return ruleMap;
	}
}
