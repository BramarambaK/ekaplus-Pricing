package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.util.ContextProvider;
import com.eka.ekaPricing.util.JsonConverter;

@Component
public class FormulaData {
	@Autowired
	FormulaFetcher formulaFetcher;
	@Autowired
	ExpressionBuilder expressionBuilder;
	@Autowired
	JsonConverter jsonConverter;

	public List<CurveDetails> populateCurveData(List<JSONObject> itemList, ContextProvider tenantProvider)
			throws Exception {
		List<String> curveList = new ArrayList<>();
		List<String> expList = new ArrayList<>();
		List<JSONArray> curveArrList = new ArrayList<>();
		List<CurveDetails> detailsList = new ArrayList<>();
		List<String> precisionList = new ArrayList<String>();
		JSONArray curveArr = new JSONArray();
		for (JSONObject jObj : itemList) {
			String formulaID = jObj.getJSONObject("pricing").getString("pricingFormulaId");
			JSONObject formulaObj = formulaFetcher.getFormula(tenantProvider, formulaID);
			String expression = null;
			expression = formulaObj.getString("formulaExpression");
			precisionList.add(formulaObj.getString("pricePrecision"));
			jObj.put("pricePrecision", formulaObj.getString("pricePrecision"));
			expList.add(expression);
			curveArr = formulaObj.getJSONArray("curves");
			curveArrList.add(curveArr);
		}
		int itemCounter = 0;
		String expression = "";
		CurveDetails curveDetail = new CurveDetails();
		while(itemCounter<itemList.size()) {
			curveList.clear();
			List<Curve> cList = new ArrayList<>();
			for(int i=0; i<curveArr.length(); i++) {
				expression = expList.get(itemCounter);
				JSONObject curveObject = curveArr.getJSONObject(i);
				curveList.add(curveObject.getString("curveName"));
				cList.add(jsonConverter.convertJsonToCurve(curveObject));
			}
			curveDetail.setCurveList(cList);
			curveDetail.setPricePrecision(precisionList.get(itemCounter));
			curveDetail.setExpression(expressionBuilder.buildExpression(curveList, expression));
			detailsList.add(curveDetail);
			itemCounter++;
		}
		return detailsList;
	}

	public List<CurveDetails> populateDefaultCurveData(CurveDetails curveDetails) throws Exception {
		List<CurveDetails> defList = new ArrayList<CurveDetails>();
		List<String> curveList = new ArrayList<String>();
		List<Curve> cList = new ArrayList<Curve>();
		for (Curve c : curveDetails.getCurveList()) {
			curveList.add(c.getCurveName());
			c.setPricePoint("Spot");
			c.setPriceType("Ask Price");
			c.setPriceQuoteRule("Contract Period Average");
			c.setPeriod("Delivery Period");
			cList.add(c);
		}
		String exp = expressionBuilder.buildExpression(curveList, curveDetails.getExpression());
		curveDetails.setExpression(exp);
		curveDetails.setCurveList(cList);
		defList.add(curveDetails);
		return defList;
	}
}
