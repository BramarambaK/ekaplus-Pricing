package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.util.ContextProvider;

@Component
public class PeriodFetcher {

	@Autowired
	ContractDataFetcher contractFetcher;

	public List<JSONObject> fetchContractItems(ContextProvider tenantProvider, String contractID) throws Exception {
		List<JSONObject> itemList = new ArrayList<>();
		JSONArray contracts = contractFetcher.getContracts(tenantProvider.getCurrentContext().getToken(),
				tenantProvider.getCurrentContext().getTenantID(), contractID);
		int i = 0;
		while (i < contracts.length()) {
			JSONObject jObj = contracts.getJSONObject(i);
			itemList.add(jObj);
			i++;
		}
		return itemList;
	}
}
