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
import com.eka.ekaPricing.pojo.PricingComponent;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class ComponentFetcher {
	final static Logger logger = ESAPI.getLogger(ComponentFetcher.class);
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.component.objectUUID}")
	private String objUUID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;


	Gson gson = new Gson();

	public List<PricingComponent> fetchComponent(ContextProvider context, String internalContractItemRefNo,
			String gmrRefNo) throws PricingException {
		String uri = validator.cleanData(contractURL + "/data/" + appUUID + "/" + objUUID + "?internalContractItemRefNo="
				+ internalContractItemRefNo);
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		List<PricingComponent> componentList = new ArrayList<PricingComponent>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchComponent entity :" + entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("fetchComponent entity :" + entity));
			JSONArray compArr = new JSONArray(response.getBody());
			for (int i = 0; i < compArr.length(); i++) {
				PricingComponent component = gson.fromJson(compArr.getJSONObject(i).toString(), PricingComponent.class);
				if (StringUtils.isEmpty(gmrRefNo) && (StringUtils.isEmpty(component.getInternalGmrRefNo())
						|| component.getInternalGmrRefNo().equals("NA"))) {
					componentList.add(component);
				} else if (!StringUtils.isEmpty(component.getInternalGmrRefNo())
						&& component.getInternalGmrRefNo().equals(gmrRefNo)) {
					componentList.add(component);
				}
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching Components"));
			List<String> params = new ArrayList<String>();
			params.add("Components");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}

		return componentList;
	}
	
	
	public List<PricingComponent> fetchComponentForContractCreation(String internalContractRefNo, String itemNo)
			throws PricingException {
		String uri = validator.cleanData(validator.cleanData(contractURL + "/data/" + appUUID + "/" + objUUID + "?componentDraftId="
				+ internalContractRefNo+"&itemNo="+itemNo));
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		if (!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		List<PricingComponent> componentList = new ArrayList<PricingComponent>();
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("fetchComponentForContractCreation entity :" + entity));
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("fetchComponentForContractCreation response :" + response));
			JSONArray compArr = new JSONArray(response.getBody());
			for (int i = 0; i < compArr.length(); i++) {
				PricingComponent component = gson.fromJson(compArr.getJSONObject(i).toString(), PricingComponent.class);
				componentList.add(component);
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching Components"));
			List<String> params = new ArrayList<String>();
			params.add("Components");
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}

		return componentList;
	}
	
}
