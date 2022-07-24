package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.FixationObject;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.resource.FixationCancellationObject;

@Component
public class FixationCancellationHelper {

	@Autowired
	GMRStatusObjectFetcher gmrStatusObjectFetcher;
	@Autowired
	TriggerPriceFetcher triggerPriceFetcher;
	@Autowired
	UpdateTriggerPriceObjectHelper updateTriggerPriceObjectHelper;
	@Autowired
	GMRStatusObjectCreationHelper gmrStatusObjectCreationHelper;
	
	public void cancelPostFixation(FixationCancellationObject cancellationObj) throws PricingException {
		String internalContractItemRefNo = cancellationObj.getInternalContractItemRefNo();
		String fixationRefNo = cancellationObj.getFixationRefNo();
		
		List<GMRStatusObject> gmrStatusObjectList = gmrStatusObjectFetcher.fetchGMRStatusObject(internalContractItemRefNo);
		List<TriggerPrice> triggerPriceList = triggerPriceFetcher.fetchTriggerPriceDetails(null, internalContractItemRefNo);
		
		for(GMRStatusObject statusObj : gmrStatusObjectList) {
			List<FixationObject> fixations = statusObj.getFixationUsed();
			double freeUpQty = 0;
			List<FixationObject> cancelledFixation = new ArrayList<FixationObject>();
			for(FixationObject fixation : fixations) {
				if(fixation.getFixatonNumber().equals(fixationRefNo)) {
					freeUpQty = freeUpQty + fixation.getFixationQty();
					cancelledFixation.add(fixation);
				}
			}
			for(FixationObject fixation : cancelledFixation) {
				statusObj.getFixationUsed().remove(fixation);
			}
			statusObj.setGmrFixedQty(statusObj.getGmrFixedQty()-freeUpQty);
			statusObj.setGmrUnFixedQty(statusObj.getGmrUnFixedQty()+freeUpQty);
			statusObj.setGmrStatus("PARTIALLY FIXED");
			gmrStatusObjectCreationHelper.createGMRStatusObject(statusObj);
		}
		for(TriggerPrice triggerPrice : triggerPriceList) {
			if(triggerPrice.getFixationRefNo().equalsIgnoreCase(fixationRefNo)) {
				updateTriggerPriceObjectHelper.deleteTriggerPrice(triggerPrice);
			}
		}
	}
}
