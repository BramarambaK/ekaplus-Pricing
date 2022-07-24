package com.eka.ekaPricing.pojo;

import java.util.List;

public class CurveDetails {
	private String contractID;
	private String expression;
	private List<Curve> curveList;
	private boolean executeByContract;
	private String pricePrecision;
	private boolean triggerPriceEnabled;
	private String holidayRule;
	private List<TriggerPrice> triggerPriceList;
	private String originalExp;
	private List<String> contractList;
	private List<PriceDifferential> priceDifferentialList;
	private List<QualityAdjustment> qualityAdjustmentList;
	private double triggerQty;

	public boolean isExecuteByContract() {
		return executeByContract;
	}

	public void setExecuteByContract(boolean executeByContract) {
		this.executeByContract = executeByContract;
	}

	public String getContractID() {
		return contractID;
	}

	public void setContractID(String contractID) {
		this.contractID = contractID;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public List<Curve> getCurveList() {
		return curveList;
	}

	public void setCurveList(List<Curve> curveList) {
		this.curveList = curveList;
	}

	public String getPricePrecision() {
		return pricePrecision;
	}

	public void setPricePrecision(String pricePrecision) {
		this.pricePrecision = pricePrecision;
	}

	public boolean isTriggerPriceEnabled() {
		return triggerPriceEnabled;
	}

	public void setTriggerPriceEnabled(boolean isTriggerPriceEnabled) {
		this.triggerPriceEnabled = isTriggerPriceEnabled;
	}

	public List<TriggerPrice> getTriggerPriceList() {
		return triggerPriceList;
	}

	public void setTriggerPriceList(List<TriggerPrice> triggerPriceList) {
		this.triggerPriceList = triggerPriceList;
	}

	public List<String> getContractList() {
		return contractList;
	}

	public void setContractList(List<String> contractList) {
		this.contractList = contractList;
	}

	public List<PriceDifferential> getPriceDifferentialList() {
		return priceDifferentialList;
	}

	public void setPriceDifferentialList(List<PriceDifferential> priceDifferentialList) {
		this.priceDifferentialList = priceDifferentialList;
	}

	public List<QualityAdjustment> getQualityAdjustmentList() {
		return qualityAdjustmentList;
	}

	public void setQualityAdjustmentList(List<QualityAdjustment> qualityAdjustmentList) {
		this.qualityAdjustmentList = qualityAdjustmentList;
	}

	public String getHolidayRule() {
		return holidayRule;
	}

	public void setHolidayRule(String holidayRule) {
		this.holidayRule = holidayRule;
	}

	public String getOriginalExp() {
		return originalExp;
	}

	public void setOriginalExp(String originalExp) {
		this.originalExp = originalExp;
	}

	public double getTriggerQty() {
		return triggerQty;
	}

	public void setTriggerQty(double triggerQty) {
		this.triggerQty = triggerQty;
	}

}
