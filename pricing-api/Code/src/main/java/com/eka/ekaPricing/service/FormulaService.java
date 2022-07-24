package com.eka.ekaPricing.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FormulaService {
	@Autowired
	ContextProvider context;
	@Value("${collections.url}")
	private String collectionURL;
	@Autowired
	CommonValidator validator;

	public Formula calulateFormula(Contract contract, Formula formula, HttpServletRequest req)
			throws JsonParseException, JsonMappingException, IOException {
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		String uri = validator.cleanData(pricingProps.getPlatform_url() + collectionURL + formula.getCurveId());
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		String token = validator.cleanData(req.getHeader("Authorization"));
		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
		ResponseEntity<Object> result = restTemplate.exchange(validator.cleanData(uri), HttpMethod.GET, entity, Object.class);
		if (result != null) {
			ObjectMapper mapper = new ObjectMapper();

			Object obj = result.getBody();
			Object data = ((Map) obj).get("data");
			String tdate = "0";
			String closePrice = "0";
			for (Object ob : ((List<Object>) data)) {
				String tradeDate = (String) ((Map) ob).get("Trade Date");
				// System.out.println("tradeDate: "+tradeDate);
				if (tradeDate.compareToIgnoreCase(tdate) > 0) {
					tdate = tradeDate;
					closePrice = (String) ((Map) ob).get("Close Price");
				}
			}

			System.out.println(tdate + "\n" + closePrice);
			formula.setPrice(((Double) (Double.parseDouble(closePrice) + 10)).toString());
		}
		return formula;
	}

	public List<Object> priceListForPreview(Contract contract, Formula formula, HttpServletRequest req) {
		String uri =validator.cleanData( collectionURL + formula.getCurveId());
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		List<Object> dataList = new ArrayList<Object>();
		String token = validator.cleanData(req.getHeader("Authorization"));
		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
		ResponseEntity<Object> result = restTemplate.exchange(uri, HttpMethod.GET, entity, Object.class);
		if (result != null) {
			ObjectMapper mapper = new ObjectMapper();
			Object obj = result.getBody();
			Object data = ((Map) obj).get("data");
			dataList = (List<Object>) data;

		}
		return dataList;
	}

}
