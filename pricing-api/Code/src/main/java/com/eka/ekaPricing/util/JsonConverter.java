package com.eka.ekaPricing.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.PriceDifferential;
import com.eka.ekaPricing.pojo.TriggerPrice;

@Component
public class JsonConverter {
	final static Logger logger = ESAPI.getLogger(JsonConverter.class);
	public Formula convertJsonToFormula(JSONObject formulaObj) throws Exception{
		Formula f = new Formula();
		f.setId(formulaObj.optString("_id",""));
		f.setFormulaExpression(formulaObj.optString("newFormulaExp",""));
		f.setFormulaName(formulaObj.optString("formulaName",""));
		f.setPricePrecision(formulaObj.optString("pricePrecision",""));
		f.setTriggerPriceEnabled(formulaObj.optBoolean("triggerPriceEnabled",false));
		f.setHolidayRule(formulaObj.optString("holidayRule",""));
		JSONArray triggerListArr = formulaObj.getJSONArray("triggerPricing");
		List<PriceDifferential> pDiffList = new ArrayList<PriceDifferential>();
		int k=0;
		JSONArray priceDiffArr = formulaObj.getJSONArray("priceDifferential");
		while(k<priceDiffArr.length()) {
			pDiffList.add(convertJsonToPriceDifferential(priceDiffArr.getJSONObject(k)));
			k++;
		}
		f.setPriceDifferential(pDiffList);
		List<TriggerPrice> triggerList = new ArrayList<TriggerPrice>();
		int j=0;
		while(j<triggerListArr.length()) {
			if(null==triggerListArr.get(j)) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Invalid trigger price details : "+triggerListArr));
				throw new PricingException("Invalid trigger price details");
			}
			triggerList.add(convertJsonToTriggerPrice(triggerListArr.getJSONObject(j)));
			j++;
		}
		f.setTriggerPricing(triggerList);
		int i = 0;
		JSONArray formulaCurveArray = formulaObj.getJSONArray("curves");
		List<Curve> cList = new ArrayList<Curve>();
		while(i<formulaCurveArray.length()) {
			JSONObject curveObject = formulaCurveArray.getJSONObject(i);
			Curve c = convertJsonToCurve(curveObject);
			cList.add(c);
			i++;
		}
		f.setCurveList(cList);
		return f;
	}
	
	public Curve convertJsonToCurve(JSONObject jObj) throws JSONException, ParseException {
		Curve c = new Curve();
		c.setCurveName(jObj.optString("curveName",""));
		c.setPricePoint(jObj.optString("pricePoint",""));
		c.setPriceType(jObj.optString("priceType",""));
		c.setPriceQuoteRule(jObj.optString("priceQuoteRule",""));
		c.setPeriod(jObj.optString("period",""));
//		c.setCurveID(jObj.getString("_id"));
		c.setQuotedPeriod(jObj.optString("quotedPeriod",""));
		c.setQuotedPeriodDate(jObj.optString("quotedPeriodDate",""));
		try {
			c.setStartDate(jObj.optString("startDate",""));
			c.setEndDate(jObj.optString("endDate",""));
		}
		catch (Exception e){
			System.out.println("Dates not available");
		}
		c.setOffset(jObj.optString("offset",""));
		c.setEvent(jObj.optString("event",""));
		c.setOffsetType(jObj.optString("offsetType",""));
		c.setFxType(jObj.optString("fxType",""));
		c.setFxInput(jObj.optFloat("fxInput",0.0f));
		String differential = jObj.optString("differential","+0");		
		c.setDifferential(differential);
		c.setDifferentialUnit(jObj.optString("differentialUnit",""));
		c.setQtyUnitConversionFactor(jObj.optDouble("qtyUnitConversionFactor"));
		c.setVersion(jObj.optString("version",""));
		c.setPricingPeriod(jObj.optString("pricingPeriod"));
		c.setActualPricing(jObj.optBoolean("isActualPricing"));
		c.setActualQuoted(jObj.optBoolean("isActualQuoted"));
		c.setMonthDefinition(jObj.optString("monthDefinition"));
		c.setOffsetDays(jObj.optInt("offsetDays"));
		c.setIndexPrecision(jObj.optInt("indexPrecision"));
		c.setFxCurve(jObj.optString("fxCurve"));
//		try {
//			c.setFxRate(jObj.getFloat("fxInput"));
//		}
//		catch (Exception e){
//			System.out.println("fx input not applicable");
//		}
		return c;
	}
	
	public TriggerPrice convertJsonToTriggerPrice(JSONObject jObj) {
		TriggerPrice tPriceObj = new TriggerPrice();
		tPriceObj.setPrice(jObj.optInt("price",0));
		tPriceObj.setQuantity(jObj.optInt("quantity",0));
		tPriceObj.setTriggerDate(jObj.optString("triggerDate",""));
		tPriceObj.setPriceU(jObj.optString("priceU",""));
		tPriceObj.setFxrate(jObj.optDouble("fxrate",1));
		return tPriceObj;
	}
	
	public PriceDifferential convertJsonToPriceDifferential(JSONObject jObj) {
		PriceDifferential priceDiffObj = new PriceDifferential();
		priceDiffObj.setDifferentialType(jObj.optString("differentialType",""));
		priceDiffObj.setDifferentialUnit(jObj.optString("differentialUnit",""));
		priceDiffObj.setDifferentialValue(jObj.optDouble("differentialValue",0.0));
		priceDiffObj.setDiffLowerThreashold(jObj.optDouble("diffLowerThreashold",0.0));
		priceDiffObj.setDiffUpperThreshold(jObj.optDouble("diffUpperThreshold",0.0));
		return priceDiffObj;
	}
}
