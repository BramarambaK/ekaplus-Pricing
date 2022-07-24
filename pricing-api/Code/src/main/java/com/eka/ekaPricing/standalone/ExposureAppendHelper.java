package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.TieredPricingItem;

@Component
public class ExposureAppendHelper {
	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(ExposureAppendHelper.class);
	@Value("${eka.pricing.collection}")
	private String collection;

	@Value("${eka.contract.url}")
	private String connectHost;

	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	
	@Async
	public Boolean uploadExposure(String body, HttpHeaders headers) {
		String uri = connectHost + "/collectionmapper/" + pricingUDID + "/" + pricingUDID + "/addToCollection";
		JSONArray collectionDataArray = new JSONArray(body);
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("collectionConnectMapProperty", "pricingexposure_collectionConnectMap");
		bodyObject.put("collectionHeaderProperty", "pricingexposure_collectionHeader");
		bodyObject.put("collectionName", "Liquids_Online_Exposure");
		bodyObject.put("collectionData", collectionDataArray);
//		System.out.println("+++++++++++++++++++"+collectionDataArray.toString());
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity<String> entity = new HttpEntity<String>(bodyObject.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("exposure update - entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			if (response.getStatusCodeValue() == 202 || response.getStatusCodeValue() == 200) {
				return true;
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()));
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Exposure upload failed for payload: " + body));
			
		}
		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("checking time post : " + LocalDateTime.now()));
		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Exposure upload success for payload: " + body));
		return false;

	}

	@Async
	public String createBodyObject(JSONObject output, Contract contract, Formula formula, String itemRefNo) {
		List<Curve> curveList = formula.getCurveList();
		List<TieredPricingItem> tieredList = contract.getItemDetails().get(0).getTieredItemList();
		if (null != tieredList && tieredList.size() > 0) {
			curveList = new ArrayList<Curve>();
			for(TieredPricingItem t: tieredList) {
				curveList.addAll(t.getFormulaObj().getCurveList());
			}
		}
		JSONArray collectionDataArray = new JSONArray();
		JSONObject contractObj = output.getJSONObject("contract");
		String internalContractRefNo = contractObj.optString("refNo");
		JSONObject contractItemObj = contractObj.optJSONArray("itemDetails").getJSONObject(0);
		String internalContractItemRefNo = contractItemObj.optString("refNo");
		JSONObject priceDetailsObj = contractItemObj.optJSONObject("priceDetails");
		String product = contract.getProduct();
		String quality = contract.getItemDetails().get(0).getQuality();
		String itemQtyUnit = priceDetailsObj.optString("quantityUnit");
		JSONArray curveDataArray = priceDetailsObj.optJSONArray("curveData");
		for (int i = 0; i < curveDataArray.length(); i++) {
			Curve c =new Curve();
			if(null!=curveList && curveList.size() > 0) {
				if(i<curveList.size()&& null!=curveList.get(i)) {
			    c = curveList.get(i);
				}
				
			}
			JSONObject curveDataObj = curveDataArray.optJSONObject(i);
			String curveQtyUnit = curveDataObj.optString("curveQtyUnit");
			String curveName = curveDataObj.optString("curveName");
			double coefficient = curveDataObj.optDouble("coefficient");
			String split = curveDataObj.optString("split");
			if(!StringUtils.isEmpty(split)) {
				split = "Split - " + curveDataObj.optString("split");
			}
			
			if(curveDataObj.has("exposureArray")) {
			JSONArray qtyDataArray = curveDataObj.optJSONArray("exposureArray");
			for (int j = 0; j < qtyDataArray.length(); j++) {
				JSONObject qtyDataObj = qtyDataArray.optJSONObject(j);
				String pricingDate = qtyDataObj.optString("date");
				double pricedQty = qtyDataObj.optDouble("pricedQty");
				double unpricedQty = qtyDataObj.optDouble("unPricedQty");
				double pricedPercentage = qtyDataObj.optDouble("pricedPercentage");
				double unpricedPercentage = qtyDataObj.optDouble("unpricedPercentage");
				String instrumentDeliveryMonth = qtyDataObj.optString("instrumentDeliveryMonth");
				JSONObject dataObj = new JSONObject();
				dataObj.put("As Of", new LocalDateTime(DateTimeZone.UTC));
				dataObj.put("Ref No", contract.getContractRefNo());
				dataObj.put("Item Ref No", itemRefNo);
				dataObj.put("InternalContractRefNo", internalContractRefNo);
				dataObj.put("InternalContractItemRefNo", internalContractItemRefNo);
				dataObj.put("Item Quantity Unit", itemQtyUnit);
				dataObj.put("Product", product);
				dataObj.put("Quality", quality);
				dataObj.put("Split", split);
				dataObj.put("Curve Name", curveName);
				dataObj.put("Component", c.getComponent());
				dataObj.put("Contractual Conversion Factor", c.getQtyUnitConversionFactor());
//				This field will be available in future
				dataObj.put("Actual Conversion Factor", "");
				dataObj.put("Curve Quantity Unit", curveQtyUnit);
				dataObj.put("Coefficient", coefficient);
				dataObj.put("Pricing Date", pricingDate);
				dataObj.put("Priced Quantity", pricedQty);
				dataObj.put("Unpriced Quantity", unpricedQty);
				dataObj.put("Item Priced Percentage", pricedPercentage);
				dataObj.put("Item Unpriced Percentage", unpricedPercentage);
				dataObj.put("Instrument Contract Month", instrumentDeliveryMonth);
				dataObj.put("Exposure Unique Filter", internalContractItemRefNo + "/" + pricingDate);
				collectionDataArray.put(dataObj);
			}
		 }
		}
		return collectionDataArray.toString();
	}
}