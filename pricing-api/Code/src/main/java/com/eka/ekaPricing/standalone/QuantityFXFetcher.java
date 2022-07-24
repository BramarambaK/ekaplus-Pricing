package com.eka.ekaPricing.standalone;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class QuantityFXFetcher {
	@Autowired
	ContextProvider contextProvider;

	public double getQtyFx(ContextProvider context, String fromUnit, String toUnit) {
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		String URI = pricingProps.getPlatform_url() + "/spring/smartapp/getUtilitiesUnitConversionList";
		String token = contextProvider.getCurrentContext().getToken();
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", token);
		HttpEntity entity = new HttpEntity(headers);
		double conversionFactor = 0d;
		try {
			HttpEntity<String> resultSet = restTemplate.exchange(URI, HttpMethod.GET, entity, String.class);
			JSONObject dataObj = new JSONObject(resultSet.getBody());
			JSONArray conversionArray = dataObj.getJSONArray("data");
			for (int i = 0; i < conversionArray.length(); i++) {
				JSONObject conversionObj = conversionArray.getJSONObject(i);
				String objFromUnit = conversionObj.getString("fromUnit");
				String objToUnit = conversionObj.getString("toUnit");
				if (objFromUnit.equals(fromUnit) && objToUnit.equals(toUnit)) {
					try {
						conversionFactor = conversionObj.getDouble("conversionFactor");
					} catch (Exception e) {
						throw new PricingException("invalid conversion rate");
					}
				}
			}
			if(conversionFactor==0d) {
				throw new PricingException("can not find conversion factor for "+fromUnit+" to "+toUnit);
			}

		} catch (Exception e) {
			System.out.println("exception here : " + URI);
		}
		return conversionFactor;
	}
}
