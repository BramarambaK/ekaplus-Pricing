package com.eka.ekaPricing.standalone;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.util.ContextProvider;

@Component
public class MassToVolumeConversion {
	
	final static  Logger logger = ESAPI.getLogger(MassToVolumeConversion.class); 
	
	@Autowired
	MDMServiceFetcher mdmFetcher;
	
	private static final String WEIGHT = "Weight";
	private static final String VOLUME = "Volume";
	
	public Map<String, Object> getConversionRate(ContextProvider context, JSONObject payloadJson) throws Exception {
		
		String productId= payloadJson.getString("productId");
		String sourceUnitId = payloadJson.getString("sourceUnitId");
		String destinationUnitId = payloadJson.getString("destinationUnitId");
		String massUnit = payloadJson.getString("massUnitId");
		String volUnit = payloadJson.getString("volumeUnitId");
		double densityValue = payloadJson.getDouble("densityValue");
		String sourceMass="";
		String destinationMass="";
		String sourceVol="";
		String destinationVol="";
		String sourceCheck="";
		String destCheck="";
		double conversionFactor=0.0;
		double sourceMassValue=0.0;
		double destMassValue=0.0;
		double sourceVolValue=0.0;
		double destVolValue=0.0;
		DecimalFormat numberFormat = new DecimalFormat("#.########");
		numberFormat.setRoundingMode(RoundingMode.CEILING);
		Map<String, Object> bodyObject = new HashMap<String,Object>();	
		
		if(sourceUnitId.equalsIgnoreCase(destinationUnitId)) {
			 bodyObject.put("conversionFactor",conversionFactor);
			 bodyObject.put("msg","Source and Destination Units are Same");
		}else {
			// To check the source and destination unit are mass and volume-> call api
			
			 sourceCheck=getMassVolumeQtyCheck(productId,sourceUnitId);
			 destCheck=getMassVolumeQtyCheck(productId,destinationUnitId);
			 if(!sourceCheck.isEmpty() && sourceCheck.equals(WEIGHT)) {
				 sourceMass=sourceUnitId;
			 }else if(!sourceCheck.isEmpty() && sourceCheck.equals(VOLUME)) {
				 sourceVol=sourceUnitId;
			 }
			 
			 if(!destCheck.isEmpty() && destCheck.equals(WEIGHT)) {
				 destinationMass=destinationUnitId;
			 }else if(!destCheck.isEmpty() && destCheck.equals(VOLUME)) {
				 destinationVol=destinationUnitId;
			 }
					
			if(!sourceCheck.equalsIgnoreCase(destCheck)) {
				if(!sourceMass.isEmpty()) {
					double qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(context, productId, massUnit,
							sourceMass);
					sourceMassValue=densityValue*qtyConversionFactor;
					
				}else if(!destinationMass.isEmpty()){
					double qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(context, productId, massUnit,
							destinationMass);
					destMassValue=densityValue*qtyConversionFactor;
				}
				
				if(!sourceVol.isEmpty()) {
					double qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(context, productId, volUnit,
							sourceVol);
					sourceVolValue=qtyConversionFactor;
				}else if(!destinationVol.isEmpty()) {
					double qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(context, productId, volUnit,
							destinationVol);
					destVolValue=qtyConversionFactor;
				}
				if(!sourceCheck.isEmpty() && !destCheck.isEmpty()) {
					if(sourceCheck.contains(WEIGHT) && destCheck.contains(VOLUME)) {
						conversionFactor=destVolValue/sourceMassValue;
					}else {
						conversionFactor=destMassValue/sourceVolValue;
					}
					bodyObject.put("conversionFactor",numberFormat.format(conversionFactor));
					bodyObject.put("msg","Success");
				}else {
					if(!sourceCheck.isEmpty()) {
						if(sourceCheck.equalsIgnoreCase(WEIGHT)) {
							bodyObject.put("conversionFactor",conversionFactor);
							bodyObject.put("msg","The " +destinationUnitId+ " is not present in the Volume unit for the Product Selected");
						}else {
							bodyObject.put("conversionFactor",conversionFactor);
							bodyObject.put("msg","The " +destinationUnitId+ "is not present in the Mass unit for the Product Selected");
						}
						
					}else if(!destCheck.isEmpty()){
						if(destCheck.equalsIgnoreCase(WEIGHT)) {
							bodyObject.put("conversionFactor",conversionFactor);
							bodyObject.put("msg","The " +sourceUnitId+ " is not present in the Volume unit for the Product Selected");
						}else {
							bodyObject.put("conversionFactor",conversionFactor);
							bodyObject.put("msg","The " +sourceUnitId+ " is not present in the Mass unit for the Product Selected");
						}
					}
				}
				
			}else if(sourceCheck.isEmpty() && destCheck.isEmpty()) {
				bodyObject.put("conversionFactor",conversionFactor);
				bodyObject.put("msg","Both Source Unit and Destination Unit are not present for the Product Selected");
			}else {
				 bodyObject.put("conversionFactor",conversionFactor);
				 bodyObject.put("msg","Source and Destination Units are equal to "+sourceCheck+" Units");
			}
		}
		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Mass To Volume Conversion class body object: "+bodyObject));
		
		return bodyObject;
		
	}
	
public String getMassVolumeQtyCheck(String productId,String qtyUnit) throws Exception {
		
		String qtyCheck="";
		Map<String, String> massUnitId=mdmFetcher.populateMDMDataForMassVol(productId,WEIGHT);
		Map<String, String> volumeUnitId=mdmFetcher.populateMDMDataForMassVol(productId,VOLUME);
		if(!massUnitId.isEmpty()) {
			for(Map.Entry<String, String> entity : massUnitId.entrySet()) {
				if(entity.getValue().equals(qtyUnit)) {
					qtyCheck=WEIGHT;
				}
			}
		}
		if(!volumeUnitId.isEmpty()) {
			for(Map.Entry<String, String> entity : volumeUnitId.entrySet()) {
				if(entity.getValue().equals(qtyUnit)) {
					qtyCheck=VOLUME;
				}
			}
		}
		logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Mass To Volume Conversion class qtyCheck: "+qtyCheck));
		
		return qtyCheck;
		
	}
	
}
