package com.eka.ekaPricing.curveBuilder;

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
import com.eka.ekaPricing.pojo.CurveBuilderProps;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Component
public class CurveBuilderDetailsFetcher {
	@Autowired
	ContextProvider context;

	final static  Logger logger = ESAPI.getLogger(CurveBuilderDetailsFetcher.class); 
	@Value("${eka.curveBuilder.uuid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String contractURL;
	@Value("${eka.curveBuilder.objectUUID}")
	private String objectUDID;

	Gson gson = new Gson();

	public List<CurveBuilderProps> getBuilderCurves() throws PricingException {
		String uri = contractURL + "/data/" + appUUID + "/" + objectUDID;
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", context.getCurrentContext().getToken());
//		headers.add("X-Locale", "en-US");
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());
		headers.add("requestId", context.getCurrentContext().getRequestId());
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		List<CurveBuilderProps> curveBuilderPropList = new ArrayList<CurveBuilderProps>();
		try {
			HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONArray curveArr = new JSONArray(response.getBody());
			for(int i=0; i<curveArr.length(); i++) {
				CurveBuilderProps curveBuilderProps = gson.fromJson(curveArr.getJSONObject(i).toString(), CurveBuilderProps.class);
				if(curveBuilderProps.getStatus().equalsIgnoreCase("active")) {
					curveBuilderPropList.add(curveBuilderProps);
				}
			}

		} catch (Exception e) {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while fetching Builder Curves"));
			throw new PricingException("Error while fetching curve properties");
		}
		return curveBuilderPropList;
	}
}
