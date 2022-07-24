package com.eka.ekaPricing.standalone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.pojo.PricingComponent;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.ContextProvider;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Component
public class CurveQuantitySetter {
	@Autowired
	FormulaeCalculator formCalc;
	@Autowired
	CurveService curveService;
	@Autowired
	CurveDataFetcher curveFetcher;

	public List<Curve> setQuanity(String exp, List<Curve> curveList, double qty, JSONObject itemObj,
			ContextProvider tenantProvider, HashMap<String, String> exchangeMap, LocalDate asOfDate,
			String holidayString, List<PricingComponent> compList) throws Exception {
//		Aggregate functions use in exposure is to be done as part of another task.
		if (exp.contains("MIN") || exp.contains("MAX") || exp.contains("AVG")) {
			return curveList;
		}
		LocalDateTime asOf = asOfDate.atStartOfDay();
		List<Double> priceList = new ArrayList<Double>();
		Map<String, Curve> varMap = new HashMap<String, Curve>();
		String expressionWithVar = exp;
		int index = 1;
		int i1 = 0;
		int i2 = 0;
		String varName = "";
		for (Curve c : curveList) {
			priceList.add(c.getCalculatedPrice());
			i1 = expressionWithVar.indexOf(c.getCurveName());
			i2 = i1 + c.getCurveName().length();
			varName = "x" + index++;
			expressionWithVar = expressionWithVar.substring(0, i1) + varName
					+ expressionWithVar.substring(i2, expressionWithVar.length());
			varMap.put(varName, c);
		}
		double constant = getConstantFromExp(expressionWithVar, varMap);
		deduceCoefficients(varMap, constant, expressionWithVar);
		for (Curve c : curveList) {
			String exchange = exchangeMap.get(c.getCurveName());
			double coef = 0d;
			if (StringUtils.isEmpty(c.getComponent())) {
				coef = c.getCoefficient();
			} else {
				for (PricingComponent ci : compList) {
					if (ci.getProductComponentName().equals(c.getComponent())) {
						coef = ci.getComponentPercentage();
						if(curveService.checkZero(coef)) {
							throw new PricingException("Component value is assigned zero for: "+c.getComponent());
						}
						coef = coef/100;
						break;
					}
				}
				if (curveService.checkZero(coef)) {
					throw new PricingException("component value is zero for : "+c.getComponent());
				}
			}
			c.setQty(qty * coef);
			double[] pricedQty = calculatePricedQuantity(c, itemObj, tenantProvider, exchange, asOf, holidayString);
			c.setPricedQty(pricedQty[0]);
			c.setUnpricedQty(pricedQty[1]);
		}
		return curveList;
	}

	public double[] calculatePricedQuantity(Curve c, JSONObject itemObj, ContextProvider tenantProvider,
			String exchange, LocalDateTime asOf, String holidayString) throws Exception {
//		System.out.println("check 1");
		double[] resArr = new double[2];
		double pricedDays = 0.0d;
		double unPricedDays = 0.0d;
		double totalDays = 0;
		boolean noHolidayRule = false;
		LocalDateTime fromDate;
		LocalDateTime toDate;
		if(null!=c.getQpFromDate() && null!=c.getQpToDate()) {
			 fromDate = c.getQpFromDate().atStartOfDay();
			 toDate = c.getQpToDate().atStartOfDay();
			 c.setQpFromDate(fromDate.toLocalDate());
			 c.setQpToDate(toDate.toLocalDate());
		}else {
			fromDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryFromDate")).atStartOfDay();
			toDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryToDate")).atStartOfDay();
		}
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		List<LocalDateTime> dateList = new ArrayList<LocalDateTime>();
		dateList = Stream.iterate(fromDate, date -> date.plusDays(1)).limit(ChronoUnit.DAYS.between(fromDate, toDate.plusDays(1)))
				.collect(Collectors.toList());
//		when both from date and to date are same because of 0-1-0 offset values, we need to price only one date (from/to)
		if(dateList.size()==0 && fromDate.equals(toDate)) {
			dateList.add(fromDate);
		}
		HolidayRuleDetails holidayRuleDetail = new HolidayRuleDetails();
		holidayRuleDetail.setExchangeName(exchange);
		holidayRuleDetail.setDateRange(dateList);
		if(null!=holidayString && !holidayString.isEmpty()) {
			holidayRuleDetail.setHolidayRule(holidayString);
			holidayRuleDetail.setExchangeName(c.getExchange());
		}
		else {
			noHolidayRule = true;
			holidayRuleDetail.setHolidayRule("Prior Business Day");
		}
		JSONArray holidayObj = new JSONArray(curveService.applyHolidayRule(holidayRuleDetail, tenantProvider));
		List<LocalDateTime> workingDaysList = new ArrayList<LocalDateTime>();
		if(noHolidayRule) {
			List<LocalDateTime> workingDaysSet = new ArrayList<LocalDateTime>();
			for (int i = 0; i < holidayObj.length(); i++) {
				JSONObject dayObj = holidayObj.optJSONObject(i);
				LocalDateTime day = LocalDateTime.parse(dayObj.optString("dateToBeUsed"), formatter);
				if (day.isAfter(fromDate) || day.isEqual(fromDate)) {
					workingDaysSet.add(day);
				}
			}
			for (LocalDateTime d: workingDaysSet) {
				workingDaysList.add(d);
			}
		}
		else {
			for (int i = 0; i < holidayObj.length(); i++) {
				JSONObject dayObj = holidayObj.optJSONObject(i);
				if(dayObj.optString("dateToBeUsed").equals("NA")) {
					continue;
				}
				LocalDateTime day = LocalDateTime.parse(dayObj.optString("dateToBeUsed"), formatter);
				LocalDateTime realDay = LocalDateTime.parse(dayObj.optString("date"), formatter);
				if (day.isAfter(fromDate) || day.isEqual(fromDate)) {
					if(holidayString.equals("Ignore Weekends")) {
						String dayString = realDay.getDayOfWeek().toString().toLowerCase();
						if(dayString.contains("sat") || dayString.contains("sun")) {
							continue;
						}
					}
					workingDaysList.add(day);
				}
			}
		}
		
		for (LocalDateTime day : workingDaysList) {
			if (!day.isAfter(asOf)) {
				pricedDays++;
			}
		}
		totalDays = workingDaysList.size();
		double qty = c.getQty();
		if (totalDays == 0) {
			resArr[0] = qty;
		} else {
			resArr[0] = (pricedDays / totalDays) * qty;
		}
		c.setPricedDays((int) pricedDays);
		unPricedDays = totalDays-pricedDays;
		c.setUnPricedDays((int) unPricedDays);
		resArr[1] = qty - resArr[0];
		return resArr;
	}
	
	public double getConstantFromExp(String exp, Map<String, Curve> varMap) throws PricingException {
		for (Map.Entry<String, Curve> entry : varMap.entrySet()) {
			exp = exp.replace("{{" + entry.getKey() + "}}", "0");
		}
		Expression e = new ExpressionBuilder(exp).build();
		double res = 0;
		try {
			res = e.evaluate();
		} catch (Exception exc) {
			throw new PricingException("Invalid Expression");
		}
		return res;
	}

	public void deduceCoefficients(Map<String, Curve> varMap, double constant, String exp) throws PricingException {
		for (Map.Entry<String, Curve> entry : varMap.entrySet()) {
//			exp = exp.replace("{{"+entry.getKey()+"}}", );
			String modifiedExp = simplifyExpression(exp, entry.getKey(), varMap);
			Expression e = new ExpressionBuilder(modifiedExp).build();
			double res = 0;
			try {
				res = e.evaluate();
			} catch (Exception exc) {
				throw new PricingException("Invalid Expression");
			}
			res = res - constant;
			if(res<0) {
				res = res*-1;
			}
			entry.getValue().setCoefficient(res);
		}
	}

	public String simplifyExpression(String exp, String variable, Map<String, Curve> varMap) {
		for (Map.Entry<String, Curve> entry : varMap.entrySet()) {
			if (entry.getKey().equals(variable)) {
				exp = exp.replace(entry.getKey(), "1");
			} else {
				exp = exp.replace(entry.getKey(), "0");
			}
		}
		return exp;
	}
}
