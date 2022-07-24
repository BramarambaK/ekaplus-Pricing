package com.eka.ekaPricing.pojo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Curve {
	private String curveID;
	private String curveName;
	private String priceType;
	private String startDate;
	private String endDate;
	private String pricePoint;
	private String quotedPeriod;
	private String quotedPeriodDate;
	private String priceQuoteRule;
	private String period;
	private String event;
	private String offsetType;
	private String offset;
	private List<JSONObject> itemList;
	private float fxInput;
	private String fxType;
	private String fxCurve;
	private String differential;
	private LocalDate qpFromDate;
	private LocalDate qpToDate;
	private int pricedDays;
	private int unPricedDays;
	double qty;
	double pricedQty;
	double unpricedQty;
//	After conversion if fx is applicable
	double calculatedPrice;
//	without fx
	double curvePrice;
	private List<TriggerPrice> triggerList;
	private String monthYear;
	private List<CurveMarketData> collectionArray;
	private int missingDays;
	private double coefficient;
	private Map<LocalDate, Double> priceMap = new HashMap<LocalDate, Double>();
	private Map<LocalDate, CurveMarketData> priceDetailsMap = new HashMap<LocalDate, CurveMarketData>();
	private LocalDate firstPricingDate;
	private String exchange;
	private String curveQty;
	private String curveCurrency;
	private Map<LocalDate, String> priceFlagMap = new HashMap<LocalDate, String>();
	private HolidayRuleDates holidayRuleDates;
	private List<GMRQPDateDetails> gmrQPDetailsList;
	private List<GMR> gmrList;
	private String component;
	private String differentialUnit;
	private double qtyUnitConversionFactor;
	private String version;
	private String pricingPeriod;
	private boolean isActualPricing;
	private boolean isActualQuoted;
	private String monthDefinition;
	private int offsetDays;
	private JSONArray expiryArr;
	private Map<LocalDate, Double> priceMapGMR = new HashMap<LocalDate, Double>();
	private Map<LocalDate, String> priceFlagMapGMR = new HashMap<LocalDate, String>();
	private boolean isExp;
	private JSONArray triggerPriceExposure;
	private int indexPrecision;
	private Map<LocalDate, String> monthFlagMap = new HashMap<LocalDate, String>();
	private Map<LocalDate, String> monthFlagMapGMR = new HashMap<LocalDate, String>();
	private Map<LocalDate, Double> priceWithoutFXMap = new HashMap<LocalDate, Double>();
	private Map<LocalDate, Double> priceFxMap = new HashMap<LocalDate, Double>();
	private Map<LocalDate, String> priceFxFlagMap = new HashMap<LocalDate, String>();
	private Map<LocalDate, Double> priceFxMapGMR = new HashMap<LocalDate, Double>();
	private Map<LocalDate, String> priceFxFlagMapGMR = new HashMap<LocalDate, String>();
	private boolean triggerPriceExecution;
	private double avgFx;
	private Map<String, List<JSONObject>> forwardPriceMap = new HashMap<String, List<JSONObject>>();
	private double totalTriggerPriceQty;
	
	public String getDifferentialUnit() {
		return differentialUnit;
	}

	public void setDifferentialUnit(String differentialUnit) {
		this.differentialUnit = differentialUnit;
	}

	public List<JSONObject> getItemList() {
		return itemList;
	}

	public void setItemList(List<JSONObject> itemList) {
		this.itemList = itemList;
	}

	public String getOffsetType() {
		return offsetType;
	}

	public void setOffsetType(String offsetType) {
		this.offsetType = offsetType;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

	public String getPricePoint() {
		return pricePoint;
	}

	public void setPricePoint(String pricePoint) {
		this.pricePoint = pricePoint;
	}

	public String getQuotedPeriod() {
		return quotedPeriod;
	}

	public void setQuotedPeriod(String quotedPeriod) {
		this.quotedPeriod = quotedPeriod;
	}

	public String getQuotedPeriodDate() {
		return quotedPeriodDate;
	}

	public void setQuotedPeriodDate(String quotedPeriodDate) {
		this.quotedPeriodDate = quotedPeriodDate;
	}

	public String getPriceQuoteRule() {
		return priceQuoteRule;
	}

	public void setPriceQuoteRule(String priceQuoteRule) {
		this.priceQuoteRule = priceQuoteRule;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getCurveID() {
		return curveID;
	}

	public void setCurveID(String curveID) {
		this.curveID = curveID;
	}

	public String getCurveName() {
		return curveName;
	}

	public void setCurveName(String curveName) {
		this.curveName = curveName;
	}

	public String getPriceType() {
		return priceType;
	}

	public void setPriceType(String priceType) {
		this.priceType = priceType;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getFxType() {
		return fxType;
	}

	public void setFxType(String fxType) {
		this.fxType = fxType;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getDifferential() {
		return differential;
	}

	public void setDifferential(String differential) {
		this.differential = differential;
	}

	public LocalDate getQpFromDate() {
		return qpFromDate;
	}

	public void setQpFromDate(LocalDate qpFromDate) {
		this.qpFromDate = qpFromDate;
	}

	public LocalDate getQpToDate() {
		return qpToDate;
	}

	public void setQpToDate(LocalDate qpToDate) {
		this.qpToDate = qpToDate;
	}

	public int getPricedDays() {
		return pricedDays;
	}

	public void setPricedDays(int pricedDays) {
		this.pricedDays = pricedDays;
	}

	public int getUnPricedDays() {
		return unPricedDays;
	}

	public void setUnPricedDays(int unPricedDays) {
		this.unPricedDays = unPricedDays;
	}

	public double getQty() {
		return qty;
	}

	public void setQty(double qty) {
		this.qty = qty;
	}

	public double getPricedQty() {
		return pricedQty;
	}

	public void setPricedQty(double pricedQty) {
		this.pricedQty = pricedQty;
	}

	public double getUnpricedQty() {
		return unpricedQty;
	}

	public void setUnpricedQty(double unpricedQty) {
		this.unpricedQty = unpricedQty;
	}

	public float getFxInput() {
		return fxInput;
	}

	public void setFxInput(float fxInput) {
		this.fxInput = fxInput;
	}

	public double getCalculatedPrice() {
		return calculatedPrice;
	}

	public void setCalculatedPrice(double calculatedPrice) {
		this.calculatedPrice = calculatedPrice;
	}

	public double getCurvePrice() {
		return curvePrice;
	}

	public void setCurvePrice(double curvePrice) {
		this.curvePrice = curvePrice;
	}

	public List<TriggerPrice> getTriggerList() {
		return triggerList;
	}

	public void setTriggerList(List<TriggerPrice> triggerList) {
		this.triggerList = triggerList;
	}

	public String getMonthYear() {
		return monthYear;
	}

	public void setMonthYear(String monthYear) {
		this.monthYear = monthYear;
	}

	public List<CurveMarketData> getCollectionArray() {
		return collectionArray;
	}

	public void setCollectionArray(List<CurveMarketData> collectionArray) {
		this.collectionArray = collectionArray;
	}

	public int getMissingDays() {
		return missingDays;
	}

	public void setMissingDays(int missingDays) {
		this.missingDays = missingDays;
	}

	public double getCoefficient() {
		return coefficient;
	}

	public void setCoefficient(double coefficient) {
		this.coefficient = coefficient;
	}

	public Map<LocalDate, Double> getPriceMap() {
		return priceMap;
	}

	public void setPriceMap(Map<LocalDate, Double> priceMap) {
		this.priceMap = priceMap;
	}

	public Map<LocalDate, CurveMarketData> getPriceDetailsMap() {
		return priceDetailsMap;
	}

	public void setPriceDetailsMap(Map<LocalDate, CurveMarketData> priceDetailsMap) {
		this.priceDetailsMap = priceDetailsMap;
	}

	public LocalDate getFirstPricingDate() {
		return firstPricingDate;
	}

	public void setFirstPricingDate(LocalDate firstPricingDate) {
		this.firstPricingDate = firstPricingDate;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getCurveQty() {
		return curveQty;
	}

	public void setCurveQty(String curveQty) {
		this.curveQty = curveQty;
	}

	public String getCurveCurrency() {
		return curveCurrency;
	}

	public void setCurveCurrency(String curveCurrency) {
		this.curveCurrency = curveCurrency;
	}

	public Map<LocalDate, String> getPriceFlagMap() {
		return priceFlagMap;
	}

	public void setPriceFlagMap(Map<LocalDate, String> priceFlagMap) {
		this.priceFlagMap = priceFlagMap;
	}

	public HolidayRuleDates getHolidayRuleDates() {
		return holidayRuleDates;
	}

	public void setHolidayRuleDates(HolidayRuleDates holidayRuleDates) {
		this.holidayRuleDates = holidayRuleDates;
	}

	public List<GMRQPDateDetails> getGmrQPDetailsList() {
		return gmrQPDetailsList;
	}

	public void setGmrQPDetailsList(List<GMRQPDateDetails> gmrQPDetailsList) {
		this.gmrQPDetailsList = gmrQPDetailsList;
	}

	public List<GMR> getGmrList() {
		return gmrList;
	}

	public void setGmrList(List<GMR> gmrList) {
		this.gmrList = gmrList;
	}

	public String getFxCurve() {
		return fxCurve;
	}

	public void setFxCurve(String fxCurve) {
		this.fxCurve = fxCurve;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public double getQtyUnitConversionFactor() {
		return qtyUnitConversionFactor;
	}

	public void setQtyUnitConversionFactor(double qtyUnitConversionFactor) {
		this.qtyUnitConversionFactor = qtyUnitConversionFactor;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPricingPeriod() {
		return pricingPeriod;
	}

	public void setPricingPeriod(String pricingPeriod) {
		this.pricingPeriod = pricingPeriod;
	}

	public boolean isActualPricing() {
		return isActualPricing;
	}

	public void setActualPricing(boolean isActualPricing) {
		this.isActualPricing = isActualPricing;
	}

	public boolean isActualQuoted() {
		return isActualQuoted;
	}

	public void setActualQuoted(boolean isActualQuoted) {
		this.isActualQuoted = isActualQuoted;
	}

	public String getMonthDefinition() {
		return monthDefinition;
	}

	public void setMonthDefinition(String monthDefinition) {
		this.monthDefinition = monthDefinition;
	}

	public int getOffsetDays() {
		return offsetDays;
	}

	public void setOffsetDays(int offsetDays) {
		this.offsetDays = offsetDays;
	}

	public JSONArray getExpiryArr() {
		return expiryArr;
	}

	public void setExpiryArr(JSONArray expiryArr) {
		this.expiryArr = expiryArr;
	}

	public Map<LocalDate, Double> getPriceMapGMR() {
		return priceMapGMR;
	}

	public void setPriceMapGMR(Map<LocalDate, Double> priceMapGMR) {
		this.priceMapGMR = priceMapGMR;
	}

	public Map<LocalDate, String> getPriceFlagMapGMR() {
		return priceFlagMapGMR;
	}

	public void setPriceFlagMapGMR(Map<LocalDate, String> priceFlagMapGMR) {
		this.priceFlagMapGMR = priceFlagMapGMR;
	}

	public boolean isExp() {
		return isExp;
	}

	public void setExp(boolean isExp) {
		this.isExp = isExp;
	}

	public JSONArray getTriggerPriceExposure() {
		return triggerPriceExposure;
	}

	public void setTriggerPriceExposure(JSONArray triggerPriceExposure) {
		this.triggerPriceExposure = triggerPriceExposure;
	}

	public int getIndexPrecision() {
		return indexPrecision;
	}

	public void setIndexPrecision(int indexPrecision) {
		this.indexPrecision = indexPrecision;
	}
	
	public Map<LocalDate, String> getMonthFlagMap() {
		return monthFlagMap;
	}

	public void setMonthFlagMap(Map<LocalDate, String> monthFlagMap) {
		this.monthFlagMap = monthFlagMap;
	}
	
	public Map<LocalDate, String> getMonthFlagMapGMR() {
		return monthFlagMapGMR;
	}

	public void setMonthFlagMapGMR(Map<LocalDate, String> monthFlagMapGMR) {
		this.monthFlagMapGMR = monthFlagMapGMR;
	}

	public Map<LocalDate, Double> getPriceWithoutFXMap() {
		return priceWithoutFXMap;
	}

	public void setPriceWithoutFXMap(Map<LocalDate, Double> priceWithoutFXMap) {
		this.priceWithoutFXMap = priceWithoutFXMap;
	}

	public boolean getTriggerPriceExecution() {
		return triggerPriceExecution;
	}

	public void setTriggerPriceExecution(boolean triggerPriceExecution) {
		this.triggerPriceExecution = triggerPriceExecution;
	}

	public Map<LocalDate, Double> getPriceFxMap() {
		return priceFxMap;
	}

	public void setPriceFxMap(Map<LocalDate, Double> priceFxMap) {
		this.priceFxMap = priceFxMap;
	}

	public Map<LocalDate, String> getPriceFxFlagMap() {
		return priceFxFlagMap;
	}

	public void setPriceFxFlagMap(Map<LocalDate, String> priceFxFlagMap) {
		this.priceFxFlagMap = priceFxFlagMap;
	}

	public Map<LocalDate, Double> getPriceFxMapGMR() {
		return priceFxMapGMR;
	}

	public void setPriceFxMapGMR(Map<LocalDate, Double> priceFxMapGMR) {
		this.priceFxMapGMR = priceFxMapGMR;
	}

	public Map<LocalDate, String> getPriceFxFlagMapGMR() {
		return priceFxFlagMapGMR;
	}

	public void setPriceFxFlagMapGMR(Map<LocalDate, String> priceFxFlagMapGMR) {
		this.priceFxFlagMapGMR = priceFxFlagMapGMR;
	}

	public double getAvgFx() {
		return avgFx;
	}

	public void setAvgFx(double avgFx) {
		this.avgFx = avgFx;
	}

	public Map<String, List<JSONObject>> getForwardPriceMap() {
		return forwardPriceMap;
	}

	public void setForwardPriceMap(Map<String, List<JSONObject>> forwardPriceMap) {
		this.forwardPriceMap = forwardPriceMap;
	}

	public double getTotalTriggerPriceQty() {
		return totalTriggerPriceQty;
	}

	public void setTotalTriggerPriceQty(double totalTriggerPriceQty) {
		this.totalTriggerPriceQty = totalTriggerPriceQty;
	}

}
