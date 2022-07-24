package com.eka.ekaPricing.standalone;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.TieredPricingItem;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ConnectExposureData {
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Value("${eka.physicals.udid}")
	private String appUUID;
	@Value("${eka.contract.url}")
	private String connectHost;
	@Value("${eka.connectExposure.objectUUID}")
	private String connectExposureUUID;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	CommonValidator validator;

	final static org.owasp.esapi.Logger logger = ESAPI.getLogger(ConnectExposureData.class);
	
	public List<Map<String, Object>> updateConnectExposureBody(ContextProvider context,JSONObject output, Contract contract, Formula formula, String itemRefNo) throws PricingException {
		
		List<GMR> gmrList = contract.getItemDetails().get(0).getGmrDetails();
		List<String> list= new ArrayList<>();
		JSONObject filter1 = new JSONObject();
		JSONObject filter2 = new JSONObject();
		JSONArray fieldsArr = new JSONArray();
		JSONObject bodyObj = new JSONObject();
		filter1.put("fieldName", "itemRefNo");
		filter1.put("value", itemRefNo);
		filter1.put("operator", "eq");
		fieldsArr.put(filter1);
		if(null!=gmrList && gmrList.size()>0) {
			for(GMR gmr : gmrList) {
				list.add(gmr.getRefNo());
			}
				list.add("");
				list.add("Outturn Loss");
				filter2.put("fieldName", "gmrRefNo");
				filter2.put("value", list);
				filter2.put("operator", "in");
				fieldsArr.put(filter2);
			
		}
		JSONObject filterData = new JSONObject();
		filterData.put("filter", fieldsArr);
		bodyObj.accumulate("filterData", filterData);
		
		List<Curve> curveList = formula.getCurveList();
		List<TieredPricingItem> tieredList = contract.getItemDetails().get(0).getTieredItemList();
		if (null != tieredList && tieredList.size() > 0) {
			curveList = new ArrayList<Curve>();
			for(TieredPricingItem t: tieredList) {
				curveList.addAll(t.getFormulaObj().getCurveList());
			}
		}
		deleteConnectExposureBodyObj(context, bodyObj);
		DecimalFormat numberFormat = new DecimalFormat("#.#####");
		numberFormat.setRoundingMode(RoundingMode.CEILING);
		List<Map<String, Object>> payloadList = new ArrayList<>();
		JSONObject contractObj = output.getJSONObject("contract");
		JSONObject contractItemObj = contractObj.optJSONArray("itemDetails").getJSONObject(0);
		String internalContractItemRefNo = contract.getItemDetails().get(0).getInternalItemRefNo();
		if(null==internalContractItemRefNo) {
			internalContractItemRefNo=contract.getItemDetails().get(0).getRefNo();
		}
		JSONObject priceDetailsObj = contractItemObj.optJSONObject("priceDetails");
		String product = contract.getProduct();
		String quality = contract.getQualityName();
		String profitCenter = contract.getProfitCenter();
		String strategy = contract.getStrategy();
		String locationName = contract.getLocationName();
		String incoTermName = contract.getIncoTermName();
		
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
			String baseQtyUnit = curveDataObj.optString("baseQtyUnit");
			double coefficient = curveDataObj.optDouble("coefficient");
			String itemQtyUnit = curveDataObj.optString("qtyUnit");
			String status=curveDataObj.optString("status");
			String remarks=curveDataObj.optString("remarks");
			String valuationInstrument = curveDataObj.optString("valuationInstrument");

			if(StringUtils.isEmpty(valuationInstrument)) {
				valuationInstrument = "Quotational";
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
				double pricedQuantityInBaseQtyUnit = qtyDataObj.optDouble("pricedQuantityInBaseQtyUnit");
				double unpricedQuantityInBaseQtyUnit = qtyDataObj.optDouble("unpricedQuantityInBaseQtyUnit");
				String instrumentDeliveryMonth = qtyDataObj.optString("instrumentDeliveryMonth");
				double contractualConversionFactor = qtyDataObj.optDouble("contractualConversionFactor");
				double actualConversionFactor = qtyDataObj.optDouble("actualConversionFactor");
				String gmrRefNo=qtyDataObj.optString("gmrRefNo");
				String gmrQuality=qtyDataObj.optString("quality");
				String gmrCurveName=qtyDataObj.optString("curveName");
				String gmrCurveQtyUnit=qtyDataObj.optString("curveQtyUnit");
				String titleTransferStatus=qtyDataObj.optString("titleTransferStatus");
				String gmrLocationName=qtyDataObj.optString("locationName");
				if(!StringUtils.isEmpty(gmrQuality)) {
					quality = gmrQuality;
				}else {
					quality=contract.getQualityName();
				}
				
				if(!StringUtils.isEmpty(gmrCurveName)) {
					curveName = gmrCurveName;
				}else {
					curveName=curveDataObj.optString("curveName");
				}
				
				if(!StringUtils.isEmpty(gmrCurveQtyUnit)) {
					curveQtyUnit = gmrCurveQtyUnit;
				}else {
					curveQtyUnit = curveDataObj.optString("curveQtyUnit");
				}
				
				if(!StringUtils.isEmpty(gmrLocationName)) {
					locationName = gmrLocationName;
				}else {
					locationName = contract.getLocationName();
				}
				
				Map<String, Object> payloadMap = new HashMap<>();
				payloadMap.put("contractRefNo", contract.getContractRefNo());
				payloadMap.put("itemRefNo", itemRefNo);
				payloadMap.put("internalContractItemRefNo", internalContractItemRefNo);
				payloadMap.put("product", product);
				payloadMap.put("quality", quality);
				payloadMap.put("valuationInstrument", valuationInstrument);
				payloadMap.put("deliveryDate", pricingDate);
				payloadMap.put("itemQuantityUnit", itemQtyUnit);
				payloadMap.put("curveName", curveName);
				payloadMap.put("curveQuantityUnit", curveQtyUnit);
				if(contractualConversionFactor!=0) {
					payloadMap.put("contractualConversionFactor", numberFormat.format(contractualConversionFactor));
				}else {
					payloadMap.put("contractualConversionFactor", "");
				}
				
				payloadMap.put("profitCenter", profitCenter);
				payloadMap.put("strategy", strategy);
				if(actualConversionFactor!=0) {
					payloadMap.put("actualConversionFactor", numberFormat.format(actualConversionFactor));
				}else {
					payloadMap.put("actualConversionFactor", "");
				}
				
				payloadMap.put("baseQuantityUnit", baseQtyUnit);
				if(pricedQuantityInBaseQtyUnit!=0) {
					payloadMap.put("exposureTypeBaseQty", numberFormat.format(pricedQuantityInBaseQtyUnit));
				}else {
					payloadMap.put("exposureTypeBaseQty", numberFormat.format(unpricedQuantityInBaseQtyUnit));
				}
				payloadMap.put("curveCoefficient", coefficient);
				if(pricedQty!=0) {
					payloadMap.put("exposureTypeCurveQty", numberFormat.format(pricedQty));
				}else {
					payloadMap.put("exposureTypeCurveQty", numberFormat.format(unpricedQty));
				}
				payloadMap.put("instrumentContractMonth", instrumentDeliveryMonth);
				payloadMap.put("incoTermName", incoTermName);
				payloadMap.put("locationName", locationName);
				payloadMap.put("status", status);
				payloadMap.put("remarks", remarks);
				payloadMap.put("gmrRefNo", gmrRefNo);
				payloadMap.put("titleTransferStatus", titleTransferStatus);
				payloadList.add(payloadMap);
			}
		 }
		}
		
		return payloadList;
	}
	
	public void updateConnectExposure(ContextProvider context, List<Map<String, Object>> payloadList)
			throws PricingException {
		String uri = connectHost + "/workflow/notifyDataChange";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		
		JSONObject bodyObj = new JSONObject();
		bodyObj.accumulate("app", "physicals");
		bodyObj.accumulate("object", "new-exposure-format-object");
		bodyObj.accumulate("objectAction", "CREATE");
		bodyObj.accumulate("payload", payloadList);

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Connect Exposure entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Connect Exposure  entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for saving Connect Exposure : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(context, "047", new ArrayList<String>()));
		}

	}
		
	public void deleteConnectExposureBodyObj(ContextProvider context, JSONObject bodyObj)
			throws PricingException {
		String uri = connectHost + "/data/" +appUUID+"/"+connectExposureUUID+"/bulkDelete";
		HttpHeaders headers = new HttpHeaders();
		RestTemplate restTemplate = new RestTemplate();
		headers.add("Content-Type", "application/json");
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
		headers.add("requestId", contextProvider.getCurrentContext().getRequestId());

		HttpEntity<String> entity = new HttpEntity<String>(bodyObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Delete Exposure entity: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Delete Exposure  entity response: " + response.getBody()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " for Deleting Connect Exposure : " + e.getMessage()));
			throw new PricingException(messageFetcher.fetchErrorMessage(context, "048", new ArrayList<String>()));
		}

	}
		
}
