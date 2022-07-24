package com.eka.ekaPricing.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Formula {
	private String id;
	private String formulaName;
	private String formulaExpression;
	private String curveId;
	private String price;
	private String pricePrecision;
	private List<Curve> curveList;
	private boolean triggerPriceEnabled;
	private List<TriggerPrice> triggerPricing;
	private String contractCurrencyPrice;
	private List<PriceDifferential> priceDifferential;
	private String holidayRule;
	public String getId() {
		return id;
	}

	public void setId(String id) {
		if(id==null) {
			id = "";
		}
		this.id = id;
	}

	public String getFormulaName() {
		return formulaName;
	}

	public void setFormulaName(String formulaName) {
		this.formulaName = formulaName;
	}

	public String getFormulaExpression() {
		return formulaExpression;
	}

	public void setFormulaExpression(String formulaExpression) {
		this.formulaExpression = formulaExpression;
	}

	public String getCurveId() {
		return curveId;
	}

	public void setCurveId(String curveId) {
		this.curveId = curveId;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getPricePrecision() {
		return pricePrecision;
	}

	public void setPricePrecision(String pricePrecision) {
		this.pricePrecision = pricePrecision;
	}

	public List<Curve> getCurveList() {
		return curveList;
	}

	public void setCurveList(List<Curve> curveList) {
		this.curveList = curveList;
	}

	public boolean isTriggerPriceEnabled() {
		return triggerPriceEnabled;
	}

	public void setTriggerPriceEnabled(boolean triggerPriceEnabled) {
		this.triggerPriceEnabled = triggerPriceEnabled;
	}

	public String toString() {
		return this.getFormulaName() + " : " + this.getPrice();
	}

	public String getContractCurrencyPrice() {
		return contractCurrencyPrice;
	}

	public void setContractCurrencyPrice(String contractCurrencyPrice) {
		this.contractCurrencyPrice = contractCurrencyPrice;
	}

	public List<TriggerPrice> getTriggerPricing() {
		return triggerPricing;
	}

	public void setTriggerPricing(List<TriggerPrice> triggerPricing) {
		this.triggerPricing = triggerPricing;
	}

	public List<PriceDifferential> getPriceDifferential() {
		return priceDifferential;
	}

	public void setPriceDifferential(List<PriceDifferential> priceDifferential) {
		this.priceDifferential = priceDifferential;
	}

	public String getHolidayRule() {
		return holidayRule;
	}

	public void setHolidayRule(String holidayRule) {
		this.holidayRule = holidayRule;
	}
}
