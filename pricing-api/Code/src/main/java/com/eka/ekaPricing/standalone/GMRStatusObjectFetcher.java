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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class GMRStatusObjectFetcher {
	final static Logger logger = ESAPI.getLogger(GMRStatusObjectFetcher.class);
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.GMRStatus.objectUUID}")
	private String objUUID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;

	Gson gson = new Gson();

	public List<GMRStatusObject> fetchGMRStatusObject(String internalContractItemRefNo) throws PricingException {
		String uri = validator.cleanData(contractURL + "/data/" + appUUID + "/" + objUUID
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
		List<GMRStatusObject> gmrStatusObjectList = new ArrayList<GMRStatusObject>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchGMRStatusObject entity :" + entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchGMRStatusObject response :" + response));
			JSONArray objArr = new JSONArray(response.getBody());
			for (int i = 0; i < objArr.length(); i++) {
				GMRStatusObject gmrStatusObject = gson.fromJson(objArr.getJSONObject(i).toString(),
						GMRStatusObject.class);
				gmrStatusObjectList.add(gmrStatusObject);

			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching GMRStatusObject"),e);
			List<String> params = new ArrayList<String>();
			params.add("GMRStatusObject");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}

		return gmrStatusObjectList;
	}
}
