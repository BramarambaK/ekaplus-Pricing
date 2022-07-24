package com.eka.ekaPricing.curveBuilder;

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
import com.eka.ekaPricing.pojo.CurveBuilderProps;
import com.eka.ekaPricing.util.ContextProvider;


@Component
public class BuilderExecutor {
	final static  Logger logger = ESAPI.getLogger(BuilderExecutor.class); 

	@Autowired
	CurveBuilderDetailsFetcher curveBuilderDetailsFetcher;
	@Autowired
	ContextProvider context;

	@Value("${eka.curveBuilder.host}")
	public String builderHost;
	@Value("${eka.contract.url}")
	public String connectHost;

	public boolean  executeBuilder() throws PricingException {
		boolean res = false;
		List<CurveBuilderProps> builderPropList = curveBuilderDetailsFetcher.getBuilderCurves();
		for (CurveBuilderProps cbp : builderPropList) {
			res = performExtrapolate(cbp);
		}
		return res;
	}

	public boolean performExtrapolate(CurveBuilderProps builderProp) {
		String uri = connectHost+"/lambdagatewayapi";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", context.getCurrentContext().getToken());
		headers.add("requestId", context.getCurrentContext().getRequestId());
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());
		headers.add("endPoint", "trigger_curve_builder");
		JSONObject bodyObj = new JSONObject();
		bodyObj.put("numOfDays", builderProp.getTenor());
		bodyObj.put("Curve-Name", builderProp.getCurveName().trim());
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		try {
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			if(response.getStatusCodeValue()==200) {
				return true;
			}
		}
		catch(Exception e) {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exception while Building curves"+builderProp.getCurveName()));
			return false;
		}
		return false;
	}
}
