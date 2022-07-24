package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

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
import com.eka.ekaPricing.pojo.PDAdjustment;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class PDAdjustmentFetcher {
	final static Logger logger = ESAPI.getLogger(ComponentFetcher.class);
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.pdAdjustment.objectUUID}")
	private String objUUID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	CommonValidator validator;

	Gson gson = new Gson();

	public List<PDAdjustment> fetchAdjustments(ContextProvider context, String internalContractItemRefNo)
			throws PricingException {
		String uri = validator.cleanData(contractURL + "/data/" + appUUID + "/" + objUUID + "?internalContractItemRefNo="
				+ internalContractItemRefNo);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		List<PDAdjustment> adjustMentList = new ArrayList<PDAdjustment>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONArray adjArr = new JSONArray(response.getBody());
			for (int i = 0; i < adjArr.length(); i++) {
				PDAdjustment adjustment = gson.fromJson(adjArr.getJSONObject(i).toString(), PDAdjustment.class);
				adjustMentList.add(adjustment);
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching PD Adjustments"));
			//throw new PricingException("Error while fetching components");
			List<String> params = new ArrayList<String>();
			params.add("PD Adjustments");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}

		return adjustMentList;
	}
}
