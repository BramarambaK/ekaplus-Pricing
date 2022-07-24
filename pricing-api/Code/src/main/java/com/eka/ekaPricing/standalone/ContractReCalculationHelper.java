package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ContractReCalculationHelper {
	@Autowired
	FormulaFetcher formulaFetcher;
	@Autowired
	ContractItemFetcher contractItemFetcher;
	@Autowired
	PricingCallHelper pricingCallHelper;
	@Autowired
	MDMServiceFetcher mdmServiceFercher;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;

	public List<String> fetchFilteredContracts(List<String> curveList) {
		List<String> formulaList = formulaFetcher.fetchFormulaForModifiedCurveData(curveList);
		List<String> contractList = contractItemFetcher.getContractsForProvidedFormula(formulaList);
		return contractList;
	}

	public Map<String, String> reEvaulateContracts(List<String> filteredContractref) throws Exception {
		Map<String, String> errorMap = new HashMap<String, String>();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", validator.cleanData(contextProvider.getCurrentContext().getToken()));
		headers.add("X-TenantID", validator.cleanData(contextProvider.getCurrentContext().getTenantID()));
		headers.add("requestId", validator.cleanData(contextProvider.getCurrentContext().getRequestId()));
		headers.add("sourceDeviceId", validator.cleanData(contextProvider.getCurrentContext().getSourceDeviceId()));
		headers.add("X-Locale", "en_US");
		headers.add("origin", "reevaluation");
		for (String contractRef : filteredContractref) {
			try {
				JSONObject contractObj = contractItemFetcher.getContractForRef(contractRef);
				JSONArray itemArr = contractObj.optJSONArray("itemDetails");
				for (int i = 0; i < itemArr.length(); i++) {
					JSONObject payload = new JSONObject();
					JSONObject contractJsonObject = new JSONObject();
					contractJsonObject.put("refNo", contractObj.optString("internalContractRefNo"));
					contractJsonObject.put("asOfDate", new LocalDateTime(DateTimeZone.UTC)+"+0000");
					contractJsonObject.put("itemDetails", new JSONArray().put(createItemPayload(itemArr.getJSONObject(i))));
					contractJsonObject.put("noOfItems", 0);
					payload.put("contract", contractJsonObject);
					pricingCallHelper.callPricing(payload, headers);
				}
			}
			catch (Exception e){
				errorMap.put(contractRef, e.getMessage());
			}
		}
		return errorMap;
	}

	public JSONObject createItemPayload(JSONObject item) throws JSONException, Exception {
		JSONObject payload = new JSONObject();
		JSONObject pricingObj = new JSONObject();
		JSONObject itemPricingObj = item.optJSONObject("pricing");
		pricingObj.accumulate("priceType", itemPricingObj.optString("priceTypeId"));
		pricingObj.accumulate("priceUnit", mdmServiceFercher.getPriceUnitValue(item.optString("productId"),
				itemPricingObj.optString("priceUnitId")));
		pricingObj.accumulate("internalPriceUnitId", itemPricingObj.optString("priceUnitId"));
		payload.put("refNo", item.optString("internalItemRefNo"));
		payload.put("qty", item.optString("itemQty"));
		payload.put("deliveryFromDate", item.optString("deliveryFromDate"));
		payload.put("deliveryToDate", item.optString("deliveryToDate"));
		payload.put("qtyUnit",
				mdmServiceFercher.getContractQty(null, item.optString("itemQtyUnitId"), item.optString("productId")));
		payload.put("pdSchedule", new JSONArray());
		payload.put("gmrDetails", new JSONArray());
		payload.put("expiryDate", new LocalDateTime(DateTimeZone.UTC)+"+0000");
		payload.put("quality",
				mdmServiceFercher.getQualityUnitValue(item.optString("productId"), item.optString("quality")));
		payload.put("pricing", pricingObj);
		payload.put("itemQty", 0);
		return payload;

	}

}
