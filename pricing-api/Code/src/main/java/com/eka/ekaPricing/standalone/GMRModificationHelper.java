package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.resource.GMRModificationObject;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class GMRModificationHelper {

	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Autowired
	ContextProvider contextProvider;
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(GMRModificationHelper.class);
	public void modifyGMR(GMR gmr, String internalContractItemRefNo) throws PricingException {
		String uri = connectHost + "/workflow/notifyDataChange";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		List<GMRModificationObject> payloadList = new ArrayList<GMRModificationObject>();
		GMRModificationObject payload = new GMRModificationObject();
		payload.setGmr(gmr);
		payloadList.add(payload);
		payload.setInternalContractItemRefNo(internalContractItemRefNo);
		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("app", "physicals");
		bodyObj.accumulate("object", "gmr-modify-object");
		bodyObj.accumulate("objectAction", "UPDATE");
		bodyObj.accumulate("payload", payloadList);

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("gmr creation entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("gmr modification entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for modifying gmr : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(contextProvider, "038", new ArrayList<String>()));
		}

	}
}