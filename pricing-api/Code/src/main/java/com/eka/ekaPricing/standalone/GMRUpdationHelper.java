package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.FixationObject;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.service.CurveService;

@Component
public class GMRUpdationHelper {
	@Autowired
	GMRStatusObjectFetcher gmrStatusObjectFetcher;
	@Autowired
	TriggerPriceFetcher triggerPriceFetcher;
	@Autowired
	UpdateTriggerPriceObjectHelper updateTriggerPriceObjectHelper;
	@Autowired
	GMRStatusObjectCreationHelper gmrStatusObjectCreationHelper;
	@Autowired
	GMRCreationHelper gmrCreationHelper;
	@Autowired
	CurveService curveService;
//	method to update stored GMR details
	public void updateGMR(GMR gmr, String itemRefNo, boolean isCancellation) throws PricingException {
		double gmrQty = 0;
		List<String> stockRefInGMR = new ArrayList<String>();
		for (Stock st : gmr.getStocks()) {
			stockRefInGMR.add(st.getRefNo());
			gmrQty = gmrQty + st.getQty();
		}
		List<GMRStatusObject> gmrStatusObjectList = gmrStatusObjectFetcher.fetchGMRStatusObject(itemRefNo);
		List<TriggerPrice> triggerPriceList = triggerPriceFetcher.fetchTriggerPriceDetails(null, itemRefNo);
		boolean isCancelInModify = false;
		if(!isCancellation) {
			isCancelInModify = checkCancelInModify(gmrStatusObjectList, gmr, stockRefInGMR);
		}
		List<FixationObject> fixationUsed = new ArrayList<FixationObject>();
		if (isCancellation || isCancelInModify) {
			for(GMRStatusObject statusObj : gmrStatusObjectList) {
				if(statusObj.getGmrStatus().equals("CANCELLED")) {
					continue;
				}
				if(statusObj.getGmrRefNo().equalsIgnoreCase(gmr.getRefNo())) {
					gmrQty = Double.parseDouble(statusObj.getGmrQty());
					fixationUsed.addAll(statusObj.getFixationUsed());
					statusObj.setGmrCancelledQty(gmrQty);
					statusObj.setGmrStatus("CANCELLED");
					statusObj.setGmrFixedQty(0);
					gmrStatusObjectCreationHelper.createGMRStatusObject(statusObj);
				}
			}
			if(curveService.checkZero(gmrQty)) {
				return;
			}
			Map<String, Double> triggerPriceFixationMap = new HashMap<String, Double>();
			for (FixationObject fixation : fixationUsed) {
				if (!fixation.getGmrRef().equalsIgnoreCase(gmr.getRefNo())) {
					continue;
				}
				if (triggerPriceFixationMap.containsKey(fixation.getFixatonNumber())) {
					triggerPriceFixationMap.put(fixation.getFixatonNumber(),
							triggerPriceFixationMap.get(fixation.getFixatonNumber()) + fixation.getFixationQty());
				} else {
					triggerPriceFixationMap.put(fixation.getFixatonNumber(), fixation.getFixationQty());
				}
			}
			Set<Entry<String, Double>> triggerPriceFixationEntrySet = triggerPriceFixationMap.entrySet();
			for(Entry<String, Double> e: triggerPriceFixationEntrySet) {
				String fixationNum = e.getKey();
				TriggerPrice triggerPrice = null;
				for (TriggerPrice trigger : triggerPriceList) {
					if (trigger.getFixationRefNo().equalsIgnoreCase(fixationNum)) {
						triggerPrice = trigger;
						if(triggerPrice.getItemFixedQtyAvailable()<triggerPrice.getQuantity()) {
							triggerPrice.setItemFixedQtyAvailable(triggerPrice.getItemFixedQtyAvailable() + e.getValue());
						}
						break;
					}
				}
				if(null!=triggerPrice) {
					updateTriggerPriceObjectHelper.updateTriggerPrice(triggerPrice);
					if(triggerPrice.getExecution().equals("Post- exection") && !isCancelInModify) {
						updateTriggerPriceObjectHelper.deleteTriggerPrice(triggerPrice);
					}
				}
			}
		}
		else {
			for(GMRStatusObject statusObj : gmrStatusObjectList) {
				JSONArray gmrDataArr = gmrCreationHelper.fetchGMRData(itemRefNo);
				double prevQty = 0d;
				double modifiedQty = 0d;
				for (int gmrInd = 0; gmrInd < gmrDataArr.length(); gmrInd++) {
					JSONObject gmrData = gmrDataArr.optJSONObject(gmrInd);
					if(gmrData.optString("internalGMRRefNo").equalsIgnoreCase(gmr.getRefNo())) {
						String payload = gmrData.optString("payload");
						if(StringUtils.isEmpty(payload.trim())) {
							payload = gmrData.optString("inputPayload");
						}
						JSONObject payloadJson = new JSONObject(payload);
						JSONArray stockArray = payloadJson.getJSONArray("stocks");
						for(int i =0; i<stockArray.length(); i++) {
							JSONObject stockObj = stockArray.getJSONObject(i);
							prevQty = prevQty + stockObj.optDouble("qty");
							
						}
					}
					else {
						continue;
					}
					
				}
				if(statusObj.getGmrRefNo().equalsIgnoreCase(gmr.getRefNo())) {
					fixationUsed.addAll(statusObj.getFixationUsed());
					modifiedQty = gmrQty - prevQty;
					if(curveService.checkZero(modifiedQty)) {
						return;
					}
					if (prevQty > gmrQty) {
						double qty = Double.parseDouble(statusObj.getGmrQty()) - modifiedQty;
						double fixedQty = statusObj.getGmrFixedQty() - modifiedQty;
						statusObj.setGmrFixedQty(fixedQty);
						statusObj.setGmrQty(Double.toString(qty));
						statusObj.setGmrUnFixedQty(qty-fixedQty);
						if(qty - fixedQty > 0) {
							statusObj.setGmrStatus("PARTIALLY FIXED");
						}
						else {
							statusObj.setGmrStatus("FULLY FIXED");
						}
						gmrStatusObjectCreationHelper.createGMRStatusObject(statusObj);
					}
					else {
						double fixedQty = statusObj.getGmrFixedQty();
						statusObj.setGmrQty(Double.toString(gmrQty));
						statusObj.setGmrUnFixedQty(gmrQty - fixedQty);
						statusObj.setGmrStatus("PARTIALLY FIXED");
						statusObj.setGmrRefNo(gmr.getRefNo());
						statusObj.setInternalContractItemRefNo(itemRefNo);
						gmrStatusObjectCreationHelper.createGMRStatusObject(statusObj);
					}
//					statusObj.setGmrStatus("CANCELLED");
					
				}
				
				Map<String, Double> triggerPriceFixationMap = new HashMap<String, Double>();
				fixationUsed = sortFixationObjects(fixationUsed);
				for (FixationObject fixation : fixationUsed) {
					if (!fixation.getGmrRef().equalsIgnoreCase(gmr.getRefNo())) {
						continue;
					}
					if(modifiedQty == 0) {
						break;
					}
					if (triggerPriceFixationMap.containsKey(fixation.getFixatonNumber())) {
						if(curveService.checkNegative(modifiedQty - fixation.getFixationQty()) ||
								curveService.checkZero(modifiedQty - fixation.getFixationQty())) {
							modifiedQty = 0;
							triggerPriceFixationMap.put(fixation.getFixatonNumber(),
									triggerPriceFixationMap.get(fixation.getFixatonNumber()) + modifiedQty);
						}
						else {
							modifiedQty = modifiedQty - fixation.getFixationQty();
							triggerPriceFixationMap.put(fixation.getFixatonNumber(),
									triggerPriceFixationMap.get(fixation.getFixatonNumber()) + fixation.getFixationQty());
						}
						
					} else {
						if (curveService.checkNegative(modifiedQty - fixation.getFixationQty())
								|| curveService.checkZero(modifiedQty - fixation.getFixationQty())) {
							modifiedQty = 0;
							triggerPriceFixationMap.put(fixation.getFixatonNumber(), modifiedQty);
						} else {
							modifiedQty = modifiedQty - fixation.getFixationQty();
							triggerPriceFixationMap.put(fixation.getFixatonNumber(), fixation.getFixationQty());
						}
					}
				}
				
				Set<Entry<String, Double>> triggerPriceFixationEntrySet = triggerPriceFixationMap.entrySet();
				for(Entry<String, Double> e: triggerPriceFixationEntrySet) {
					String fixationNum = e.getKey();
					TriggerPrice triggerPrice = null;
					for (TriggerPrice trigger : triggerPriceList) {
						if (trigger.getFixationRefNo().equalsIgnoreCase(fixationNum)) {
							triggerPrice = trigger;
							triggerPrice.setItemFixedQtyAvailable(triggerPrice.getItemFixedQtyAvailable() + e.getValue());
							break;
						}
					}
					if(null!=triggerPrice) {
						updateTriggerPriceObjectHelper.updateTriggerPrice(triggerPrice);
					}
				}
			}
		}
	}
	
	public List<FixationObject> sortFixationObjects(List<FixationObject> fixationList) {
		Collections.sort(fixationList, new Comparator<FixationObject>() {
			@Override
			public int compare(FixationObject t1, FixationObject t2) {
				return t2.getFixatonNumber().compareTo(t1.getFixatonNumber());
			}
		}); 
		return fixationList;
	}
	
	public boolean checkCancelInModify(List<GMRStatusObject> gmrStatusObjectList, GMR gmr, List<String> stockRefInGMR) {
		GMRStatusObject gmrStatusObjectForPrevious = null;
		for(GMRStatusObject gmrStatusObj : gmrStatusObjectList) {
			if(gmrStatusObj.getGmrRefNo().equals(gmr.getRefNo())) {
				gmrStatusObjectForPrevious = gmrStatusObj;
				break;
			}
		}
		if(null!=gmrStatusObjectForPrevious) {
			List<FixationObject> fixationsInPrevious = gmrStatusObjectForPrevious.getFixationUsed();
			for(FixationObject fixation: fixationsInPrevious) {
				for(JSONObject stockObjectInFixation : fixation.getStocks()) {
					String stockRefNo = stockObjectInFixation.optString("stock");
					if(stockRefInGMR.contains(stockRefNo)) {
						return false;
					}
				}
			}
		}
		return true;
	}
}